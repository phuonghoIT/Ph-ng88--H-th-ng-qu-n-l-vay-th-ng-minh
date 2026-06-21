CREATE OR REPLACE FUNCTION check_collateral_deletion_rules()
RETURNS TRIGGER AS $$
DECLARE
    -- Khai báo biến để lưu trạng thái của khoản vay
    loan_status_of_collateral loan_status;
BEGIN
    -- =================================================================
    -- QUY TẮC 3: CẤM XÓA NẾU KHOẢN VAY ĐANG HOẠT ĐỘNG HOẶC ĐÃ KẾT THÚC
    -- =================================================================

    -- Lấy trạng thái của khoản vay mà tài sản này đang gắn vào.
    -- Dùng OLD vì đây là dữ liệu của dòng sắp bị xóa.
    SELECT status INTO loan_status_of_collateral
    FROM loans
    WHERE loan_id = OLD.loan_id;

    -- Nếu trạng thái là 'ACTIVE', 'PAID', hoặc các trạng thái "khóa" khác...
    IF loan_status_of_collateral IN ('ACTIVE', 'PAID') THEN
        -- ...thì ném ra lỗi và hủy bỏ thao tác DELETE.
        RAISE EXCEPTION 'LỖI DB (P0014): Không thể xóa tài sản thế chấp của một khoản vay đang ở trạng thái "%".', loan_status_of_collateral
            USING HINT = 'Tài sản đảm bảo phải được lưu trữ vĩnh viễn cho các khoản vay đã hoặc đang hoạt động.',
                  ERRCODE = 'P0014';
    END IF;

    -- Nếu trạng thái là 'PENDING', hàm sẽ kết thúc mà không ném lỗi,
    -- và thao tác DELETE sẽ được phép tiếp tục.
    RETURN OLD;
END;
$$ LANGUAGE plpgsql;


-- Xóa trigger cũ nếu tồn tại
DROP TRIGGER IF EXISTS trg_collateral_before_delete ON collaterals;

-- Tạo trigger mới
CREATE TRIGGER trg_collateral_before_delete
    BEFORE DELETE ON collaterals
    FOR EACH ROW
    EXECUTE FUNCTION check_collateral_deletion_rules();