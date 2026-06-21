CREATE OR REPLACE FUNCTION check_loan_update_rules()
RETURNS TRIGGER AS $$
DECLARE
    -- Tạo một biến tạm có kiểu dữ liệu giống hệt một dòng của bảng 'loan'
    temp_new loan;
BEGIN
    -- QUY TẮC 1: Cấm tuyệt đối việc cập nhật một khoản vay đã tất toán (PAID).
    -- OLD là một biến đặc biệt trong trigger, chứa giá trị của dòng TRƯỚC khi update.
    IF OLD.status = 'PAID' THEN
        RAISE EXCEPTION 'LỖI DB: Khoản vay đã tất toán xong (ID: %), không được phép thay đổi!', OLD.id
            USING HINT = 'Vui lòng kiểm tra lại trạng thái khoản vay.', ERRCODE = 'P0001';
    END IF;

    -- QUY TẮC 2: Chỉ cho phép cột 'status' được thay đổi.
    -- NEW là một biến đặc biệt khác, chứa giá trị của dòng SAU KHI update.
    
    -- Tạo một bản sao của dòng mới (NEW)
    temp_new := NEW;
    
    -- Giả lập rằng 'status' không thay đổi bằng cách gán giá trị cũ cho nó.
    temp_new.status := OLD.status;
    
    -- Bây giờ, so sánh bản sao (đã chuẩn hóa status) với dòng gốc (OLD).
    -- Nếu chúng vẫn khác nhau, điều đó có nghĩa là một cột nào đó khác 'status' đã bị thay đổi.
    IF temp_new IS DISTINCT FROM OLD THEN
        RAISE EXCEPTION 'LỖI DB: Chỉ được phép cập nhật trạng thái (status) của khoản vay. Các thông tin khác (như amount, loan_date,...) là bất biến.'
            USING ERRCODE = 'P0002';
    END IF;
    
    -- Nếu tất cả các quy tắc đều được thỏa mãn, cho phép thực hiện lệnh UPDATE.
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Tạo trigger mới
CREATE TRIGGER trg_loan_before_update
BEFORE UPDATE ON loan        -- Chạy TRƯỚC khi lệnh UPDATE thực sự ghi dữ liệu
FOR EACH ROW                 -- Áp dụng cho mỗi dòng bị ảnh hưởng bởi lệnh UPDATE
EXECUTE FUNCTION check_loan_update_rules();