CREATE OR REPLACE FUNCTION trg_before_update_employee_func()
RETURNS TRIGGER AS $$
BEGIN
    -- 1. CHẶN SỬA ĐỔI KHÓA CHÍNH (ID)
    IF NEW.employee_id IS DISTINCT FROM OLD.employee_id THEN
        RAISE EXCEPTION '🔴 BẢO MẬT DB: Không được phép sửa đổi Khóa chính (employee_id).';
    END IF;

    -- 2. CHẶN SỬA ĐỔI NHÂN VIÊN ĐÃ NGHỈ VIỆC
    -- Chỉ cho phép duy nhất một ngoại lệ: Nếu họ đang INACTIVE mà được sửa lại thành ACTIVE (tái tuyển dụng)
    IF OLD.status = 'INACTIVE' AND NEW.status = 'INACTIVE' THEN
        RAISE EXCEPTION '🔴 BẢO MẬT DB: Hồ sơ nhân viên đã nghỉ việc (ID: %) bị khóa. Không thể chỉnh sửa.', OLD.employee_id;
    END IF;

    RETURN NEW; -- Vượt qua mọi trạm gác thì cho phép UPDATE tiếp tục
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER before_update_employee
BEFORE UPDATE ON employees
FOR EACH ROW
EXECUTE FUNCTION trg_before_update_employee_func();



CREATE OR REPLACE FUNCTION trg_after_update_employee_func()
RETURNS TRIGGER AS $$
DECLARE
    v_new_handler_id BIGINT;
BEGIN
    -- 3. TỰ ĐỘNG BÀN GIAO CÔNG VIỆC KHI ĐỔI CHI NHÁNH
    IF OLD.branch_id IS DISTINCT FROM NEW.branch_id THEN
        
        -- Tìm người rảnh nhất ở chi nhánh CŨ
        SELECT e.employee_id INTO v_new_handler_id
        FROM employees e
        LEFT JOIN loans l ON e.employee_id = l.employee_id
        WHERE e.status = 'ACTIVE' 
          AND e.branch_id = OLD.branch_id 
          AND e.employee_id != OLD.employee_id
        GROUP BY e.employee_id
        ORDER BY COUNT(l.loan_id) ASC
        LIMIT 1;

        -- Nếu không tìm được ai, chặn luôn toàn bộ Transaction
        IF v_new_handler_id IS NULL THEN
            RAISE EXCEPTION '🔴 LỖI DB: Chi nhánh cũ không còn nhân viên nào để nhận bàn giao. Hủy bỏ điều chuyển!';
        END IF;

        -- Bàn giao toàn bộ hợp đồng (Cập nhật bảng loans)
        UPDATE loans 
        SET employee_id = v_new_handler_id 
        WHERE employee_id = OLD.employee_id;
        
    END IF;

    -- 4. TỰ ĐỘNG ĐỒNG BỘ ROLE TÀI KHOẢN
    IF OLD.role IS DISTINCT FROM NEW.role THEN
        -- Cập nhật Role bên bảng users
        UPDATE users 
        SET role = NEW.role 
        WHERE employee_id = NEW.employee_id;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER after_update_employee
AFTER UPDATE ON employees
FOR EACH ROW
EXECUTE FUNCTION trg_after_update_employee_func();






-- Hàm thực hiện việc sa thải và bàn giao
CREATE OR REPLACE FUNCTION fire_employee_and_reassign_loans(p_employee_id BIGINT)
RETURNS VOID AS $$
DECLARE
    v_old_branch_id BIGINT;
    v_new_handler_id BIGINT;
BEGIN
    -- Lấy chi nhánh của nhân viên sắp bị sa thải
    SELECT branch_id INTO v_old_branch_id FROM employees WHERE employee_id = p_employee_id;

    -- SỬA LỖI: Thêm e. vào trước employee_id để tránh lỗi ambiguous
    SELECT e.employee_id INTO v_new_handler_id
    FROM employees e
    LEFT JOIN loans l ON e.employee_id = l.employee_id
    WHERE e.status = 'ACTIVE' AND e.branch_id = v_old_branch_id AND e.employee_id != p_employee_id
    GROUP BY e.employee_id
    ORDER BY COUNT(l.loan_id) ASC
    LIMIT 1;

    -- Chặn nếu không có ai để bàn giao VÀ nhân viên này đang quản lý khoản vay
    IF v_new_handler_id IS NULL AND EXISTS (SELECT 1 FROM loans WHERE employee_id = p_employee_id) THEN
        RAISE EXCEPTION '🔴 LỖI DB: Không thể sa thải nhân viên ID % vì không có ai trong chi nhánh để nhận bàn giao các khoản vay.', p_employee_id;
    END IF;

    -- Bàn giao các khoản vay (nếu có người nhận)
    IF v_new_handler_id IS NOT NULL THEN
        UPDATE loans SET employee_id = v_new_handler_id WHERE employee_id = p_employee_id;
    END IF;

    -- Xóa mềm (Soft Delete)
    UPDATE employees SET status = 'INACTIVE' WHERE employee_id = p_employee_id;

END;
$$ LANGUAGE plpgsql;


-- Hàm đơn giản chỉ để ném lỗi
CREATE OR REPLACE FUNCTION prevent_employee_hard_delete()
RETURNS TRIGGER AS $$
BEGIN
    -- Ném ra lỗi và hướng dẫn cách làm đúng
    RAISE EXCEPTION 'LỖI DB: Xóa vật lý nhân viên bị cấm để bảo toàn lịch sử.'
        USING HINT = 'Để cho nhân viên nghỉ việc, hãy sử dụng hàm: SELECT fire_employee_and_reassign_loans(employee_id);';
END;
$$ LANGUAGE plpgsql;

