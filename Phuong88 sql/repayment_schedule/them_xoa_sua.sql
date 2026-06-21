select *
from repayment_schedules;


-- Cố gắng thay đổi số tiền gốc của một kỳ hạn (hành động sẽ bị chặn)



UPDATE repayment_schedules
SET principal_amount = 5000000
WHERE schedule_id = 3;

-- Cố gắng xóa một kỳ hạn cụ thể (hành động sẽ bị chặn)
DELETE FROM repayment_schedules WHERE schedule_id = 1;