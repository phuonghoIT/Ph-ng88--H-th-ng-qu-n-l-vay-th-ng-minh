package com.example.demo.service;

import com.example.demo.entity.RepaymentSchedule;
import com.example.demo.repository.RepaymentScheduleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RepaymentScheduleService {

    @Autowired
    private RepaymentScheduleRepository scheduleRepository;

    public RepaymentSchedule getScheduleById(Long id) {
        return scheduleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("🔴 LỖI: Không tìm thấy kỳ trả nợ có ID là " + id));
    }

    public List<RepaymentSchedule> getSchedulesByLoanId(Long loanId) {
        return scheduleRepository.findByLoanLoanIdOrderByPeriodNumberAsc(loanId);
    }
}
