-- FUNCTION: Được gọi bởi trigger trên bảng 'repayment_schedule'
CREATE OR REPLACE FUNCTION protect_repayment_schedule_data()
RETURNS TRIGGER AS $$
BEGIN
    -- Kiểm tra nếu bất kỳ cột nào trong danh sách cấm bị thay đổi
    IF NEW.principal_amount IS DISTINCT FROM OLD.principal_amount OR
       NEW.interest_amount IS DISTINCT FROM OLD.interest_amount OR
       NEW.due_date IS DISTINCT FROM OLD.due_date OR
       NEW.loan_id IS DISTINCT FROM OLD.loan_id OR
       NEW.period_number IS DISTINCT FROM OLD.period_number
    THEN
        RAISE EXCEPTION 'LỖI DB (P0005): Không được phép thay đổi các thông tin tài chính cốt lõi (gốc, lãi, ngày đáo hạn...) của một kỳ hạn đã tạo.'
            USING HINT = 'Chỉ có thể cập nhật trạng thái (status) và tiền phạt (penalty_amount).';
    END IF;

    -- Nếu không vi phạm, cho phép UPDATE tiếp tục
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- TRIGGER: Gắn vào bảng 'repayment_schedule'
DROP TRIGGER IF EXISTS trg_before_schedule_update ON repayment_schedule;
CREATE TRIGGER trg_before_schedule_update
    BEFORE UPDATE ON repayment_schedule
    FOR EACH ROW
    EXECUTE FUNCTION protect_repayment_schedule_data();