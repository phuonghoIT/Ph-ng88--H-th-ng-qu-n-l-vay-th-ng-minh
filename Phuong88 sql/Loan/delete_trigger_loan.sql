CREATE OR REPLACE FUNCTION prevent_loan_deletion()
RETURNS TRIGGER AS $$
BEGIN
    -- Ném ra một ngoại lệ để hủy bỏ thao tác DELETE
    RAISE EXCEPTION 'LỖI DB: Xóa vật lý khoản vay (ID: %) bị cấm để đảm bảo tính toàn vẹn dữ liệu và lịch sử giao dịch.', OLD.id
        USING HINT = 'Thay vì xóa, hãy xem xét việc cập nhật trạng thái của khoản vay.',
              ERRCODE = 'P0003';
END;
$$ LANGUAGE plpgsql;


-- Xóa trigger cũ nếu tồn tại
DROP TRIGGER IF EXISTS trg_loan_before_delete ON loan;

-- Tạo trigger mới
CREATE TRIGGER trg_loan_before_delete
BEFORE DELETE ON loan
FOR EACH ROW
EXECUTE FUNCTION prevent_loan_deletion();