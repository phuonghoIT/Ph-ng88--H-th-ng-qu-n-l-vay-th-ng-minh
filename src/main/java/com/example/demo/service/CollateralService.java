package com.example.demo.service;

import com.example.demo.entity.Collateral;
import com.example.demo.repository.CollateralRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CollateralService {

    @Autowired
    private CollateralRepository collateralRepository;

    public List<Collateral> getAllCollaterals() {
        return collateralRepository.findAll();
    }

    public Collateral getCollateralById(Long id) {
        return collateralRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("🔴 LỖI: Không tìm thấy tài sản thế chấp có ID là " + id));
    }

    public Collateral createCollateral(Collateral collateral) {
        return collateralRepository.save(collateral);
    }

    public Collateral updateCollateral(Long id, Collateral updatedData) {
        Collateral existing = getCollateralById(id);
        existing.setAssetType(updatedData.getAssetType());
        existing.setEstimatedValue(updatedData.getEstimatedValue());
        existing.setConversionRate(updatedData.getConversionRate());
        return collateralRepository.save(existing);
    }
}
