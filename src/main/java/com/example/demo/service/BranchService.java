package com.example.demo.service;

import com.example.demo.repository.BranchRepository;
import com.example.demo.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BranchService {
    @Autowired
    // ket noi bang
    private EmployeeRepository employeeRepository;
    @Autowired
    private BranchRepository branchRepository;
    @Transactional
    public void deleteBranch(Long branchId){
        employeeRepository.shipEmployeesToBranch99(branchId);
        //chay cau lenh o tren
        branchRepository.flush();
        branchRepository.deleteById(branchId);

    }

}
