CREATE OR REPLACE FUNCTION check_collateral_update_rules()
RETURNS TRIGGER AS $$
DECLARE
    -- Khai báo biến để lưu trạng thái của khoản vay
    loan_status_of_collateral loan_status;
BEGIN
    -- =================================================================
    -- QUY TẮC 1: CẤM THAY ĐỔI LOAN_ID
    -- OLD chứa giá trị cũ, NEW chứa giá trị mới.
    -- 'IS DISTINCT FROM' là cách so sánh an toàn, xử lý đúng cả giá trị NULL.
    -- =================================================================
    IF NEW.loan_id IS DISTINCT FROM OLD.loan_id THEN
        RAISE EXCEPTION 'LỖI DB (P0012): Không được phép chuyển tài sản thế chấp từ khoản vay này (ID: %) sang khoản vay khác (ID: %).', OLD.loan_id, NEW.loan_id
            USING ERRCODE = 'P0012';
    END IF;

    -- =================================================================
    -- QUY TẮC 2: KIỂM TRA TRẠNG THÁI KHOẢN VAY ĐỂ QUYẾT ĐỊNH CHO/CẤM SỬA
    -- =================================================================

    -- Lấy trạng thái của khoản vay từ bảng 'loans'.
    -- Dùng OLD.loan_id vì nó không thể bị thay đổi (đã được kiểm tra ở trên).
    SELECT status INTO loan_status_of_collateral
    FROM loans
    WHERE loan_id = OLD.loan_id;

    -- Nếu trạng thái không phải là 'PENDING'...
    IF loan_status_of_collateral <> 'PENDING' THEN
        -- ...thì kiểm tra xem có bất kỳ cột quan trọng nào bị thay đổi không.
        IF NEW.asset_type IS DISTINCT FROM OLD.asset_type OR
           NEW.estimated_value IS DISTINCT FROM OLD.estimated_value OR
           NEW.conversion_rate IS DISTINCT FROM OLD.conversion_rate
        THEN
            -- Nếu có, ném ra lỗi và hủy giao dịch.
            RAISE EXCEPTION 'LỖI DB (P0013): Không được phép thay đổi thông tin tài sản thế chấp khi khoản vay đang ở trạng thái "%".', loan_status_of_collateral
                USING HINT = 'Chỉ có thể sửa đổi thông tin tài sản khi khoản vay đang chờ duyệt (PENDING).',
                      ERRCODE = 'P0013';
        END IF;
    END IF;

    -- Nếu tất cả các quy tắc đều được thỏa mãn, cho phép UPDATE.
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Xóa trigger cũ nếu tồn tại
DROP TRIGGER IF EXISTS trg_collateral_before_update ON collaterals;

-- Tạo trigger mới
CREATE TRIGGER trg_collateral_before_update
    BEFORE UPDATE ON collaterals
    FOR EACH ROW
    EXECUTE FUNCTION check_collateral_update_rules();