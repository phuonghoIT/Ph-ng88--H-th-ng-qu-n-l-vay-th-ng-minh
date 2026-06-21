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

    @Autowired // SỬA LỖI: Thêm @Autowired để Spring inject LoanRepository
    private LoanRepository loanRepository;

    public List<Collateral> getAllCollaterals() {
        return collateralRepository.findAll();
    }

    public Collateral getCollateralById(Long id) {
        return collateralRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("🔴 LỖI: Không tìm thấy tài sản thế chấp có ID là " + id));
    }

    @Transactional
    public Collateral createCollateral(Collateral newCollateral) {
        // 1. Kiểm tra xem đối tượng đầu vào có gắn với một khoản vay không.
        if (newCollateral.getLoan() == null || newCollateral.getLoan().getLoanId() == null) {
            throw new IllegalArgumentException("LỖI LOGIC: Tài sản thế chấp phải được gắn vào một khoản vay hợp lệ.");
        }

        Long loanId = newCollateral.getLoan().getLoanId();

        // 2. Lấy thông tin khoản vay ĐẦY ĐỦ từ Database.
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("LỖI LOGIC: Không tìm thấy khoản vay với ID: " + loanId + " để gắn tài sản thế chấp."));

        // 3. SỬA LỖI: Kiểm tra loại khoản vay cho đúng với ENUM là THE_CHAP
        if (loan.getLoanType() != LoanType.THE_CHAP) {
            throw new IllegalStateException("LỖI LOGIC: Chỉ có thể thêm tài sản thế chấp cho các khoản vay có loại là 'THE_CHAP'. " +
                    "Khoản vay (ID: " + loanId + ") hiện đang có loại là '" + loan.getLoanType() + "'.");
        }
        
        // 4. Kiểm tra giá trị tài sản
        if (newCollateral.getEstimatedValue() == null || newCollateral.getEstimatedValue().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("LỖI LOGIC: Giá trị ước tính của tài sản thế chấp phải lớn hơn 0.");
        }

        // 5. Nếu tất cả các quy tắc đều được thỏa mãn, cho phép lưu vào DB.
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
