package com.zenyrahr.hrms.config;

import com.zenyrahr.hrms.Repository.UserRepository;
import com.zenyrahr.hrms.model.Employee;
import com.zenyrahr.hrms.service.TenantAccessService;
import com.zenyrahr.hrms.service.UserDetailsService;
import com.zenyrahr.hrms.utils.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;
    private final UserRepository userRepository;
    private final TenantAccessService tenantAccessService;

    public JwtFilter(
            JwtUtil jwtUtil,
            UserDetailsService userDetailsService,
            UserRepository userRepository,
            TenantAccessService tenantAccessService
    ) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
        this.userRepository = userRepository;
        this.tenantAccessService = tenantAccessService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String authorizationHeader = request.getHeader("Authorization");
            if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                String token = authorizationHeader.substring(7);
                processTokenAuthentication(token, request);
            }
        } catch (Exception e) {
            int status = "ORGANIZATION_DISABLED".equalsIgnoreCase(e.getMessage())
                    ? HttpServletResponse.SC_FORBIDDEN
                    : HttpServletResponse.SC_UNAUTHORIZED;
            response.setStatus(status);
            response.getWriter().write(e.getMessage() != null ? e.getMessage() : "Unauthorized");
            return; // Stop further processing
        }
        filterChain.doFilter(request, response);
    }


    /**
     * Processes the token for authentication.
     *
     * @param token   the JWT token
     * @param request the HTTP servlet request
     */
    private void processTokenAuthentication(String token, HttpServletRequest request) {
        String username = jwtUtil.extractUsername(token);

        if (username != null) {
            Employee employee = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
            tenantAccessService.assertOrganizationActive(employee);

            Long tokenOrgId = jwtUtil.extractOrganizationId(token);
            Long employeeOrgId = employee.getOrganization() != null ? employee.getOrganization().getId() : null;
            if (!tenantAccessService.isMainAdmin(employee)
                    && tokenOrgId != null
                    && employeeOrgId != null
                    && !tokenOrgId.equals(employeeOrgId)) {
                throw new RuntimeException("Token organization mismatch");
            }

            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            if (!userDetails.isEnabled() || !userDetails.isAccountNonLocked()) {
                throw new RuntimeException("Account is deactivated");
            }

            if (jwtUtil.isTokenValid(token, userDetails.getUsername())) {
                setAuthentication(userDetails, request);
            } else {
                throw new RuntimeException("Invalid token");
            }
        }
    }


    /**
     * Sets the authentication in the SecurityContext.
     *
     * @param userDetails the user details
     * @param request     the HTTP servlet request
     */
    private void setAuthentication(UserDetails userDetails, HttpServletRequest request) {
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authToken);
    }

}
