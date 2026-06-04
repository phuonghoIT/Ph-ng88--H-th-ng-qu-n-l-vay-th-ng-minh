package com.example.demo.service;

import com.example.demo.entity.LoanProduct;
import com.example.demo.repository.LoanProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LoanProductService {

    @Autowired
    private LoanProductRepository loanProductRepository;

    public List<LoanProduct> getAllLoanProducts() {
        return loanProductRepository.findAll();
    }

    public LoanProduct getLoanProductById(Long id) {
        return loanProductRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("🔴 LỖI: Không tìm thấy gói sản phẩm vay có ID là " + id));
    }

    public LoanProduct createLoanProduct(LoanProduct product) {
        return loanProductRepository.save(product);
    }

    public LoanProduct updateLoanProduct(Long id, LoanProduct updatedData) {
        LoanProduct existing = getLoanProductById(id);
        existing.setName(updatedData.getName());
        existing.setInterestRate(updatedData.getInterestRate());
        existing.setPenaltyRate(updatedData.getPenaltyRate());
        existing.setDurationMonths(updatedData.getDurationMonths());
        return loanProductRepository.save(existing);
    }
}
