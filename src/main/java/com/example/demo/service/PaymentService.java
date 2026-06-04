package com.example.demo.service;

import com.example.demo.entity.Payment;
import com.example.demo.entity.RepaymentSchedule;
import com.example.demo.entity.ScheduleStatus;
import com.example.demo.repository.PaymentRepository;
import com.example.demo.repository.RepaymentScheduleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }

    public Payment getPaymentById(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("🔴 LỖI: Không tìm thấy giao dịch thanh toán có ID là " + id));
    }

    @Autowired
    private RepaymentScheduleRepository scheduleRepository;

    @Transactional
    public Payment processPayment(Payment newPayment) {
        // 1. Kiểm tra an toàn đầu vào: Hóa đơn nộp tiền phải gắn với một kỳ hạn lịch trình
        if (newPayment.getRepaymentSchedule() == null || newPayment.getRepaymentSchedule().getScheduleId() == null) {
            throw new RuntimeException("🔴 LỖI: Hóa đơn thanh toán bắt buộc phải chỉ định mã kỳ hạn nợ!");
        }

        Long scheduleId = newPayment.getRepaymentSchedule().getScheduleId();

        // 2. Lấy thông tin kỳ hạn nợ gốc từ DB lên (Tương đương bảng repayment_schedules)
        RepaymentSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new RuntimeException("🔴 LỖI: Không tìm thấy kỳ hạn trả nợ trên hệ thống!"));

        // Nếu kỳ này đã trả xong (PAID) rồi thì cấm không cho nộp thêm tiền vào nữa
        if (schedule.getStatus() == ScheduleStatus.PAID) {
            throw new RuntimeException("🔴 LỖI: Kỳ hạn này đã được thanh toán hoàn tất trước đó!");
        }

        // 3. Tính số tiền hệ thống bắt buộc phải thu ở kỳ này = Gốc + Lãi + Phạt
        BigDecimal requiredAmount = schedule.getPrincipalAmount()
                .add(schedule.getInterestAmount())
                .add(schedule.getPenaltyAmount());

        // 4. 🌟 MẸO CHÍ MẠNG TRÊN JAVA (Thay thế cho BEFORE Trigger):
        // Bước A: Quét bảng payments để tính tổng tiền khách đã nộp TRONG QUÁ KHỨ cho riêng kỳ này
        List<Payment> pastPayments = paymentRepository.findByRepaymentScheduleScheduleId(scheduleId);

        BigDecimal pastPaidSum = pastPayments.stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Bước B: Tổng tiền thực tế = Tiền quá khứ + Tiền khách đang đứng ở quầy nộp (newPayment.getAmount())
        BigDecimal totalPaidRealtime = pastPaidSum.add(newPayment.getAmount());

        // 5. Kiểm tra xem tổng tiền đã đủ đô để xóa nợ kỳ này chưa
        if (totalPaidRealtime.compareTo(requiredAmount) >= 0) {

            // Đổi trạng thái kỳ hạn sang PAID
            schedule.setStatus(ScheduleStatus.PAID);
            scheduleRepository.save(schedule);

        } // Nếu chưa đủ tiền, giữ nguyên trạng thái UNPAID/OVERDUE để khách gom tiền nộp tiếp lần sau

        // 6. Ghi nhận dòng tiền mới vào bảng payments (Chính thức INSERT xuống DB)
        return paymentRepository.save(newPayment);
    }
}