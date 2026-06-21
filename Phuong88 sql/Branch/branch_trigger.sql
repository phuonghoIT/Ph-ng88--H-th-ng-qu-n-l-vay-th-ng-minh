CREATE OR REPLACE FUNCTION ship_employee_99()
RETURNS TRIGGER AS $$
BEGIN
	UPDATE employees
	SET branch_id = 99
	WHERE branch_id = OLD.branch_id

	RETURN OLD;
END;
$$ LANGUAGE plpsql


CREATE OR REPLACE FUNCTION ban_update_branch_id99()

CREATE OR REPLACE FUNCTION trg_prevent_change_branch_id()
RETURNS TRIGGER AS $$
BEGIN
    -- Kiểm tra xem giá trị branch_id MỚI (NEW) có khác giá trị CŨ (OLD) không
    IF NEW.branch_id IS DISTINCT FROM OLD.branch_id THEN
        RAISE EXCEPTION '🔴 LỖI BẢO MẬT: Không được phép thay đổi Chi nhánh (branch_id) của nhân viên đã nghiêm phong!';
    END IF;

    -- Nếu không sửa branch_id (chỉ sửa tên, số điện thoại...), cho phép UPDATE bình thường
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;


CREATE TRIGGER before_delete_branch
BEFORE DELETE ON branches
FOR EACH ROW
EXECUTE FUNCTION ship_employee_99();

CREATE TRIGGER before_update_branch
BEFORE UPDATE ON branches
FOR EACH ROW
EXECUTE FUNCTION trg_prevent_change_branch_id();


