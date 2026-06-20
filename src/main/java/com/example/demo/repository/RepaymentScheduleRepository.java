package com.example.demo.repository;

import com.example.demo.entity.RepaymentSchedule;
import com.example.demo.entity.ScheduleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface RepaymentScheduleRepository extends JpaRepository<RepaymentSchedule, Long> {

    // Lấy ra lịch trả nợ của một khoản vay cụ thể và sắp xếp theo số thứ tự kỳ (Kỳ 1, Kỳ 2...)
    List<RepaymentSchedule> findByLoanLoanIdOrderByPeriodNumberAsc(Long loanId);
    List<RepaymentSchedule> findByDueDateBeforeAndStatus(LocalDate targetDate, ScheduleStatus targetStatus);
}
