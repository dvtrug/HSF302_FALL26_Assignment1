package com.fu.cinema.service;

import com.fu.cinema.dto.CustomerForm;
import com.fu.cinema.enums.AccountStatus;
import com.fu.cinema.exception.BusinessException;
import com.fu.cinema.repository.AccountRepository;
import com.fu.cinema.repository.CustomerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CustomerAdminServiceTest {
    @Autowired private CustomerService service;
    @Autowired private CustomerRepository customers;
    @Autowired private AccountRepository accounts;
    @Autowired private PasswordEncoder encoder;

    private CustomerForm form() {
        CustomerForm f = new CustomerForm();
        f.setFullName(" Test Customer ");
        f.setEmail("NEW.CUSTOMER@example.com");
        f.setMobile("0987654321");
        f.setIdentityCard("123456789012");
        f.setBirthday(LocalDate.now().minusYears(20));
        f.setPassword("Customer@123");
        return f;
    }

    @Test
    void createsAccountAndCustomerWithEncodedPassword() {
        service.create(form());
        var account = accounts.findByUsername("new.customer@example.com").orElseThrow();
        assertThat(encoder.matches("Customer@123", account.getPassword())).isTrue();
        assertThat(account.getPassword()).isNotEqualTo("Customer@123");
        assertThat(customers.findByAccount_AccountId(account.getAccountId()).orElseThrow().getFullName())
                .isEqualTo("Test Customer");
    }

    @ParameterizedTest
    @ValueSource(strings = {"email", "mobile", "identityCard", "accountUsername"})
    void rejectsDuplicateIdentityWithoutCreatingRows(String field) {
        var f = form();
        switch (field) {
            case "email" -> f.setEmail("an.nv@gmail.com");
            case "mobile" -> f.setMobile("0901234567");
            case "identityCard" -> f.setIdentityCard("079200001234");
            case "accountUsername" -> f.setEmail("admin@fucinema.vn");
        }
        long before = accounts.count();
        assertThatThrownBy(() -> service.create(f)).isInstanceOf(BusinessException.class);
        assertThat(accounts.count()).isEqualTo(before);
    }

    @Test
    void rejectsUnderageCustomerAndMissingPassword() {
        var f = form();
        f.setBirthday(LocalDate.now().minusYears(13).plusDays(1));
        assertThatThrownBy(() -> service.create(f)).isInstanceOf(BusinessException.class)
                .hasMessageContaining("13");
        f.setBirthday(LocalDate.now().minusYears(13));
        f.setPassword("");
        assertThatThrownBy(() -> service.create(f)).isInstanceOf(BusinessException.class)
                .hasMessageContaining("Mật khẩu");
    }

    @Test
    void updateExcludesSelfPreservesBlankPasswordAndEncodesNewPassword() {
        var an = service.search("an.nv@gmail.com", 0, 10).getContent().getFirst();
        var f = service.getForm(an.getCustomerId());
        String oldHash = an.getAccount().getPassword();
        f.setPassword("");
        service.update(an.getCustomerId(), f);
        assertThat(an.getAccount().getPassword()).isEqualTo(oldHash);
        f.setEmail("changed@example.com");
        f.setPassword("NewPassword123");
        service.update(an.getCustomerId(), f);
        assertThat(an.getAccount().getUsername()).isEqualTo("changed@example.com");
        assertThat(encoder.matches("NewPassword123", an.getAccount().getPassword())).isTrue();
    }

    @Test
    void updateRejectsAnotherCustomersDuplicateMobile() {
        var an = service.search("an.nv@gmail.com", 0, 10).getContent().getFirst();
        var f = service.getForm(an.getCustomerId());
        f.setMobile("0912345678");
        assertThatThrownBy(() -> service.update(an.getCustomerId(), f))
                .isInstanceOf(BusinessException.class).hasMessageContaining("Số điện thoại");
    }

    @Test
    void locksAndUnlocksCustomerAccount() {
        var an = service.search("an.nv@gmail.com", 0, 10).getContent().getFirst();
        service.toggleLock(an.getCustomerId());
        assertThat(an.getAccount().getStatus()).isEqualTo(AccountStatus.LOCKED);
        service.toggleLock(an.getCustomerId());
        assertThat(an.getAccount().getStatus()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    void deletesCustomerAndAccountWhenNoBookingsExist() {
        service.create(form());
        var account = accounts.findByUsername("new.customer@example.com").orElseThrow();
        var c = customers.findByAccount_AccountId(account.getAccountId()).orElseThrow();
        assertThat(service.delete(c.getCustomerId())).isTrue();
        customers.flush();
        assertThat(customers.findById(c.getCustomerId())).isEmpty();
        assertThat(accounts.findById(account.getAccountId())).isEmpty();
    }

    @Test
    void onlyLocksCustomerWithBookings() {
        var an = service.search("an.nv@gmail.com", 0, 10).getContent().getFirst();
        assertThat(service.delete(an.getCustomerId())).isFalse();
        assertThat(customers.existsById(an.getCustomerId())).isTrue();
        assertThat(accounts.existsById(an.getAccount().getAccountId())).isTrue();
        assertThat(an.getAccount().getStatus()).isEqualTo(AccountStatus.LOCKED);
    }
}
