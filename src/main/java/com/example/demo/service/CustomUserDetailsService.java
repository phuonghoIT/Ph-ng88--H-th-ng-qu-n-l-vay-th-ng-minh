package com.example.demo.service;

import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 1. Mò xuống DB bốc tài khoản lên bằng username
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy tài khoản: " + username));

        // 2. Ép Role từ chuỗi chữ (CUSTOMER/STAFF/MANAGER) thành cấu hình Quyền hạn chuẩn của Spring
        // Spring Security bắt buộc quyền phải có tiền tố "ROLE_" ở đầu (Ví dụ: ROLE_CUSTOMER)
        String roleWithPrefix = "ROLE_" + user.getRole();

        // 3. Đóng gói thành đối tượng UserDetails xịn sò để trả về cho Spring Security gác cổng
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword()) // Mật khẩu đã băm BCrypt dưới DB
                .authorities(roleWithPrefix)   // Gán quyền hạn cho tài khoản
                .build();
    }
}