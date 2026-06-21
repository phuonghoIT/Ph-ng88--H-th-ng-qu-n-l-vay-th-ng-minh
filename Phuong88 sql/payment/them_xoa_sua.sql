select *
from payments;

select *
from repayment_schedules;

select *
from loans;

-- Ghi nhận một khoản thanh toán 5,500,000 cho kỳ hạn ID 1, do nhân viên ID 2 thực hiện
INSERT INTO payments (amount, payment_date, payment_method, repayment_schedule_id, employee_id)
VALUES (5500000, CURRENT_DATE, 'Chuyển khoản', 3, 2);

-- Cố gắng thay đổi số tiền của một giao dịch (hành động sẽ bị chặn)
UPDATE payments
SET amount = 6000000
WHERE payment_id = 1;


-- Cố gắng xóa một giao dịch (hành động sẽ bị chặn)
DELETE FROM payments WHERE payment_id = 1;