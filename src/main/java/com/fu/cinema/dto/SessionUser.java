package com.fu.cinema.dto;

import com.fu.cinema.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionUser implements Serializable {

    private Long accountId;
    private Long customerId;
    private String username;
    private String displayName;
    private Role role;

    public boolean isAdmin() {
        return Role.ADMIN.equals(this.role);
    }

    public boolean isCustomer() {
        return Role.CUSTOMER.equals(this.role);
    }
}
