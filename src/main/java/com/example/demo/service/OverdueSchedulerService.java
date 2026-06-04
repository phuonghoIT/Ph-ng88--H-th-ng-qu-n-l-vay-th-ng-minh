package com.example.demo.service;

import com.example.demo.entity.Customers;
import com.example.demo.entity.RepaymentSchedule;
import com.example.demo.entity.ScheduleStatus;
import com.example.demo.repository.CustomersRepository;
import com.example.demo.repository.RepaymentScheduleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class OverdueSchedulerService {

    @Autowired
    private RepaymentScheduleRepository scheduleRepository;

    @Autowired
    private CustomersRepository customersRepository;

    /**
     * ⏰ TỰ ĐỘNG CHẠY BAN ĐÊM LÚC 00:00
     */
    @Scheduled(fixedRate = 10000)
    @Transactional
    public void runOverdueScannerJob() {
        System.out.println("🚀 [ROBOT JAVA] Bắt đầu tiến trình quét nợ quá hạn và xử lý điểm uy tín...");
        LocalDate today = LocalDate.now();

        // 🛑 BƯỚC 1: QUÉT & CẬP NHẬT TRẠNG THÁI QUÁ HẠN
        // Tìm tất cả kỳ hạn có due_date trước ngày hôm nay mà vẫn 'UNPAID'
        List<RepaymentSchedule> overdueSchedules = scheduleRepository.findByDueDateBeforeAndStatus(today, ScheduleStatus.UNPAID);

        if (overdueSchedules.isEmpty()) {
            System.out.println("✅ [ROBOT JAVA] Không có khoản nợ nào bị quá hạn ngày hôm nay.");
            return;
        }

        for (RepaymentSchedule schedule : overdueSchedules) {
            // 1. Đổi trạng thái sang OVERDUE
            schedule.setStatus(ScheduleStatus.OVERDUE);

            // 2. TÍNH TIỀN PHẠT (PENALTY AMOUNT)
            // Lấy tỷ lệ phạt từ gói sản phẩm vay gắn liền với khoản vay đó
            BigDecimal penaltyRate = schedule.getLoan().getLoanProduct().getPenaltyRate();
            BigDecimal principal = schedule.getPrincipalAmount();
            BigDecimal interest = schedule.getInterestAmount();

            // Công thức: (Gốc + Lãi) * penaltyRate / 100
            BigDecimal totalDue = principal.add(interest);
            BigDecimal penaltyAmount = totalDue.multiply(penaltyRate).divide(BigDecimal.valueOf(100));
            schedule.setPenaltyAmount(penaltyAmount);

            // 3. TRỪ THẲNG TAY 20 ĐIỂM TÍN DỤNG CỦA KHÁCH HÀNG
            // Kiểm tra xem kỳ hạn này có phải vừa mới quá hạn vào ngày hôm qua hay không (để chỉ trừ điểm 1 lần duy nhất)
            if (schedule.getDueDate().equals(today.minusDays(1))) {
                Customers customer = schedule.getLoan().getCustomer();
                int currentScore = customer.getCreditScore();

                // Trừ 20 điểm nhưng không được để điểm tụt xuống dưới 0
                int newScore = Math.max(currentScore - 20, 0);
                customer.setCreditScore(newScore);

                // Lưu lại sự thay đổi của khách hàng
                customersRepository.save(customer);
                System.out.println("⚠️ [ROBOT JAVA] Khách hàng " + customer.getFullName() + " bị trừ 20 điểm tín dụng. Điểm mới: " + newScore);
            }
        }

        // 🛑 BƯỚC 2: ĐỔ BÊ TÔNG XUỐNG DATABASE
        // Chỉ cần gọi saveAll, JPA sẽ tự sinh câu lệnh UPDATE hàng loạt xuống đĩa cứng
        scheduleRepository.saveAll(overdueSchedules);
        System.out.println("✅ [ROBOT JAVA] Đã cập nhật xong trạng thái và tiền phạt cho " + overdueSchedules.size() + " kỳ hạn.");
    }
}