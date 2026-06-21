CREATE OR REPLACE FUNCTION check_collateral_preconditions()
RETURNS TRIGGER AS $$
DECLARE
    -- Khai báo một biến để lưu trữ loại khoản vay lấy từ bảng 'loans'
    loan_type_of_collateral loan_type;
BEGIN
    -- =================================================================
    -- QUY TẮC 1: KIỂM TRA GIÁ TRỊ ƯỚC TÍNH CỦA TÀI SẢN
    -- NEW là một biến đặc biệt chứa toàn bộ dữ liệu của dòng sắp được chèn vào.
    -- =================================================================
    IF NEW.estimated_value <= 0 THEN
        -- Nếu giá trị không hợp lệ, ném ra một ngoại lệ và hủy bỏ giao dịch.
        RAISE EXCEPTION 'LỖI DB (P0011): Giá trị ước tính của tài sản thế chấp (estimated_value) phải lớn hơn 0.'
            USING ERRCODE = 'P0011';
    END IF;

    -- =================================================================
    -- QUY TẮC 2: KIỂM TRA LOẠI CỦA KHOẢN VAY LIÊN QUAN
    -- =================================================================

    -- Dùng 'loan_id' từ dòng collateral mới để truy vấn sang bảng 'loans'.
    SELECT type INTO loan_type_of_collateral
    FROM loans
    WHERE id = NEW.loan_id;

    -- Nếu khoản vay không phải là 'SECURED', ném ra lỗi.
    IF loan_type_of_collateral <> 'SECURED' THEN
        RAISE EXCEPTION 'LỖI DB (P0010): Chỉ có thể thêm tài sản thế chấp cho các khoản vay có loại là SECURED.'
            USING HINT = 'Khoản vay (ID: ' || NEW.loan_id || ') hiện đang có loại là "' || loan_type_of_collateral || '".',
                  ERRCODE = 'P0010';
    END IF;

    -- =================================================================
    -- Nếu tất cả các quy tắc đều được thỏa mãn, cho phép thao tác INSERT tiếp tục.
    -- =================================================================
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;


-- Xóa trigger cũ nếu tồn tại để đảm bảo script có thể chạy lại nhiều lần
DROP TRIGGER IF EXISTS trg_collateral_before_insert ON collaterals;

-- Tạo trigger mới
CREATE TRIGGER trg_collateral_before_insert
    BEFORE INSERT ON collaterals      -- Chạy TRƯỚC khi lệnh INSERT thực sự ghi dữ liệu
    FOR EACH ROW                     -- Áp dụng cho mỗi dòng bị ảnh hưởng bởi lệnh INSERT
    EXECUTE FUNCTION check_collateral_preconditions();