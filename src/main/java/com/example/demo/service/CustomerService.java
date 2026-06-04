package com.example.demo.service;

import com.example.demo.entity.Customers; // Khớp với Entity viết hoa có s của em
import com.example.demo.repository.CustomersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CustomerService {

    @Autowired
    private CustomersRepository customersRepository;

    /**
     * 🟢 1. Lấy danh sách tất cả khách hàng
     */
    public List<Customers> getAllCustomers() {
        return customersRepository.findAll();
    }

    /**
     * 🟢 2. Tìm khách hàng theo ID (Phục vụ xem Profile)
     */
    public Customers getCustomerById(Long id) {
        return customersRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("🔴 LỖI: Không tìm thấy khách hàng có ID là " + id));
    }

    /**
     * 🔵 3. Đăng ký khách hàng mới tinh (Mở tài khoản)
     * Nghiệp vụ: Chống trùng CCCD và tự kích hoạt 600 điểm tín dụng mặc định
     */
    @Transactional
    public Customers createCustomer(Customers newCustomer) {
        // 🌟 NGHIỆP VỤ 1: Kiểm tra xem số CCCD này đã có ông nào dùng chưa?
        if (customersRepository.existsByIdentityNumber(newCustomer.getIdentityNumber())) {
            throw new RuntimeException("🔴 LỖI: Số CCCD/Identity Number này đã tồn tại trên hệ thống!");
        }

        // 🌟 NGHIỆP VỤ 2: Người mới tinh chưa có lịch sử nợ, tự động cấp 600 điểm uy tín
        newCustomer.setCreditScore(600);

        return customersRepository.save(newCustomer);
    }

    /**
     * 🟡 4. Cập nhật thông tin cá nhân khách hàng
     * Nghiệp vụ: Cấm sửa đổi số CCCD (vì CCCD là cố định định danh), cấm sửa điểm tín dụng bằng tay
     */
    @Transactional
    public Customers updateCustomer(Long id, Customers updatedData) {
        // Tìm ông khách hàng cũ dưới DB lên
        Customers existingCustomer = this.getCustomerById(id);

        // Tiến hành đè các thông tin thay đổi thông thường (SĐT, Địa chỉ, Nghề nghiệp)
        existingCustomer.setFullName(updatedData.getFullName());
        existingCustomer.setSdt(updatedData.getSdt());
        existingCustomer.setAddress(updatedData.getAddress());
        existingCustomer.setJob(updatedData.getJob());

        // ❌ Tuyệt đối không viết lệnh setIdentityNumber và setCreditScore ở đây!
        // Muốn đổi điểm thì phải do con Robot quét nợ hoặc API tất toán khoản vay đổi, Admin không được sửa bừa.

        return customersRepository.save(existingCustomer);
    }

    /**
     * 🟢 5. Tìm khách hàng bằng số CCCD (Phục vụ tra cứu tại quầy giao dịch)
     * Nghiệp vụ: Nếu gõ sai số CCCD hoặc khách hàng chưa đăng ký -> Quăng lỗi 404 không tìm thấy
     */
    @Transactional
    public Customers getCustomerByIdentityNumber(String identityNumber) {
        return customersRepository.findByIdentityNumber(identityNumber)
                .orElseThrow(() -> new RuntimeException("🔴 LỖI: Không tìm thấy khách hàng nào có số CCCD là " + identityNumber));
    }
}