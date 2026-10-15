package com.fu.cinema.service;

import com.fu.cinema.dto.RegisterForm;
import com.fu.cinema.dto.SessionUser;
import com.fu.cinema.entity.Account;
import com.fu.cinema.entity.Customer;
import com.fu.cinema.enums.AccountStatus;
import com.fu.cinema.enums.Role;
import com.fu.cinema.exception.BusinessException;
import com.fu.cinema.repository.AccountRepository;
import com.fu.cinema.repository.CustomerRepository;
import com.fu.cinema.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthServiceImpl authService;

    private Account adminAccount;
    private Account customerAccount;
    private Account lockedAccount;
    private Customer customer;

    @BeforeEach
    void setUp() {
        adminAccount = new Account("admin", "encodedAdminPass", Role.ADMIN, AccountStatus.ACTIVE);
        adminAccount.setAccountId(1L);

        customerAccount = new Account("customer1", "encodedCustPass", Role.CUSTOMER, AccountStatus.ACTIVE);
        customerAccount.setAccountId(2L);

        lockedAccount = new Account("locked_user", "encodedLockedPass", Role.CUSTOMER, AccountStatus.LOCKED);
        lockedAccount.setAccountId(3L);

        customer = new Customer();
        customer.setCustomerId(10L);
        customer.setFullName("Nguyen Van A");
        customer.setAccount(customerAccount);
    }

    @Test
    @DisplayName("login: Đăng nhập Admin thành công")
    void login_Admin_Success() {
        when(accountRepository.findByUsername("admin")).thenReturn(Optional.of(adminAccount));
        when(passwordEncoder.matches("Admin@123", "encodedAdminPass")).thenReturn(true);

        SessionUser user = authService.login("admin", "Admin@123");

        assertNotNull(user);
        assertEquals(Role.ADMIN, user.getRole());
        assertTrue(user.isAdmin());
        assertFalse(user.isCustomer());
        assertEquals("admin", user.getUsername());
    }

    @Test
    @DisplayName("login: Đăng nhập Customer thành công")
    void login_Customer_Success() {
        when(accountRepository.findByUsername("customer1")).thenReturn(Optional.of(customerAccount));
        when(passwordEncoder.matches("123456", "encodedCustPass")).thenReturn(true);
        when(customerRepository.findByAccount_AccountId(2L)).thenReturn(Optional.of(customer));

        SessionUser user = authService.login("customer1", "123456");

        assertNotNull(user);
        assertEquals(Role.CUSTOMER, user.getRole());
        assertTrue(user.isCustomer());
        assertEquals(10L, user.getCustomerId());
        assertEquals("Nguyen Van A", user.getDisplayName());
    }

    @Test
    @DisplayName("login: Sai mật khẩu bắn BusinessException")
    void login_WrongPassword_ThrowsException() {
        when(accountRepository.findByUsername("admin")).thenReturn(Optional.of(adminAccount));
        when(passwordEncoder.matches("wrong", "encodedAdminPass")).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                authService.login("admin", "wrong")
        );
        assertTrue(ex.getMessage().contains("không chính xác"));
    }

    @Test
    @DisplayName("login: Tài khoản LOCKED bắn BusinessException (BR-03)")
    void login_LockedAccount_ThrowsException() {
        when(accountRepository.findByUsername("locked_user")).thenReturn(Optional.of(lockedAccount));
        when(passwordEncoder.matches("123456", "encodedLockedPass")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                authService.login("locked_user", "123456")
        );
        assertTrue(ex.getMessage().contains("bị khóa"));
    }

    @Test
    @DisplayName("register: Đăng ký thành công tạo Account và Customer (BR-01, BR-02)")
    void register_Success() {
        RegisterForm form = new RegisterForm(
                "newuser", "Password@123", "Password@123",
                "Tran Thi B", "b@example.com", "0987654321",
                "012345678901", LocalDate.now().minusYears(20)
        );

        when(accountRepository.existsByUsername("newuser")).thenReturn(false);
        when(customerRepository.existsByEmail("b@example.com")).thenReturn(false);
        when(customerRepository.existsByMobile("0987654321")).thenReturn(false);
        when(customerRepository.existsByIdentityCard("012345678901")).thenReturn(false);
        when(passwordEncoder.encode("Password@123")).thenReturn("encodedPassword");

        Account savedAccount = new Account("newuser", "encodedPassword", Role.CUSTOMER, AccountStatus.ACTIVE);
        savedAccount.setAccountId(99L);
        when(accountRepository.save(any(Account.class))).thenReturn(savedAccount);

        Customer savedCustomer = new Customer();
        savedCustomer.setCustomerId(88L);
        savedCustomer.setFullName("Tran Thi B");
        when(customerRepository.save(any(Customer.class))).thenReturn(savedCustomer);

        SessionUser sessionUser = authService.register(form);

        assertNotNull(sessionUser);
        assertEquals(Role.CUSTOMER, sessionUser.getRole());
        assertEquals("newuser", sessionUser.getUsername());
        assertEquals("Tran Thi B", sessionUser.getDisplayName());
        assertEquals(88L, sessionUser.getCustomerId());
    }

    @Test
    @DisplayName("register: Tuổi dưới 13 bị từ chối (BR-02)")
    void register_Under13_ThrowsException() {
        RegisterForm form = new RegisterForm(
                "kiduser", "Password@123", "Password@123",
                "Em Be", "kid@example.com", "0987654321",
                "012345678901", LocalDate.now().minusYears(10)
        );

        BusinessException ex = assertThrows(BusinessException.class, () ->
                authService.register(form)
        );
        assertTrue(ex.getMessage().contains("13 tuổi"));
    }

    @Test
    @DisplayName("register: Mật khẩu yếu không đủ chữ hoa, thường, số (BR-02)")
    void register_WeakPassword_ThrowsException() {
        RegisterForm form = new RegisterForm(
                "user1", "weakpass", "weakpass",
                "Nguyen C", "c@example.com", "0987654321",
                "012345678901", LocalDate.now().minusYears(18)
        );

        BusinessException ex = assertThrows(BusinessException.class, () ->
                authService.register(form)
        );
        assertTrue(ex.getMessage().contains("chữ hoa, chữ thường và chữ số"));
    }
}
