package com.example.demo.repository;

import com.example.demo.entity.Employees;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Repository
public interface EmployeeRepository extends JpaRepository<Employees, Long> {

    List<Employees> findByBranchBranchId(Long branchId);

    @Transactional
    @Modifying
    @Query(value = "UPDATE employees " +
            "SET branch_id = 99 " +
            "WHERE branch_id = :oldBranchId", nativeQuery = true)
    void shipEmployeesToBranch99(@Param("oldBranchId") Long oldBranchId);

    @Query(value = "SELECT e.employee_id " +
            "FROM employees e " +
            "LEFT JOIN loans l ON e.employee_id = l.employee_id " +
            "WHERE e.status = 'ACTIVE' AND e.branch_id = :branchId AND e.employee_id != :employeeId" +
            "GROUP BY e.employee_id " +
            "ORDER BY COUNT(l.loan_id) ASC " +
            "LIMIT 1", nativeQuery = true)
    Long findLeastContractSameBranch(@Param("branchId") Long branchId, @Param("employeeId") Long employeeId);

    @Transactional
    @Modifying
    @Query(value = "UPDATE employees SET status = 'INACTIVE' WHERE employee_id = :empId", nativeQuery = true)
    void softDeleteEmployee(@Param("empId") Long empId);




}
