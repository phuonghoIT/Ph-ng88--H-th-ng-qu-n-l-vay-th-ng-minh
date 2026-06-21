SELECT *
from employees;

SELECT public.create_employee_and_account_plaintext(
    'employee thử việc',  
    'STAFF', 
    'thuviec', 
    'thuviec',
    2 -- Branch ID
);

-- Thăng chức cho nhân viên có ID là 5 từ 'STAFF' lên 'MANAGER'
UPDATE employees
SET role = 'MANAGER'
WHERE employee_id = 5;


-- Sa thải (xóa mềm) nhân viên có ID là 5 và tự động bàn giao các khoản vay
SELECT fire_employee_and_reassign_loans(5);