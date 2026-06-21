CREATE OR REPLACE FUNCTION prevent_customer_deletion()
RETURNS TRIGGER AS $$
BEGIN
    -- Ném ra một ngoại lệ để hủy bỏ hoàn toàn thao tác DELETE.
    RAISE EXCEPTION 'LỖI DB (P0017): Xóa vật lý khách hàng (ID: %) bị cấm tuyệt đối để bảo toàn lịch sử và tính toàn vẹn dữ liệu.', OLD.customer_id
        USING HINT = 'Thay vì xóa, hãy xem xét việc cập nhật trạng thái của khách hàng thành INACTIVE.';
END;
$$ LANGUAGE plpgsql;



-- Xóa trigger cũ nếu tồn tại
DROP TRIGGER IF EXISTS trg_customer_before_delete ON customers;

-- Tạo trigger mới
CREATE TRIGGER trg_customer_before_delete
    BEFORE DELETE ON customers
    FOR EACH ROW
    EXECUTE FUNCTION prevent_customer_deletion();