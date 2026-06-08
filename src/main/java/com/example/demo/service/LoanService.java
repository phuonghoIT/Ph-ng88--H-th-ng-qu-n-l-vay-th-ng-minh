package com.example.demo.service;

import com.example.demo.entity.*;

import com.example.demo.repository.LoanProductRepository;
import com.example.demo.repository.LoanRepository;
import com.example.demo.repository.RepaymentScheduleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
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
        // 1. Tìm khoản vay cũ đang nằm im trong Database (Tương đương biến OLD trong Trigger)
        Loan oldLoan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khoản vay!"));
        if (oldLoan.getStatus() == LoanStatus.PAID) {
            throw new RuntimeException("🔴 LỖI: Khoản vay đã tất toán xong, không được phép thay đổi trạng thái!");
        }

        // 2. Tiến hành cập nhật trạng thái mới (Hành vi hợp lệ)
        oldLoan.setStatus(newStatus);

        // 3. Bắn lệnh lưu xuống DB mượt mà
        return loanRepository.save(oldLoan);
    }

    public List<Loan> getMyLoans() {
        // 1. Bốc thông tin Principal (đối tượng chứng thực) từ bộ nhớ gác cổng của Spring
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        String username;
        if (principal instanceof UserDetails) {
            username = ((UserDetails) principal).getUsername();
        } else {
            username = principal.toString();
        }

        // 2. Gọi Repository quét xuống DB lọc đúng ID của ông này
        return loanRepository.findByCustomer_User_Username(username);
    }

    @Transactional
    public Loan createLoanAndGenerateSchedule(Loan newLoan) {
        // 🌟 SỬA BƯỚC 1 + 2: Tìm gói sản phẩm thật từ DB lên trước để có cấu hình lãi suất và tháng
        if (newLoan.getLoanProduct() == null || newLoan.getLoanProduct().getLoanProductId() == null) {
            throw new RuntimeException("🔴 LỖI: Khoản vay đăng ký bắt buộc phải chọn một gói sản phẩm hợp lệ!");
        }

        LoanProduct product = loanProductRepository.findById(newLoan.getLoanProduct().getLoanProductId())
                .orElseThrow(() -> new RuntimeException("🔴 LỖI: Không tìm thấy gói sản phẩm vay trên hệ thống!"));

        // Gán ngược gói sản phẩm đầy đủ thông tin vào đối tượng vay trước khi lưu
        newLoan.setLoanProduct(product);

        // Bây giờ mới lưu khoản vay xuống DB để lấy loan_id thật
        Loan savedLoan = loanRepository.save(newLoan);

        // 3. Tiến hành lấy cấu hình làm toán (Bảo đảm 100% không lo bị Null nữa!)
        int duration = product.getDurationMonths();
        BigDecimal interestRate = product.getInterestRate();

        BigDecimal allAmount = savedLoan.getAmount();
        BigDecimal principalPerMonth = allAmount.divide(BigDecimal.valueOf(duration), 2, RoundingMode.HALF_UP);
        BigDecimal remaining = allAmount;
        LocalDate loanDate = savedLoan.getLoanDate();

        List<RepaymentSchedule> schedulesList = new ArrayList<>();

        // 4. Vòng lặp FOR bên dưới của em viết RẤT CHUẨN, giữ nguyên 100% không cần sửa gì cả ...
        for (int period = 1; period <= duration; period++) {
            BigDecimal monthlyRate = interestRate.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP)
                    .divide(BigDecimal.valueOf(12), 6, RoundingMode.HALF_UP);
            BigDecimal interestAmount = remaining.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);

            LocalDate dueDates = loanDate.plusMonths(period);

            RepaymentSchedule schedule = RepaymentSchedule.builder()
                    .dueDate(dueDates)
                    .interestAmount(interestAmount)
                    .penaltyAmount(BigDecimal.ZERO)
                    .periodNumber(period)
                    .principalAmount(principalPerMonth)
                    .status(ScheduleStatus.UNPAID)
                    .loan(savedLoan)
                    .build();

            schedulesList.add(schedule);
            remaining = remaining.subtract(principalPerMonth);
        }

        scheduleRepository.saveAll(schedulesList);
        return savedLoan;
    }
}