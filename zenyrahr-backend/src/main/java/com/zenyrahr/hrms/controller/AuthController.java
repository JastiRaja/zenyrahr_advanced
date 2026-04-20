package com.zenyrahr.hrms.controller;
import com.zenyrahr.hrms.Repository.OrganizationRepository;
import com.zenyrahr.hrms.Repository.UserRepository;
import com.zenyrahr.hrms.dto.BootstrapMainAdminRequest;
import com.zenyrahr.hrms.model.Employee;
import com.zenyrahr.hrms.model.User;
import com.zenyrahr.hrms.service.TenantAccessService;
import com.zenyrahr.hrms.service.UserDetailsService;
import com.zenyrahr.hrms.service.EmployeeLeaveBalanceService;
import com.zenyrahr.hrms.service.EmployeeCodeService;
import com.zenyrahr.hrms.service.OrganizationRoleService;
import com.zenyrahr.hrms.service.OrganizationService;
import com.zenyrahr.hrms.service.SequenceService;
import com.zenyrahr.hrms.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import sibApi.TransactionalEmailsApi;
import sibModel.*;
import sendinblue.ApiClient;
import sendinblue.Configuration;
import sendinblue.auth.ApiKeyAuth;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;

    private final PasswordEncoder passwordEncoder;

    private final UserDetailsService userDetailsService;

    @Value("${brevo.api.key}")
    private String brevoApiKey;

    @Value("${brevo.sender.email}")
    private String brevoSenderEmail;

    @Value("${brevo.sender.name}")
    private String brevoSenderName;

    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final TenantAccessService tenantAccessService;
    private final EmployeeLeaveBalanceService employeeLeaveBalanceService;
    private final EmployeeCodeService employeeCodeService;
    private final OrganizationRoleService organizationRoleService;
    private final OrganizationService organizationService;
    private final SequenceService sequenceService;

    @PostMapping("/bootstrap-main-admin")
    public ResponseEntity<Map<String, String>> bootstrapMainAdmin(@RequestBody BootstrapMainAdminRequest request) {
        if (userRepository.existsByRoleIgnoreCase(TenantAccessService.MAIN_PLATFORM_ADMIN_ROLE)
                || userRepository.existsByRoleIgnoreCase("admin")) {
            throw new ResponseStatusException(BAD_REQUEST, "Main admin already exists");
        }
        if (request.getFirstName() == null || request.getFirstName().isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "First name is required");
        }
        if (request.getLastName() == null || request.getLastName().isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "Last name is required");
        }
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "Email is required");
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "Password is required");
        }
        if (request.getWorkLocation() == null || request.getWorkLocation().isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "Work location is required");
        }
        String normalizedEmail = request.getUsername().trim().toLowerCase();
        if (userRepository.findByUsername(normalizedEmail).isPresent()) {
            throw new ResponseStatusException(BAD_REQUEST, "User already exists with this email");
        }

        Employee admin = new Employee();
        admin.setFirstName(request.getFirstName().trim());
        admin.setLastName(request.getLastName().trim());
        admin.setUsername(normalizedEmail);
        admin.setPassword(passwordEncoder.encode(request.getPassword()));
        admin.setRole(TenantAccessService.MAIN_PLATFORM_ADMIN_ROLE);
        admin.setWorkLocation(request.getWorkLocation().trim());
        admin.setFirstLogin(true);
        admin.setLocked(false);
        admin.setOrganization(null);
        admin.setCode(sequenceService.getNextCode("USER"));

        userRepository.save(admin);
        return ResponseEntity.ok(Map.of("message", "Main admin bootstrapped successfully"));
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> registerUser(@RequestBody Employee employee) throws Exception {
        if (employee.getUsername() == null || employee.getUsername().isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "Email is required");
        }
        if (userRepository.findByUsername(employee.getUsername()).isPresent()) {
            throw new ResponseStatusException(BAD_REQUEST, "User already exists with this email");
        }
        Employee actor = tenantAccessService.requireCurrentEmployee();
        tenantAccessService.assertOrganizationActive(actor);
        tenantAccessService.assertCanManageEmployees(actor);

        Long scopedOrgId = tenantAccessService.scopedOrganizationForNewEmployee(actor, employee)
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "Organization is required"));
        employee.setOrganization(
                organizationRepository.findById(scopedOrgId)
                        .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Organization not found"))
        );
        if (!tenantAccessService.isMainAdmin(actor)
                && !Boolean.TRUE.equals(employee.getOrganization().getActive())) {
            throw new ResponseStatusException(FORBIDDEN, "Cannot create users in disabled organization");
        }
        String requestedRole = organizationRoleService.normalizeAssignableRole(employee.getRole());
        tenantAccessService.assertOrgAdminDoesNotCreatePrivilegedUsers(actor, employee);
        organizationRoleService.assertRoleExistsForOrganization(scopedOrgId, requestedRole);
        organizationService.assertCanAddActiveUsers(scopedOrgId, 1);
        employee.setRole(requestedRole);
        String generatedPassword = generateRandomPassword();
        employee.setPassword(passwordEncoder.encode(generatedPassword));
        employee.setFirstLogin(true);
        String requestedCode = employee.getCode();
        employee.setCode(employeeCodeService.resolveCodeForNewEmployee(actor, employee.getOrganization(), requestedCode));
        Employee savedEmployee = userRepository.save(employee);
        employeeLeaveBalanceService.initializeLeaveBalancesForEmployee(savedEmployee);
        try {
            sendWelcomeEmail(employee.getFirstName() + " " + employee.getLastName(), generatedPassword, employee.getUsername());
        } catch (Exception emailError) {
            log.error("User created but welcome email failed for {}", employee.getUsername(), emailError);
            return ResponseEntity.ok(Map.of(
                    "message", "User registered successfully, but welcome email could not be sent"
            ));
        }
        return ResponseEntity.ok(Map.of("message", "User registered successfully"));    }
    private String generateRandomPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        Random random = new Random();
        StringBuilder password = new StringBuilder();
        for (int i = 0; i < 12; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }
        return password.toString();
    }
    private String buildEmailContent(String employeeName, String username, String password) {
        return "Dear " + employeeName + ",\n\n" + "We are pleased to inform you that your account has been successfully " + "created in ZenyraHR. Please find your login credentials " + "below:\n\n" + "Username: " + username + "\n" + "Password: " + password + "\n\n" + "You can log in to the ZenyraHR portal using the following link: " + "localhost:5173/login" + "\n\n" + "Please change your password upon your first login for security purposes. " + "If you encounter any issues, feel free to reach out to the HR support team at " + "hr@hrms.com" + ".\n\n" + "Best regards,\n" + "ZenyraHR\n";    }
    private void sendWelcomeEmail(String username, String password, String email) throws Exception {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        ApiKeyAuth apiKey = (ApiKeyAuth) defaultClient.getAuthentication("api-key");
        apiKey.setApiKey(brevoApiKey);
        TransactionalEmailsApi apiInstance = new TransactionalEmailsApi();
        SendSmtpEmail sendSmtpEmail = new SendSmtpEmail();
        sendSmtpEmail.setSender(new SendSmtpEmailSender().email(brevoSenderEmail).name(brevoSenderName));
        sendSmtpEmail.setTo(java.util.Arrays.asList(new SendSmtpEmailTo().email(email).name(username)));
        sendSmtpEmail.setSubject("Welcome to ZenyraHR");
        sendSmtpEmail.setHtmlContent("<html><body>" + buildEmailContent(username, email, password).replace("\n", "<br>") + "</body></html>");
        try {
            apiInstance.sendTransacEmail(sendSmtpEmail);
            log.info("Welcome email sent successfully to {}", email);
        } catch (sendinblue.ApiException ex) {
            log.error("Brevo API Error - Status: {}, Code: {}, Message: {}", 
                ex.getCode(), 
                ex.getResponseBody(), 
                ex.getMessage());
            throw new Exception("Error sending email via Brevo: " + ex.getResponseBody(), ex);
        } catch (Exception ex) {
            log.error("Unexpected error sending welcome email to {}: {}", email, ex.getMessage(), ex);
            throw new Exception("Error sending email via Brevo: " + ex.getMessage(), ex);
        }
    }
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> loginUser(@RequestBody User user) {
        try {
            // Check if the username exists in the database
            boolean usernameExists = userRepository.findByUsername(user.getUsername()).isPresent();
            if (!usernameExists) {
                return ResponseEntity.status(404).body(Map.of(
                        "usernameExists", false,
                        "ErrorMessage", "User not found."
                ));
            }

            // Authenticate the user
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(user.getUsername(), user.getPassword())
            );

            // Fetch user details from the database
            Employee authenticatedUser = userRepository.findByUsername(user.getUsername())
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));
            tenantAccessService.assertOrganizationActive(authenticatedUser);

            // Check if it's the first login
            boolean redirectToResetPassword = authenticatedUser.isFirstLogin();

            // Generate JWT tokens
            String accessToken = jwtUtil.generateToken(authenticatedUser);
            String refreshToken = jwtUtil.generateRefreshToken(authenticatedUser);

            // Prepare the response map
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("id", authenticatedUser.getId().toString());
            response.put("username", authenticatedUser.getUsername().toString());
            response.put("name", authenticatedUser.getName().toString());
            response.put("firstName", authenticatedUser.getFirstName().toString());
            response.put("role", authenticatedUser.getRole());
            response.put("organizationId", authenticatedUser.getOrganization() != null
                    ? authenticatedUser.getOrganization().getId()
                    : null);
            response.put("usernameExists", true);
            response.put("accessToken", accessToken);
            response.put("refreshToken", refreshToken);
            response.put("redirectToResetPassword", redirectToResetPassword);

            return ResponseEntity.ok(response);
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(401).body(Map.of(
                    "usernameExists", true,
                    "ErrorMessage", "Authentication failed. Invalid credentials."
            ));
        } catch (DisabledException | LockedException e) {
            return ResponseEntity.status(403).body(Map.of(
                    "usernameExists", true,
                    "ErrorMessage", "Your account is deactivated. Please contact HR."
            ));
        } catch (UsernameNotFoundException e) {
            return ResponseEntity.status(404).body(Map.of(
                    "usernameExists", false,
                    "ErrorMessage", "User not found."
            ));
        } catch (ResponseStatusException e) {
            String reason = e.getReason() == null ? "Access denied." : e.getReason();
            String message = "ORGANIZATION_DISABLED".equalsIgnoreCase(reason)
                    ? "Your organization is currently disabled. Please contact your main admin."
                    : reason;
            return ResponseEntity.status(e.getStatusCode()).body(Map.of(
                    "ErrorMessage", message
            ));
        } catch (Exception e) {
            log.error("Unexpected login error for user {}", user.getUsername(), e);
            return ResponseEntity.status(500).body(Map.of(
                    "ErrorMessage", "An unexpected error occurred."
            ));
        }
    }

    @PostMapping("/resetPassword")
    public ResponseEntity<Map<String, Object>> resetPassword(@RequestBody User user) {
        try {
            // Validate input
            if (user.getUsername() == null || user.getPassword() == null) {
                return ResponseEntity.status(400).body(Map.of(
                    "ErrorMessage",
                    "Username and password are required."
                ));
            }

            // Fetch the user by username
            Employee existingUser = userRepository.findByUsername(user.getUsername())
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            // Update the password and firstLogin status
            existingUser.setPassword(passwordEncoder.encode(user.getPassword()));
            existingUser.setFirstLogin(false);
            
            // Save the updated user
            userRepository.save(existingUser);
            
            // Return success message
            return ResponseEntity.ok(Map.of(
                "message",
                "Password reset successfully"));
        } catch (UsernameNotFoundException e) {
            // Handle user not found
            return ResponseEntity.status(404).body(Map.of(
                "ErrorMessage",
                "User not found."
            ));
        } catch (Exception e) {
            // Handle unexpected errors
            log.error("Error resetting password: ", e);
            return ResponseEntity.status(500).body(Map.of(
                "ErrorMessage",
                "An unexpected error occurred while resetting password."
            ));
        }
    }
    @PostMapping("/refresh")
    public Map<String, String> refreshToken(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "Refresh token is required");
        }

        try {
            String username = jwtUtil.extractUsername(refreshToken);
            Employee userFromDB = userRepository.findByUsername(username)
                    .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "User not found"));
            tenantAccessService.assertOrganizationActive(userFromDB);
            if (jwtUtil.isTokenValid(refreshToken, userFromDB.getUsername())) {
                String newAccessToken = jwtUtil.generateToken(userFromDB);
                return Map.of("accessToken", newAccessToken, "refreshToken", refreshToken);
            }
            throw new ResponseStatusException(UNAUTHORIZED, "Invalid Refresh Token");
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new ResponseStatusException(UNAUTHORIZED, "Invalid Refresh Token");
        }
    }

    @PostMapping("/refresh-token")
    public Map<String, String> refreshTokenAlias(@RequestBody Map<String, String> request) {
        return refreshToken(request);
    }
    @PostMapping("/forgotPassword")
    public ResponseEntity<Map<String, String>> forgetPassword(@RequestBody Map<String, String> request) throws Exception {
        // Extract username (email) from the request
        String username = request.get("username");

        // Find the user by username (email)
        Employee employee = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // Generate a random PIN
        int generatedPin = generateRandomPin();

        // Send the PIN to the user's email
        sendPinEmail(employee.getFirstName() + " " + employee.getLastName(), generatedPin, employee.getUsername());

        // Optionally, store the PIN in the database (or another field if not using the password field)
        employee.setTemporaryCode(String.valueOf(generatedPin)); // Assuming `temporaryCode` is a field in your `User` class
        userRepository.save(employee);

        // Respond with success message
        return ResponseEntity.ok(Map.of("message", "A PIN has been sent to your email for password reset"));
    }

    private int generateRandomPin() {
        String digits = "123456789";
        Random random = new Random();
        StringBuilder pinBuilder = new StringBuilder();

        for (int i = 0; i < 4; i++) {
            pinBuilder.append(digits.charAt(random.nextInt(digits.length())));
        }

        return Integer.parseInt(pinBuilder.toString());
    }

    private String buildPinEmailContent(String employeeName, int pin) {
        return "Dear " + employeeName + ",\n\n" +
                "We are pleased to inform you that your account setup process requires a verification step. " +
                "Please use the following PIN to complete the verification:\n\n" +
                "PIN: " + pin + "\n\n" +
                "You can use this PIN on the ZenyraHR portal to verify your account or perform other secure actions. " +
                "Please do not share this PIN with anyone to ensure the security of your account.\n\n" +
                "If you encounter any issues, feel free to reach out to the HR support team at " +
                "hr@hrms.com" + ".\n\n";
    }


    private void sendPinEmail(String employeeName, int pin, String email) throws Exception {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        ApiKeyAuth apiKey = (ApiKeyAuth) defaultClient.getAuthentication("api-key");
        apiKey.setApiKey(brevoApiKey);
        TransactionalEmailsApi apiInstance = new TransactionalEmailsApi();
        SendSmtpEmail sendSmtpEmail = new SendSmtpEmail();
        sendSmtpEmail.setSender(new SendSmtpEmailSender().email(brevoSenderEmail).name(brevoSenderName));
        sendSmtpEmail.setTo(java.util.Arrays.asList(new SendSmtpEmailTo().email(email).name(employeeName)));
        sendSmtpEmail.setSubject("Your ZenyraHR account verification PIN");
        sendSmtpEmail.setHtmlContent("<html><body>" + buildPinEmailContent(employeeName, pin).replace("\n", "<br>") + "</body></html>");
        try {
            apiInstance.sendTransacEmail(sendSmtpEmail);
        } catch (Exception ex) {
            throw new Exception("Error sending email via Brevo", ex);
        }
    }
    @GetMapping("/usersdata")
    public ResponseEntity<List<Employee>> getAllUsersData(@RequestParam String role) {
        // Keep request param for backward compatibility, but enforce authorization from authenticated principal.
        Employee actor = tenantAccessService.requireCurrentEmployee();
        if (tenantAccessService.isMainAdmin(actor)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }
        if (!tenantAccessService.canManageEmployees(actor)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }
        List<Employee> employees = userDetailsService.getAllUsers();
        employees = tenantAccessService.filterEmployeesByScope(actor, employees);
        if (employees.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(employees);
    }


    @GetMapping("/employeebyid")
    public ResponseEntity<Employee> getUserById(@RequestParam Long id) {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        if (tenantAccessService.isMainAdmin(actor)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }
        // Fetch user details by ID
        Optional<Employee> userOptional = userDetailsService.getEmployeeById(id);
//
        // Return user details if found
        // 404 Not Found if user doesn't exist
        return userOptional
                .map(user -> {
                    tenantAccessService.assertCanAccessEmployee(actor, user);
                    return ResponseEntity.ok(user);
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
    }

    @PostMapping("/verifyPinAndResetPassword")
    public ResponseEntity<Map<String, String>> verifyPinAndResetPassword(@RequestBody Map<String, String> request) throws Exception {
        // Extract username, PIN, and new password from the request
        String username = request.get("username");
        String providedPin = request.get("pin");
        String newPassword = request.get("newPassword");
        // Find the user by username (email)
        Employee employee = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        // Verify if the provided PIN matches the stored temporary code
        if (providedPin.equals(employee.getTemporaryCode())) {
            // PIN is correct, now reset the password
            employee.setPassword(passwordEncoder.encode(newPassword));
            // Encrypt the new password
            employee.setTemporaryCode(null);
            // Clear the temporary PIN after use
            userRepository.save(employee);
            // Respond with success message
            return ResponseEntity.ok(Map.of(
                    "message",
                    "Password has been successfully reset"
            ));
        } else {
            // If the PIN is incorrect, return an error message
            return ResponseEntity.status(400).body(Map.of(
                    "ErrorMessage",
                    "Invalid PIN. Please try again."));
        }}
}