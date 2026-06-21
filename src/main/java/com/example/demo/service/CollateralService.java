package com.example.demo.service;

import com.example.demo.entity.Collateral;
import com.example.demo.entity.Loan;
import com.example.demo.entity.LoanType;
import com.example.demo.repository.CollateralRepository;
import com.example.demo.repository.LoanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CollateralService {

    @Autowired
    private CollateralRepository collateralRepository;
    private LoanRepository loanRepository ;

    public List<Collateral> getAllCollaterals() {
        return collateralRepository.findAll();
    }

    public Collateral getCollateralById(Long id) {
        return collateralRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("🔴 LỖI: Không tìm thấy tài sản thế chấp có ID là " + id));
    }

    @Transactional
    public Collateral createCollateral(Collateral newCollateral) {

        // =================================================================
        // QUY TẮC 1: KIỂM TRA GIÁ TRỊ ƯỚC TÍNH CỦA TÀI SẢN
        // Tương đương: CHECK (estimated_value > 0)
        // =================================================================
        if (newCollateral.getEstimatedValue() == null || newCollateral.getEstimatedValue().compareTo(BigDecimal.ZERO) <= 0) {
            // Ném ra một ngoại lệ để dừng quá trình và báo lỗi.
            throw new IllegalArgumentException("LỖI LOGIC: Giá trị ước tính của tài sản thế chấp phải lớn hơn 0.");
        }

        // =================================================================
        // QUY TẮC 2: KIỂM TRA LOẠI CỦA KHOẢN VAY LIÊN QUAN
        // =================================================================

        // 2a. Kiểm tra xem đối tượng đầu vào có gắn với một khoản vay không.
        if (newCollateral.getLoan() == null || newCollateral.getLoan().getLoanId() == null) {
            throw new IllegalArgumentException("LỖI LOGIC: Tài sản thế chấp phải được gắn vào một khoản vay hợp lệ.");
        }

        Long loanId = newCollateral.getLoan().getLoanId();

        // 2b. Lấy thông tin khoản vay ĐẦY ĐỦ từ Database.
        // Đây là bước quan trọng để đảm bảo dữ liệu là đáng tin cậy, không tin vào dữ liệu thô từ client.
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("LỖI LOGIC: Không tìm thấy khoản vay với ID: " + loanId + " để gắn tài sản thế chấp."));

        // 2c. Kiểm tra loại khoản vay.
        if (loan.getLoanType() != LoanType.THE_CHAP) {
            // Ném ra ngoại lệ nếu loại khoản vay không phải là 'SECURED'.
            throw new IllegalStateException("LỖI LOGIC: Chỉ có thể thêm tài sản thế chấp cho các khoản vay có loại là SECURED. " +
                    "Khoản vay (ID: " + loanId + ") hiện đang có loại là \"" + loan.getLoanType() + "\".");
        }

        // =================================================================
        // Nếu tất cả các quy tắc đều được thỏa mãn, cho phép lưu vào DB.
        // =================================================================
        return collateralRepository.save(newCollateral);
    }

    @Transactional
    public Collateral updateCollateral(Long id, Collateral updatedData) {
        Collateral existing = getCollateralById(id);
        existing.setAssetType(updatedData.getAssetType());
        existing.setEstimatedValue(updatedData.getEstimatedValue());
        existing.setConversionRate(updatedData.getConversionRate());
        return collateralRepository.save(existing);
    }
}
