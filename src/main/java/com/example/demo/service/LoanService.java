package com.example.demo.service;

import com.example.demo.entity.*;
import com.example.demo.repository.LoanProductRepository;
import com.example.demo.repository.LoanRepository;
import com.example.demo.repository.RepaymentScheduleRepository;
import com.example.demo.security.CustomUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class LoanService {

    @Autowired
    private LoanRepository loanRepository;

    public List<Loan> getAllLoans() {
        return loanRepository.findAll();
    }

    public Loan getLoanById(Long id) {
        return loanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("🔴 LỖI: Không tìm thấy khoản vay có ID là " + id));
    }

    public List<Loan> getLoansByCustomerId(Long customerId) {
        return loanRepository.findByCustomerId(customerId);
    }

    @Autowired
    private LoanProductRepository loanProductRepository;

    @Autowired
    private RepaymentScheduleRepository scheduleRepository;

    @Transactional
    public Loan updateLoanStatus(Long loanId, LoanStatus newStatus) {
        Loan oldLoan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khoản vay!"));
        if (oldLoan.getStatus() == LoanStatus.PAID) {
            throw new RuntimeException("🔴 LỖI: Khoản vay đã tất toán xong, không được phép thay đổi trạng thái!");
        }
        oldLoan.setStatus(newStatus);
        return loanRepository.save(oldLoan);
    }

    public List<Loan> getMyLoans() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String username = ((CustomUserDetails) principal).getUsername();
        return loanRepository.findByCustomer_User_Username(username);
    }

    @Transactional
    public Loan createLoanAndGenerateSchedule(Loan newLoan) {
        // 🌟 BƯỚC 1: LẤY THÔNG TIN NGƯỜI DÙNG TỪ TOKEN (Bất khả xâm phạm)
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        CustomUserDetails userDetails = (CustomUserDetails) principal;
        Long customerId = userDetails.getCustomerId();

        // Nếu người đăng nhập không phải là khách hàng (ví dụ: STAFF), cấm tạo vay
        if (customerId == null) {
            throw new IllegalStateException("Chỉ có khách hàng mới được phép đăng ký khoản vay.");
        }

        // 🌟 BƯỚC 2: TẠO ĐỐI TƯỢNG CUSTOMER ĐÁNG TIN CẬY VÀ GHI ĐÈ
        Customer authenticatedCustomer = new Customer();
        authenticatedCustomer.setCustomerId(customerId);
        newLoan.setCustomer(authenticatedCustomer); // Ghi đè, không tin frontend

        // --- Các logic còn lại giữ nguyên ---
        if (newLoan.getLoanProduct() == null || newLoan.getLoanProduct().getLoanProductId() == null) {
            throw new RuntimeException("🔴 LỖI: Khoản vay đăng ký bắt buộc phải chọn một gói sản phẩm hợp lệ!");
        }

        LoanProduct product = loanProductRepository.findById(newLoan.getLoanProduct().getLoanProductId())
                .orElseThrow(() -> new RuntimeException("🔴 LỖI: Không tìm thấy gói sản phẩm vay trên hệ thống!"));

        newLoan.setLoanProduct(product);
        Loan savedLoan = loanRepository.save(newLoan);

        int duration = product.getDurationMonths();
        BigDecimal interestRate = product.getInterestRate();
        BigDecimal allAmount = savedLoan.getAmount();
        BigDecimal principalPerMonth = allAmount.divide(BigDecimal.valueOf(duration), 2, RoundingMode.HALF_UP);
        BigDecimal remaining = allAmount;
        LocalDate loanDate = savedLoan.getLoanDate();
        List<RepaymentSchedule> schedulesList = new ArrayList<>();
        BigDecimal monthlyRate = interestRate.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP)
                .divide(BigDecimal.valueOf(12), 6, RoundingMode.HALF_UP);

        for (int period = 1; period <= duration; period++) {
            BigDecimal interestAmount = remaining.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);
            LocalDate dueDates = loanDate.plusMonths(period);
            BigDecimal currentPrincipal;
            if (period == duration) {
                currentPrincipal = remaining;
            } else {
                currentPrincipal = principalPerMonth;
            }
            RepaymentSchedule schedule = RepaymentSchedule.builder()
                    .dueDate(dueDates)
                    .interestAmount(interestAmount)
                    .penaltyAmount(BigDecimal.ZERO)
                    .periodNumber(period)
                    .principalAmount(currentPrincipal)
                    .status(ScheduleStatus.UNPAID)
                    .loan(savedLoan)
                    .build();
            schedulesList.add(schedule);
            remaining = remaining.subtract(currentPrincipal);
        }

        scheduleRepository.saveAll(schedulesList);
        return savedLoan;
    }
}
