CREATE OR REPLACE FUNCTION check_payment_preconditions()
RETURNS TRIGGER AS $$
DECLARE
    schedule_current_status schedule_status;
BEGIN
    -- Lấy trạng thái hiện tại của kỳ hạn mà khoản thanh toán này đang nhắm tới.
    -- NEW.repayment_schedule_id là ID của kỳ hạn từ dòng payment sắp được chèn vào.
    SELECT status INTO schedule_current_status
    FROM repayment_schedules
    WHERE schedule_id = NEW.repayment_schedule_id;

    -- Nếu trạng thái là 'PAID', ném ra lỗi và hủy bỏ thao tác INSERT.
    IF schedule_current_status = 'PAID' THEN
        RAISE EXCEPTION 'LỖI DB (P0007): Kỳ hạn (ID: %) đã được thanh toán hoàn tất. Không thể nộp thêm tiền.', NEW.repayment_schedule_id;
    END IF;

    -- Nếu mọi thứ ổn, cho phép INSERT.
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;


DROP TRIGGER IF EXISTS trg_payment_before_insert ON payments;
CREATE TRIGGER trg_payment_before_insert
    BEFORE INSERT ON payments
    FOR EACH ROW
    EXECUTE FUNCTION check_payment_preconditions();

CREATE OR REPLACE FUNCTION update_schedule_status_after_payment()
RETURNS TRIGGER AS $$
DECLARE
    total_required NUMERIC;
    total_paid NUMERIC;
BEGIN
    -- Tính tổng số tiền CẦN PHẢI TRẢ cho kỳ hạn này.
    SELECT (principal_amount + interest_amount + penalty_amount)
    INTO total_required
    FROM repayment_schedules
    WHERE schedule_id = NEW.repayment_schedule_id;

    -- Tính tổng số tiền ĐÃ THỰC SỰ TRẢ cho kỳ hạn này (bao gồm cả khoản vừa nộp).
    SELECT SUM(amount)
    INTO total_paid
    FROM payments
    WHERE repayment_schedule_id = NEW.repayment_schedule_id;

    -- Nếu đã trả đủ hoặc thừa, cập nhật trạng thái của kỳ hạn thành 'PAID'.
    IF total_paid >= total_required THEN
        UPDATE repayment_schedules
        SET status = 'PAID'
        WHERE schedule_id = NEW.repayment_schedule_id;
    END IF;

    -- Trigger AFTER INSERT phải trả về NULL.
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;


DROP TRIGGER IF EXISTS trg_payment_after_insert ON payments;
CREATE TRIGGER trg_payment_after_insert
    AFTER INSERT ON payments
    FOR EACH ROW
    EXECUTE FUNCTION update_schedule_status_after_payment();