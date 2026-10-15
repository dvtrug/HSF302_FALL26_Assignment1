package com.fu.cinema.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class CustomerServiceTest {
    @Autowired(required = false)
    private CustomerService service;

    @Test
    void searchesSeedCustomersByNamePhoneAndEmail() {
        assertThat(service).as("CustomerService must be available").isNotNull();
        assertThat(service.search("nguyen", 0, 10).getContent())
                .extracting("fullName").containsExactly("Nguyen Van An");
        assertThat(service.search("0912", 0, 10).getContent())
                .extracting("fullName").containsExactly("Tran Thi Binh");
        assertThat(service.search("GMAIL", 0, 10).getTotalElements()).isEqualTo(2);
    }

    @Test
    void paginatesSortsAndAcceptsBlankKeywordAndNegativePage() {
        assertThat(service).as("CustomerService must be available").isNotNull();
        var first = service.search("   ", -1, 1);
        var second = service.search(null, 1, 1);
        assertThat(first.getTotalElements()).isEqualTo(2);
        assertThat(first.getContent()).extracting("fullName").containsExactly("Nguyen Van An");
        assertThat(second.getContent()).extracting("fullName").containsExactly("Tran Thi Binh");
        assertThat(service.search("no-such-customer", 0, 10)).isEmpty();
    }
}
