-- FUNCTION: Được gọi bởi trigger trên bảng 'repayment_schedule'
CREATE OR REPLACE FUNCTION prevent_schedule_deletion()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'LỖI DB (P0006): Xóa một kỳ hạn trong lịch trả nợ (ID: %) bị cấm tuyệt đối để bảo toàn lịch sử giao dịch.', OLD.schedule_id;
END;
$$ LANGUAGE plpgsql;

-- TRIGGER: Gắn vào bảng 'repayment_schedule'
DROP TRIGGER IF EXISTS trg_before_schedule_delete ON repayment_schedule;
CREATE TRIGGER trg_before_schedule_delete
    BEFORE DELETE ON repayment_schedule
    FOR EACH ROW
    EXECUTE FUNCTION prevent_schedule_deletion();