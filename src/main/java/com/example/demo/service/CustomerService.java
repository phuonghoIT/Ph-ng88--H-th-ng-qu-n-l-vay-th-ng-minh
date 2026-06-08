package com.example.demo.service;

import com.example.demo.entity.Customer;
import com.example.demo.entity.User;
import com.example.demo.repository.CustomersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder; // Thêm import
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CustomerService {

    @Autowired
    private CustomersRepository customersRepository;

    @Autowired
    private PasswordEncoder passwordEncoder; // 🌟 SỬA LỖI: Khai báo và inject PasswordEncoder

    /**
     * 🟢 1. Lấy danh sách tất cả khách hàng
     */
    public List<Customer> getAllCustomers() {
        return customersRepository.findAll();
    }

    /**
     * 🟢 2. Tìm khách hàng theo ID (Phục vụ xem Profile)
     */
    public Customer getCustomerById(Long id) {
        return customersRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("🔴 LỖI: Không tìm thấy khách hàng có ID là " + id));
    }

    /**
     * 🔵 3. Đăng ký khách hàng mới tinh (Mở tài khoản)
     * Nghiệp vụ: Chống trùng CCCD và tự kích hoạt 600 điểm tín dụng mặc định
     */
    @Transactional
    public Customer createCustomer(Customer newCustomer) {
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
    public Customer updateCustomer(Long id, Customer updatedData) {
        // Tìm ông khách hàng cũ dưới DB lên
        Customer existingCustomer = this.getCustomerById(id);

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
    public Customer getCustomerByIdentityNumber(String identityNumber) {
        return customersRepository.findByIdentityNumber(identityNumber)
                .orElseThrow(() -> new RuntimeException("🔴 LỖI: Không tìm thấy khách hàng nào có số CCCD là " + identityNumber));
    }

    @Transactional
    public Customer createCustomerWithAccount(Customer customer) {
        // 1. Kiểm tra xem Request gửi lên có kèm thông tin tài khoản không
        if (customer.getUser() != null) {
            User account = customer.getUser();

            // 2. Ép băm mật khẩu bảo mật trước khi lưu
            account.setPassword(passwordEncoder.encode(account.getPassword()));
            account.setRole("CUSTOMER"); // Mặc định quyền là Khách hàng

            // 3. Thiết lập mối quan hệ 2 chiều để JPA biết đường sinh khóa ngoại
            account.setCustomer(customer);
            customer.setUser(account);
        }

        // 4. Lưu một phát ăn cả hai bảng luôn nhờ Cascade!
        return customersRepository.save(customer);
    }
}
