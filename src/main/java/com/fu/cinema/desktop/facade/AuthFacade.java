package com.fu.cinema.desktop.facade;

import com.fu.cinema.dto.SessionUser;
import com.fu.cinema.exception.BusinessException;
import com.fu.cinema.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthFacade {

    private final AuthService authService;

    /**
     * Xác thực tài khoản quản trị viên đăng nhập vào quầy vé desktop (BR-D1).
     */
    public SessionUser loginAdmin(String username, String password) {
        SessionUser user = authService.login(username, password);
        if (!user.isAdmin()) {
            throw new BusinessException("Ứng dụng quầy vé chỉ dành cho nhân viên (tài khoản ADMIN)");
        }
        return user;
    }

    /**
     * Bonus B2: Xác thực tài khoản khách hàng hoặc nhân viên trong chế độ Kiosk tự phục vụ.
     */
    public SessionUser loginKiosk(String username, String password) {
        return authService.login(username, password);
    }
}
