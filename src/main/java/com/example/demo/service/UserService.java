package com.example.demo.service;

import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // 1. Logic Đăng ký tài khoản mới (Tự động băm mật khẩu trước khi lưu)
    public User registerUser(User user) {
        // Băm mật khẩu trần thành chuỗi bảo mật BCrypt
        String hashedPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(hashedPassword);
        return userRepository.save(user);
    }

    // 2. Logic Đăng nhập (Đối chiếu mật khẩu trần với mật khẩu đã băm dưới DB)
    public String login(String username, String plainPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Tài khoản không tồn tại!"));

        // So sánh mật khẩu người dùng gõ vào với chuỗi băm dưới DB
        if (passwordEncoder.matches(plainPassword, user.getPassword())) {
            // Đăng nhập thành công!
            // Tạm thời trả về chuỗi thông báo kèm Role, tuần sau mình sẽ đổi chỗ này thành chuỗi JWT Token xịn.
            return "Đăng nhập thành công! Vai trò của bạn là: " + user.getRole();
        } else {
            throw new RuntimeException("Mật khẩu không chính xác!");
        }
    }

    // Logic đổi mật khẩu an toàn
    public void changePassword(String username, String oldPassword, String newPassword) {
        // 1. Tìm xem tài khoản có tồn tại không
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Tài khoản không tồn tại!"));

        // 2. Kiểm tra xem mật khẩu cũ nhập vào có khớp với mật khẩu đang lưu dưới DB không
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new RuntimeException("Mật khẩu cũ không chính xác!");
        }

        // 3. Nếu khớp, tiến hành băm mật khẩu mới và lưu lại
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}