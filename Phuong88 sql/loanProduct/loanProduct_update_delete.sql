CREATE OR REPLACE FUNCTION prevent_loan_product_update()
RETURNS TRIGGER AS $$
BEGIN
    -- Ném ra một ngoại lệ để hủy bỏ hoàn toàn thao tác UPDATE.
    RAISE EXCEPTION 'LỖI DB (P0018): Cập nhật thông tin gói sản phẩm vay (ID: %) bị cấm tuyệt đối.', OLD.loan_product_id
        USING HINT = 'Để thay đổi, hãy tạo một gói sản phẩm vay mới với các thông số khác.';
END;
$$ LANGUAGE plpgsql;

-- Xóa trigger cũ nếu tồn tại
DROP TRIGGER IF EXISTS trg_loan_product_before_update ON loan_products;

-- Tạo trigger mới
CREATE TRIGGER trg_loan_product_before_update
    BEFORE UPDATE ON loan_products
    FOR EACH ROW
    EXECUTE FUNCTION prevent_loan_product_update();


CREATE OR REPLACE FUNCTION prevent_loan_product_deletion()
RETURNS TRIGGER AS $$
BEGIN
    -- Ném ra một ngoại lệ để hủy bỏ hoàn toàn thao tác DELETE.
    RAISE EXCEPTION 'LỖI DB (P0019): Xóa gói sản phẩm vay (ID: %) bị cấm tuyệt đối.', OLD.loan_product_id
        USING HINT = 'Các gói sản phẩm phải được lưu trữ vĩnh viễn để đảm bảo tính toàn vẹn lịch sử cho các khoản vay cũ.';
END;
$$ LANGUAGE plpgsql;


-- Xóa trigger cũ nếu tồn tại
DROP TRIGGER IF EXISTS trg_loan_product_before_delete ON loan_products;

-- Tạo trigger mới
CREATE TRIGGER trg_loan_product_before_delete
    BEFORE DELETE ON loan_products
    FOR EACH ROW
    EXECUTE FUNCTION prevent_loan_product_deletion();