package com.zenyrahr.hrms.controller;

import com.zenyrahr.hrms.Repository.OrganizationRepository;
import com.zenyrahr.hrms.Repository.UserRepository;
import com.zenyrahr.hrms.model.Employee;
import com.zenyrahr.hrms.service.EmployeeCodeService;
import com.zenyrahr.hrms.service.EmployeeLeaveBalanceService;
import com.zenyrahr.hrms.service.OrganizationRoleService;
import com.zenyrahr.hrms.service.OrganizationService;
import com.zenyrahr.hrms.service.SequenceService;
import com.zenyrahr.hrms.service.TenantAccessService;
import com.zenyrahr.hrms.service.UserDetailsService;
import com.zenyrahr.hrms.utils.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@ExtendWith(MockitoExtension.class)
class AuthControllerRefreshTokenTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private OrganizationRepository organizationRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private UserDetailsService userDetailsService;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private TenantAccessService tenantAccessService;
    @Mock
    private EmployeeLeaveBalanceService employeeLeaveBalanceService;
    @Mock
    private EmployeeCodeService employeeCodeService;
    @Mock
    private OrganizationRoleService organizationRoleService;
    @Mock
    private OrganizationService organizationService;
    @Mock
    private SequenceService sequenceService;

    @InjectMocks
    private AuthController authController;

    @Test
    void refreshTokenReturnsNewAccessTokenForValidRefreshToken() {
        String refreshToken = "valid-refresh-token";
        Employee employee = new Employee();
        employee.setUsername("test@example.com");

        when(jwtUtil.extractUsername(refreshToken)).thenReturn("test@example.com");
        when(userRepository.findByUsername("test@example.com")).thenReturn(Optional.of(employee));
        when(jwtUtil.isTokenValid(refreshToken, "test@example.com")).thenReturn(true);
        when(jwtUtil.generateToken(employee)).thenReturn("new-access-token");

        Map<String, String> response = authController.refreshToken(Map.of("refreshToken", refreshToken));

        assertEquals("new-access-token", response.get("accessToken"));
        assertEquals(refreshToken, response.get("refreshToken"));
    }

    @Test
    void refreshTokenRejectsMissingToken() {
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> authController.refreshToken(Map.of())
        );

        assertEquals(BAD_REQUEST, ex.getStatusCode());
        assertEquals("Refresh token is required", ex.getReason());
    }

    @Test
    void refreshTokenRejectsMalformedToken() {
        String malformedToken = "bad-token";
        when(jwtUtil.extractUsername(malformedToken)).thenThrow(new IllegalArgumentException("bad token"));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> authController.refreshToken(Map.of("refreshToken", malformedToken))
        );

        assertEquals(UNAUTHORIZED, ex.getStatusCode());
        assertEquals("Invalid Refresh Token", ex.getReason());
    }
}
