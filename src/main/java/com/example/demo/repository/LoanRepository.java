package com.example.demo.repository;

import com.example.demo.entity.Loan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {
    List<Loan> findByCustomer_User_Username(String username);

    // Tìm các khoản vay dựa vào ID khách hàng
    @Query("SELECT l FROM Loan l WHERE l.customer.customerId = :customerId")
    List<Loan> findByCustomerId(@Param("customerId") Long customerId);

    //Giao cong viec cho thang khac
    @Modifying
    @Query(value = "UPDATE loans " +
            "SET employee_id = :p_new_emp_id " +
            "WHERE status != 'PAID' " +
            "  AND employee_id = :p_old_emp_id", nativeQuery = true) // 1. Sửa lỗi dấu nháy kép thừa ở đây
    void shipWorkNative(@Param("p_old_emp_id") Long pOldEmpId,
                        @Param("p_new_emp_id") Long pNewEmpId);


}