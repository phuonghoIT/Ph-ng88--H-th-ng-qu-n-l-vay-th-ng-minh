CREATE OR REPLACE FUNCTION check_loan_update_rules()
RETURNS TRIGGER AS $$
DECLARE
    -- SỬA LỖI: Khai báo biến với kiểu %ROWTYPE
    temp_new loans%ROWTYPE;
BEGIN
    -- QUY TẮC 1: Cấm tuyệt đối việc cập nhật một khoản vay đã tất toán (PAID).
    IF OLD.status = 'PAID' THEN
        -- SỬA LỖI: Dùng đúng tên cột 'loan_id'
        RAISE EXCEPTION 'LỖI DB: Khoản vay đã tất toán xong (ID: %), không được phép thay đổi!', OLD.loan_id
            USING HINT = 'Vui lòng kiểm tra lại trạng thái khoản vay.', ERRCODE = 'P0001';
    END IF;

    -- QUY TẮC 2: Chỉ cho phép cột 'status' được thay đổi.
    temp_new := NEW;
    temp_new.status := OLD.status;
    
    -- So sánh bản sao với dòng gốc.
    IF temp_new IS DISTINCT FROM OLD THEN
        RAISE EXCEPTION 'LỖI DB: Chỉ được phép cập nhật trạng thái (status) của khoản vay. Các thông tin khác là bất biến.'
            USING ERRCODE = 'P0002';
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Gắn lại trigger vào bảng 'loans' (có 's')
DROP TRIGGER IF EXISTS trg_loan_before_update ON loans;
CREATE TRIGGER trg_loan_before_update
    BEFORE UPDATE ON loans
    FOR EACH ROW
    EXECUTE FUNCTION check_loan_update_rules();

