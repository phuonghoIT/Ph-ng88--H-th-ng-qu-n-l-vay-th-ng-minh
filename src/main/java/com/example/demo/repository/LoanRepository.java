package com.example.demo.repository;

import com.example.demo.entity.Loan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {

    // Tìm các khoản vay dựa vào ID khách hàng
    @Query("SELECT l FROM Loan l WHERE l.customer.customerId = :customerId")
    List<Loan> findByCustomerId(@Param("customerId") Long customerId);
}