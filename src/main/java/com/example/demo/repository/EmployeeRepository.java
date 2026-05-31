package com.example.demo.repository;

import com.example.demo.entity.Employees;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface EmployeeRepository extends JpaRepository< Employees, Long> {
    List<Employees> findByBranchBranchId(Long branchId);
    @Modifying
    @Query(value = "UPDATE employees " +
            "SET branch_id = 99 " +
            "WHERE branch_id = :oldBranchId", nativeQuery = true)
    void shipEmployeesToBranch99(@Param("oldBranchId") Long oldBranchId);
}
