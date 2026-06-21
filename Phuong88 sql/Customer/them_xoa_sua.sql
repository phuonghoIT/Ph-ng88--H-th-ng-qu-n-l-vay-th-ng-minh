SELECT *
from customers;

INSERT INTO customers (full_name, address, sdt, identity_number, job)
VALUES ('Nguyễn Văn Khách', 'Hà Nội', '0988123456', '001090111222', 'Giáo viên');

SELECT create_customer_and_account(
    p_full_name       := 'Nguyễn Thị Khách Hàng',
    p_address         := '123 Đường ABC, Quận 1, TP.HCM',
    p_sdt             := '0987654321',
    p_identity_number := '037106001230',
    p_job             := 'Kỹ sư phần mềm',
    p_username        := 'cus3',
    p_plain_password  := 'cus3'
);


UPDATE customers
SET address = 'Hải Phòng', 
    job = 'Hiệu trưởng',
    sdt = '0988999888'
WHERE customer_id = 1; 


DELETE FROM customers 
WHERE customer_id = 1;
