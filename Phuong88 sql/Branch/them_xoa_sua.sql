SELECT *
from branches;



-- Thêm một chi nhánh bình thường
INSERT INTO branches (branch_name, address, phone)
VALUES ('Chi nhánh An Lạc', '123 Nguyễn Tri Phương, Quận 1, TP.HCM', '02811112222');

INSERT INTO branches (branch_id, branch_name, address, phone)
VALUES (99, 'Chi nhánh Chờ phân bổ', 'Không có địa chỉ', '0000000000')
ON CONFLICT (branch_id) DO NOTHING; -- Tránh lỗi nếu đã tạo rồi


-- Cập nhật số điện thoại và địa chỉ của chi nhánh (Giả sử chi nhánh vừa tạo có ID là 1) thành công
UPDATE branches
SET phone = '0999888777', 
    address = '456 Nguyễn Trãi, Quận 1, TP.HCM'
WHERE branch_id = 3;

-- Cố gắng thay đổi ID của chi nhánh từ 1 sang 100 -Thất bại
UPDATE branches
SET branch_id = 100
WHERE branch_id = 3;

-- Xóa
-- BƯỚC 1: Thêm thử một nhân viên vào chi nhánh ID 1 để làm mồi nhử
INSERT INTO employees (full_name, role, status, branch_id)
VALUES ('Nhân viên Test', 'STAFF', 'ACTIVE', 3);

-- Kiểm tra xem nhân viên đã ở chi nhánh 1 chưa
SELECT employee_id, full_name, branch_id FROM employees WHERE full_name = 'Nhân viên Test';

-- BƯỚC 2: Xóa chi nhánh ID 1
DELETE FROM branches WHERE branch_id = 3;

-- BƯỚC 3: Kiểm tra lại nhân viên.
-- MONG ĐỢI: Chi nhánh 1 đã biến mất, nhưng "Nhân viên Test" không bị xóa, 
-- mà cột branch_id của họ đã tự động chuyển thành 99.
SELECT employee_id, full_name, branch_id FROM employees WHERE full_name = 'Nhân viên Test';