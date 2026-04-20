package com.zenyrahr.hrms.controller;

import com.zenyrahr.hrms.Repository.EmployeeRepository;
import com.zenyrahr.hrms.model.Employee;
import com.zenyrahr.hrms.service.TenantAccessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import static org.springframework.http.HttpStatus.FORBIDDEN;

@RestController
@RequestMapping("/api/hr/analytics")
@CrossOrigin(origins = "*")
public class HrAnalyticsController {
    @Autowired
    private EmployeeRepository employeeRepository;
    @Autowired
    private TenantAccessService tenantAccessService;

    private void requireMainAdmin() {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        if (!tenantAccessService.isMainAdmin(actor)) {
            throw new ResponseStatusException(FORBIDDEN, "Only main admin can access analytics");
        }
    }

    @GetMapping("/new-joinings")
    public Map<String, Object> getNewJoinings() {
        requireMainAdmin();
        YearMonth currentMonth = YearMonth.now();
        LocalDate startOfMonth = currentMonth.atDay(1);
        LocalDate endOfMonth = currentMonth.atEndOfMonth();
        int thisMonth = employeeRepository.countByJoinDateBetween(startOfMonth, endOfMonth);
        List<Object[]> trend = employeeRepository.countJoiningsPerMonth();
        return Map.of(
            "thisMonth", thisMonth,
            "trend", trend.stream().map(obj -> Map.of("month", obj[0], "count", obj[1])).collect(Collectors.toList())
        );
    }

    @GetMapping("/employees-by-department")
    public List<Map<String, Object>> getEmployeesByDepartment() {
        requireMainAdmin();
        List<Object[]> data = employeeRepository.countByDepartment();
        return data.stream()
            .map(obj -> Map.of("department", obj[0], "count", obj[1]))
            .collect(Collectors.toList());
    }

    @GetMapping("/gender-diversity")
    public Map<String, Long> getGenderDiversity() {
        requireMainAdmin();
        long male = employeeRepository.countByGender("Male");
        long female = employeeRepository.countByGender("Female");
        long other = employeeRepository.countByGender("Other");
        Map<String, Long> result = new HashMap<>();
        result.put("Male", male);
        result.put("Female", female);
        result.put("Other", other);
        return result;
    }
} 