package com.fu.cinema.service.impl;

import com.fu.cinema.dto.RegisterForm;
import com.fu.cinema.dto.SessionUser;
import com.fu.cinema.entity.Account;
import com.fu.cinema.entity.Customer;
import com.fu.cinema.enums.AccountStatus;
import com.fu.cinema.enums.Role;
import com.fu.cinema.exception.BusinessException;
import com.fu.cinema.repository.AccountRepository;
import com.fu.cinema.repository.CustomerRepository;
import com.fu.cinema.service.AuthService;
import com.fu.cinema.util.ValidationUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public SessionUser login(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new BusinessException("Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu");
        }

        Account account = accountRepository.findByUsername(username.trim())
                .orElseThrow(() -> new BusinessException("Tên đăng nhập hoặc mật khẩu không chính xác"));

        if (!passwordEncoder.matches(password, account.getPassword())) {
            throw new BusinessException("Tên đăng nhập hoặc mật khẩu không chính xác");
        }

        // BR-03: Tài khoản LOCKED không đăng nhập được
        if (account.getStatus() == AccountStatus.LOCKED) {
            throw new BusinessException("Tài khoản của bạn đã bị khóa. Vui lòng liên hệ quản trị viên.");
        }

        Long customerId = null;
        String displayName = account.getUsername();

        if (account.getRole() == Role.CUSTOMER) {
            Customer customer = customerRepository.findByAccount_AccountId(account.getAccountId())
                    .orElse(null);
            if (customer != null) {
                customerId = customer.getCustomerId();
                displayName = customer.getFullName();
            }
        } else if (account.getRole() == Role.ADMIN) {
            displayName = "Quản trị viên";
        }

        return SessionUser.builder()
                .accountId(account.getAccountId())
                .customerId(customerId)
                .username(account.getUsername())
                .displayName(displayName)
                .role(account.getRole())
                .build();
    }

    @Override
    @Transactional
    public SessionUser register(RegisterForm form) {
        if (form == null) {
            throw new BusinessException("Thông tin đăng ký không hợp lệ");
        }

        // Validate confirm password
        if (form.getConfirmPassword() == null || !form.getPassword().equals(form.getConfirmPassword())) {
            throw new BusinessException("Mật khẩu xác nhận không khớp");
        }

        // BR-02: Mật khẩu ≥ 8 ký tự, có chữ hoa, chữ thường và chữ số
        String pwd = form.getPassword();
        if (pwd.length() < 8 || !pwd.matches(".*[A-Z].*") || !pwd.matches(".*[a-z].*") || !pwd.matches(".*\\d.*")) {
            throw new BusinessException("Mật khẩu phải từ 8 ký tự trở lên, bao gồm chữ hoa, chữ thường và chữ số");
        }

        // BR-02: Khách hàng đăng ký phải từ đủ 13 tuổi trở lên
        if (form.getBirthday() == null || ValidationUtils.age(form.getBirthday()) < 13) {
            throw new BusinessException("Khách hàng phải từ đủ 13 tuổi trở lên để đăng ký tài khoản");
        }

        // BR-01: Username/Email, Mobile (10 chữ số, bắt đầu bằng 0), IdentityCard (12 chữ số) không trùng
        String username = form.getUsername().trim();
        String email = form.getEmail().trim();
        String mobile = form.getMobile().trim();
        String identityCard = form.getIdentityCard().trim();

        if (accountRepository.existsByUsername(username)) {
            throw new BusinessException("Tên đăng nhập '" + username + "' đã được sử dụng");
        }
        if (customerRepository.existsByEmail(email)) {
            throw new BusinessException("Email '" + email + "' đã được sử dụng");
        }
        if (customerRepository.existsByMobile(mobile)) {
            throw new BusinessException("Số điện thoại '" + mobile + "' đã được sử dụng");
        }
        if (customerRepository.existsByIdentityCard(identityCard)) {
            throw new BusinessException("Số CCCD '" + identityCard + "' đã được sử dụng");
        }

        // Tạo Account + Customer trong 1 transaction
        Account account = new Account(
                username,
                passwordEncoder.encode(pwd),
                Role.CUSTOMER,
                AccountStatus.ACTIVE
        );
        account = accountRepository.save(account);

        Customer customer = new Customer();
        customer.setFullName(form.getFullName().trim());
        customer.setEmail(email);
        customer.setMobile(mobile);
        customer.setIdentityCard(identityCard);
        customer.setBirthday(form.getBirthday());
        customer.setAccount(account);
        customer = customerRepository.save(customer);

        return SessionUser.builder()
                .accountId(account.getAccountId())
                .customerId(customer.getCustomerId())
                .username(account.getUsername())
                .displayName(customer.getFullName())
                .role(account.getRole())
                .build();
    }
}
