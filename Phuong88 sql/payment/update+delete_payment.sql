-- Function cấm UPDATE
CREATE OR REPLACE FUNCTION prevent_payment_update()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'LỖI DB (P0008): Cập nhật một giao dịch thanh toán đã ghi nhận (ID: %) bị cấm tuyệt đối.', OLD.payment_id;
END;
$$ LANGUAGE plpgsql;

-- Function cấm DELETE
CREATE OR REPLACE FUNCTION prevent_payment_deletion()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'LỖI DB (P0009): Xóa một giao dịch thanh toán đã ghi nhận (ID: %) bị cấm tuyệt đối.', OLD.payment_id;
END;
$$ LANGUAGE plpgsql;


-- Trigger cấm UPDATE
DROP TRIGGER IF EXISTS trg_payment_before_update ON payments;
CREATE TRIGGER trg_payment_before_update
    BEFORE UPDATE ON payments
    FOR EACH ROW
    EXECUTE FUNCTION prevent_payment_update();

-- Trigger cấm DELETE
DROP TRIGGER IF EXISTS trg_payment_before_delete ON payments;
CREATE TRIGGER trg_payment_before_delete
    BEFORE DELETE ON payments
    FOR EACH ROW
    EXECUTE FUNCTION prevent_payment_deletion();