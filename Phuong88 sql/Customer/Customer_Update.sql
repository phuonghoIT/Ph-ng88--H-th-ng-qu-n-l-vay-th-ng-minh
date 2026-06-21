CREATE OR REPLACE FUNCTION check_customer_update_rules()
RETURNS TRIGGER AS $$
BEGIN
    -- =================================================================
    -- QUY TẮC 1: CẤM THAY ĐỔI SỐ CCCD (IDENTITY_NUMBER)
    -- CCCD là thông tin định danh gốc, không bao giờ được thay đổi.
    -- =================================================================
    IF NEW.identity_number IS DISTINCT FROM OLD.identity_number THEN
        RAISE EXCEPTION 'LỖI DB (P0015): Không được phép thay đổi số CCCD (identity_number) của khách hàng.'
            USING HINT = 'CCCD là một thông tin định danh bất biến sau khi đã tạo hồ sơ.',
                  ERRCODE = 'P0015';
    END IF;

    -- =================================================================
    -- QUY TẮC 2: CẤM THAY ĐỔI ĐIỂM TÍN DỤNG (CREDIT_SCORE) BẰNG TAY
    -- Điểm tín dụng chỉ được thay đổi bởi các quy trình tự động (quét nợ, tất toán).
    -- =================================================================
    IF NEW.credit_score IS DISTINCT FROM OLD.credit_score THEN
        RAISE EXCEPTION 'LỖI DB (P0016): Không được phép thay đổi điểm tín dụng (credit_score) của khách hàng một cách thủ công.'
            USING HINT = 'Điểm tín dụng chỉ được cập nhật tự động bởi hệ thống dựa trên lịch sử thanh toán.',
                  ERRCODE = 'P0016';
    END IF;

    -- =================================================================
    -- Nếu tất cả các quy tắc đều được thỏa mãn, cho phép UPDATE tiếp tục.
    -- Các trường như full_name, address, job,... sẽ được cập nhật bình thường.
    -- =================================================================
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;


-- Xóa trigger cũ nếu tồn tại để đảm bảo script có thể chạy lại
DROP TRIGGER IF EXISTS trg_customer_before_update ON customers;

-- Tạo trigger mới
CREATE TRIGGER trg_customer_before_update
    BEFORE UPDATE ON customers
    FOR EACH ROW
    EXECUTE FUNCTION check_customer_update_rules();