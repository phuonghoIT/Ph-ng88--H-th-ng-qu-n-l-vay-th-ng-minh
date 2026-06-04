package com.example.demo.service;

import com.example.demo.entity.Employees;
import com.example.demo.repository.EmployeeRepository;
import com.example.demo.repository.LoanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmployeeService {
    @Autowired
    private EmployeeRepository employeeRepository;
    @Autowired
    private LoanRepository loanRepository;
    @Transactional
    public void ship_work_same_branch(Long employeeId, Long newBranchId){
        Employees e = employeeRepository.findById(employeeId).orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên"));
        Long oldBranchId = e.getBranch().getBranchId();
        if (!oldBranchId.equals(newBranchId)){
            Long newHandlerId = employeeRepository.findingLeastContractSameBranch(oldBranchId);
            if (newHandlerId == null || newHandlerId.equals(employeeId)){
                throw new RuntimeException("🔴 LỖI: Chi nhánh cũ không còn ai khác để nhận bàn giao việc!");

            }
            loanRepository.shipWorkNative(employeeId, newHandlerId);
            e.getBranch().setBranchId(newBranchId);
            employeeRepository.save(e);

        }


    }

    @Transactional
    public void fired_employee(Long employeeId){
        ship_work_same_branch(employeeId, 0L);
        employeeRepository.softDeleteEmployee(employeeId);
        employeeRepository.flush();

    }


}
