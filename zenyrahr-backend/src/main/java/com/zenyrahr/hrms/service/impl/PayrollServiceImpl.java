package com.zenyrahr.hrms.service.impl;

import com.zenyrahr.hrms.Repository.AttendanceRepository;
import com.zenyrahr.hrms.Repository.EmployeeRepository;
import com.zenyrahr.hrms.Repository.OrganizationRepository;
import com.zenyrahr.hrms.Repository.PayscaleRepository;
import com.zenyrahr.hrms.Repository.PayrollGenerationRepository;
import com.zenyrahr.hrms.Repository.PayrollRepository;
import com.zenyrahr.hrms.model.Attendance;
import com.zenyrahr.hrms.model.Employee;
import com.zenyrahr.hrms.model.Payscale;
import com.zenyrahr.hrms.model.Payroll;
import com.zenyrahr.hrms.model.PayrollGeneration;
import com.zenyrahr.hrms.service.PayrollService;
import com.zenyrahr.hrms.service.TenantAccessService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class PayrollServiceImpl implements PayrollService {

    private final PayrollRepository payrollRepository;
    private final EmployeeRepository employeeRepository;
    private final PayscaleRepository payscaleRepository;
    private final PayrollGenerationRepository payrollGenerationRepository;
    private final AttendanceRepository attendanceRepository;
    private final OrganizationRepository organizationRepository;
    private final TenantAccessService tenantAccessService;

    public PayrollServiceImpl(PayrollRepository payrollRepository,
                              EmployeeRepository employeeRepository,
                              PayscaleRepository payscaleRepository,
                              PayrollGenerationRepository payrollGenerationRepository,
                              AttendanceRepository attendanceRepository,
                              OrganizationRepository organizationRepository,
                              TenantAccessService tenantAccessService) {
        this.payrollRepository = payrollRepository;
        this.employeeRepository = employeeRepository;
        this.payscaleRepository = payscaleRepository;
        this.payrollGenerationRepository = payrollGenerationRepository;
        this.attendanceRepository = attendanceRepository;
        this.organizationRepository = organizationRepository;
        this.tenantAccessService = tenantAccessService;
    }

    @Override
    public Payroll generatePayroll(Long employeeId, Payroll payrollData) {
        Employee emp = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));
        Payroll payroll = new Payroll();
        payroll.setOrganizationId(organizationIdFor(emp));
        payroll.setEmployeeId(employeeId);
        payroll.setPayrollDate(payrollData.getPayrollDate());
        payroll.setGrossPay      (payrollData.getGrossPay());
        payroll.setNetPay        (payrollData.getNetPay());
        payroll.setNetPayInWords (payrollData.getNetPayInWords());
        payroll.setTotalEarnings (payrollData.getTotalEarnings());
        payroll.setStatus        ("Pending");
        payroll.setBasicPay             (payrollData.getBasicPay());
        payroll.setHouseRentAllowance   (payrollData.getHouseRentAllowance());
        payroll.setDearnessAllowance    (payrollData.getDearnessAllowance());
        payroll.setConveyanceAllowance  (payrollData.getConveyanceAllowance());
        payroll.setMedicalAllowance     (payrollData.getMedicalAllowance());
        payroll.setOtherAllowances      (payrollData.getOtherAllowances());
        payroll.setTotalDeductions      (payrollData.getTotalDeductions());
        payroll.setEpfAmount            (payrollData.getEpfAmount());
        payroll.setProfessionalTax      (payrollData.getProfessionalTax());
        payroll.setHealthInsuranceDeduction(payrollData.getHealthInsuranceDeduction());
        return payrollRepository.save(payroll);
    }

    @Override
    public List<Payroll> getPayrollsByEmployeeId(Long employeeId) {
        return payrollRepository.findByEmployeeId(employeeId);
    }

    @Override
    public Payroll generatePayrollForEmployee(Long employeeId, YearMonth month) {
        Employee emp = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));
        String monthYear = month.getYear() + "-" + String.format("%02d", month.getMonthValue());
        Optional<Payroll> existing = payrollRepository.findByEmployeeId(emp.getId())
                .stream()
                .filter(p -> monthYear.equals(p.getPayrollMonthYear()))
                .findFirst();
        if (existing.isPresent()) {
            return existing.get();
        }

        int totalWorkingDays = getWorkingDaysInMonth(month);
        Payscale payscale = payscaleRepository.findByEmployeeId(emp.getId())
                .stream()
                .filter(ps ->
                        (ps.getEffectiveFrom() == null || !month.atEndOfMonth().isBefore(ps.getEffectiveFrom())) &&
                        (ps.getEffectiveTo() == null || !month.atEndOfMonth().isAfter(ps.getEffectiveTo()))
                )
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No active payscale found for employee in selected month"));

        LocalDate start = month.atDay(1), end = month.atEndOfMonth();
        LocalDate joiningDate = emp.getJoinDate();
        LocalDate effectiveStart = joiningDate != null && joiningDate.isAfter(start) ? joiningDate : start;
        List<Attendance> atts = attendanceRepository.findByEmployeeIdAndDateBetween(emp.getId(), effectiveStart, end);
        long absentDays = atts.stream().filter(a -> "ABSENT".equalsIgnoreCase(a.getStatus())).count();
        long paidDays = totalWorkingDays - absentDays;
        double payableDays = calculatePayableDays(atts);
        long presentDays = atts.stream().filter(a -> "PRESENT".equalsIgnoreCase(a.getStatus())).count();
        long halfDays = atts.stream().filter(a -> "HALF_DAY".equalsIgnoreCase(a.getStatus())).count();
        int totalDaysInMonth = end.getDayOfMonth();
        int daysFromJoining = totalDaysInMonth - (effectiveStart.getDayOfMonth() - 1);
        double proRata = Math.min(
                (double) payableDays / totalWorkingDays,
                (double) daysFromJoining / totalDaysInMonth
        );

        double monthlyCtc = safe(payscale.getCtc());
        double monthlyPf = safe(payscale.getPfContribution());
        double monthlyHealthIns = safe(payscale.getHealthInsurance());
        double monthlyProfTax = safe(payscale.getProfessionalTax());
        double proratedCtc = monthlyCtc * proRata;
        double proratedPf = monthlyPf * proRata;
        double proratedHealthIns = monthlyHealthIns * proRata;
        double proratedProfTax = monthlyProfTax * proRata;
        double net = proratedCtc - (proratedPf + proratedHealthIns + proratedProfTax);

        Payroll payroll = new Payroll();
        payroll.setOrganizationId(organizationIdFor(emp));
        payroll.setEmployeeId(emp.getId());
        payroll.setPayrollDate(end);
        payroll.setPayrollMonthYear(monthYear);
        payroll.setStatus("PENDING");
        payroll.setGrossPay(String.format("%.2f", proratedCtc));
        payroll.setBasicPay(String.format("%.2f", safe(payscale.getBasicSalary()) * proRata));
        payroll.setHouseRentAllowance(String.format("%.2f", safe(payscale.getHra()) * proRata));
        payroll.setDearnessAllowance(String.format("%.2f", safe(payscale.getDa()) * proRata));
        payroll.setConveyanceAllowance("0.00");
        payroll.setMedicalAllowance(String.format("%.2f", safe(payscale.getMedicalAllowance()) * proRata));
        payroll.setOtherAllowances(String.format("%.2f", safe(payscale.getAllowance()) * proRata));
        payroll.setTotalEarnings(String.format("%.2f", proratedCtc));
        payroll.setEpfAmount(String.format("%.2f", proratedPf));
        payroll.setProfessionalTax(String.format("%.2f", proratedProfTax));
        payroll.setHealthInsuranceDeduction(String.format("%.2f", proratedHealthIns));
        payroll.setTotalDeductions(String.format("%.2f", proratedPf + proratedHealthIns + proratedProfTax));
        payroll.setNetPay(String.format("%.2f", net));
        payroll.setNetPayInWords(convertNumberToWords((int) net));
        payroll.setWorkingDays(String.format("%.1f", payableDays));
        payroll.setTotalWorkingDays(String.valueOf(totalWorkingDays));
        payroll.setPresentDays(String.valueOf(presentDays));
        payroll.setAbsentDays(String.valueOf(absentDays));
        payroll.setHalfDays(String.valueOf(halfDays));
        payroll.setPaidDays(String.valueOf(paidDays));
        return payrollRepository.save(payroll);
    }

    @Override
    public List<Payroll> generatePayrollForAllEmployees(YearMonth month) {
        Employee actor = tenantAccessService.requireCurrentEmployee();
        List<Employee> employees = tenantAccessService
                .filterEmployeesByScope(actor, employeeRepository.findAll())
                .stream()
                .filter(this::isPayrollEligibleEmployee)
                .toList();
        List<Payroll> created = new ArrayList<>();
        int totalWorkingDays = getWorkingDaysInMonth(month);
        String monthYear = month.getYear() + "-" + String.format("%02d", month.getMonthValue());

        List<String> missingPayscaleEmployees = new ArrayList<>();
        for (Employee emp : employees) {
            boolean alreadyExists = payrollRepository.findByEmployeeId(emp.getId())
                    .stream()
                    .anyMatch(p -> monthYear.equals(p.getPayrollMonthYear()));
            if (alreadyExists) {
                continue;
            }

            boolean hasActivePayscale = payscaleRepository.findByEmployeeId(emp.getId())
                    .stream()
                    .anyMatch(ps ->
                            (ps.getEffectiveFrom() == null || !month.atEndOfMonth().isBefore(ps.getEffectiveFrom())) &&
                            (ps.getEffectiveTo() == null || !month.atEndOfMonth().isAfter(ps.getEffectiveTo()))
                    );

            if (!hasActivePayscale) {
                String fullName = ((emp.getFirstName() == null ? "" : emp.getFirstName()) + " "
                        + (emp.getLastName() == null ? "" : emp.getLastName())).trim();
                missingPayscaleEmployees.add(
                        (fullName.isBlank() ? "Employee ID " + emp.getId() : fullName) + " (ID: " + emp.getId() + ")"
                );
            }
        }

        if (!missingPayscaleEmployees.isEmpty()) {
            throw new IllegalStateException(
                    "Payscale is not added for: " + String.join(", ", missingPayscaleEmployees)
            );
        }

        for (Employee emp : employees) {
            // Check if payroll already exists
            boolean alreadyExists = payrollRepository.findByEmployeeId(emp.getId())
                    .stream()
                    .anyMatch(p -> monthYear.equals(p.getPayrollMonthYear()));
            if (alreadyExists) continue;

            // Get active payscale for the month (all values in payscale are monthly)
            Payscale payscale = payscaleRepository.findByEmployeeId(emp.getId())
                    .stream()
                    .filter(ps ->
                            (ps.getEffectiveFrom() == null || !month.atEndOfMonth().isBefore(ps.getEffectiveFrom())) &&
                            (ps.getEffectiveTo() == null || !month.atEndOfMonth().isAfter(ps.getEffectiveTo()))
                    )
                    .findFirst()
                    .orElse(null);

            if (payscale == null) {
                continue;
            }

            // Calculate proration based on joining date and working days
            LocalDate start = month.atDay(1), end = month.atEndOfMonth();
            LocalDate joiningDate = emp.getJoinDate();
            LocalDate effectiveStart = joiningDate != null && joiningDate.isAfter(start) ? joiningDate : start;
            
            // Get attendance records
            List<Attendance> atts = attendanceRepository.findByEmployeeIdAndDateBetween(emp.getId(), effectiveStart, end);
            long absentDays = atts.stream().filter(a -> "ABSENT".equalsIgnoreCase(a.getStatus())).count();
            long paidDays = totalWorkingDays - absentDays;
            double payableDays = calculatePayableDays(atts);
            long presentDays = atts.stream().filter(a -> "PRESENT".equalsIgnoreCase(a.getStatus())).count();
            long halfDays = atts.stream().filter(a -> "HALF_DAY".equalsIgnoreCase(a.getStatus())).count();
            
            // Calculate proration factor considering joining date
            // This will be used to prorate the monthly salary components
            int totalDaysInMonth = end.getDayOfMonth();
            int daysFromJoining = totalDaysInMonth - (effectiveStart.getDayOfMonth() - 1);
            double proRata = Math.min(
                (double) payableDays / totalWorkingDays,
                (double) daysFromJoining / totalDaysInMonth
            );

            // Get monthly salary components from payscale
            double monthlyCtc = safe(payscale.getCtc());
            double monthlyPf = safe(payscale.getPfContribution());
            double monthlyHealthIns = safe(payscale.getHealthInsurance());
            double monthlyProfTax = safe(payscale.getProfessionalTax());

            // Pro-rate all values
            double proratedCtc = monthlyCtc * proRata;
            double proratedPf = monthlyPf * proRata;
            double proratedHealthIns = monthlyHealthIns * proRata;
            double proratedProfTax = monthlyProfTax * proRata;

            // Net Salary = Prorated CTC - (Prorated PF + Prorated Health Insurance + Prorated Professional Tax)
            double net = proratedCtc - (proratedPf + proratedHealthIns + proratedProfTax);

            // Build Payroll record with pro-rated values
            Payroll p = new Payroll();
            p.setOrganizationId(organizationIdFor(emp));
            p.setEmployeeId(emp.getId());
            p.setPayrollDate(end);
            p.setPayrollMonthYear(monthYear);
            p.setStatus("PENDING");
            p.setGrossPay(String.format("%.2f", proratedCtc));
            p.setBasicPay(String.format("%.2f", safe(payscale.getBasicSalary()) * proRata));
            p.setHouseRentAllowance(String.format("%.2f", safe(payscale.getHra()) * proRata));
            p.setDearnessAllowance(String.format("%.2f", safe(payscale.getDa()) * proRata));
            p.setConveyanceAllowance("0.00"); // No longer used
            p.setMedicalAllowance(String.format("%.2f", safe(payscale.getMedicalAllowance()) * proRata));
            p.setOtherAllowances(String.format("%.2f", safe(payscale.getAllowance()) * proRata));
            p.setTotalEarnings(String.format("%.2f", proratedCtc));
            p.setEpfAmount(String.format("%.2f", proratedPf));
            p.setProfessionalTax(String.format("%.2f", proratedProfTax));
            p.setHealthInsuranceDeduction(String.format("%.2f", proratedHealthIns));
            p.setTotalDeductions(String.format("%.2f", proratedPf + proratedHealthIns + proratedProfTax));
            p.setNetPay(String.format("%.2f", net));
            p.setNetPayInWords(convertNumberToWords((int) net));
            p.setWorkingDays(String.format("%.1f", payableDays));
            p.setTotalWorkingDays(String.valueOf(totalWorkingDays));
            p.setPresentDays(String.valueOf(presentDays));
            p.setAbsentDays(String.valueOf(absentDays));
            p.setHalfDays(String.valueOf(halfDays));
            p.setPaidDays(String.valueOf(paidDays));

            created.add(payrollRepository.save(p));
        }

        return created;
    }

    private boolean isPayrollEligibleEmployee(Employee employee) {
        if (employee == null) {
            return false;
        }
        if (!Boolean.TRUE.equals(employee.getActive()) || Boolean.TRUE.equals(employee.getDeleted())) {
            return false;
        }
        String role = employee.getRole() == null ? "" : employee.getRole().toLowerCase(Locale.ROOT);
        // Main admin is a platform account and should never block org payroll generation.
        return !"zenyrahr_admin".equals(role);
    }

    @Override
    @Transactional
    public PayrollGeneration generatePayroll(Long organizationId, String monthYear, String generatedBy) {
        if (!organizationRepository.existsById(organizationId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Organization not found");
        }
        if (payrollGenerationRepository.findFirstByOrganizationIdAndMonthYear(organizationId, monthYear).isPresent()) {
            throw new RuntimeException("Payroll already generated for this organization and month: " + monthYear);
        }
        PayrollGeneration pg = new PayrollGeneration();
        pg.setOrganizationId(organizationId);
        pg.setMonthYear(monthYear);
        pg.setGenerationDate(LocalDate.now());
        pg.setStatus("DRAFT");
        pg.setGeneratedBy(generatedBy);
        pg.setTotalEmployees(0);
        pg.setTotalPayrollAmount(0.0);
        pg.setTotalDeductions(0.0);
        pg.setTotalNetAmount(0.0);
        return payrollGenerationRepository.save(pg);
    }

    @Override
    @Transactional
    public PayrollGeneration approvePayroll(Long id, String approvedBy) {
        PayrollGeneration pg = getPayrollGenerationById(id);
        if (!"DRAFT".equals(pg.getStatus())) throw new RuntimeException("Only DRAFT can be approved");
        pg.setStatus("APPROVED");
        pg.setApprovedBy(approvedBy);
        pg.setApprovedDate(LocalDate.now());
        return payrollGenerationRepository.save(pg);
    }

    @Override
    @Transactional
    public PayrollGeneration rejectPayroll(Long id, String approvedBy, String reason) {
        PayrollGeneration pg = getPayrollGenerationById(id);
        if (!"DRAFT".equals(pg.getStatus())) throw new RuntimeException("Only DRAFT can be rejected");
        pg.setStatus("REJECTED");
        pg.setApprovedBy(approvedBy);
        pg.setApprovedDate(LocalDate.now());
        pg.setRejectionReason(reason);
        return payrollGenerationRepository.save(pg);
    }

    @Override
    public PayrollGeneration getPayrollGenerationById(Long id) {
        PayrollGeneration pg = payrollGenerationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("PayrollGeneration not found"));
        tenantAccessService.assertPayrollGenerationAccessible(tenantAccessService.requireCurrentEmployee(), pg);
        return pg;
    }

    @Override
    public List<PayrollGeneration> getAllPayrolls(Long organizationId) {
        return payrollGenerationRepository.findByOrganizationIdOrderByGenerationDateDesc(organizationId);
    }

    @Override
    public List<PayrollGeneration> getPayrollsByStatus(Long organizationId, String status) {
        return payrollGenerationRepository.findByOrganizationIdAndStatus(organizationId, status);
    }

    @Override
    public List<PayrollGeneration> getPayrollsByMonthYear(Long organizationId, String monthYear) {
        return payrollGenerationRepository.findAllByOrganizationIdAndMonthYear(organizationId, monthYear);
    }

    @Override
    @Transactional
    public void deletePayroll(Long id) {
        PayrollGeneration pg = payrollGenerationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("PayrollGeneration not found"));
        tenantAccessService.assertPayrollGenerationAccessible(tenantAccessService.requireCurrentEmployee(), pg);
        if ("APPROVED".equals(pg.getStatus())) throw new RuntimeException("Cannot delete approved payroll");
        payrollGenerationRepository.deleteById(id);
    }

    @Override
    public Payroll getPayrollById(Long id) {
        return payrollRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payroll not found"));
    }

    @Override
    public Payroll save(Payroll payroll) {
        return payrollRepository.save(payroll);
    }

    // --- Helpers ---

    private static Long organizationIdFor(Employee emp) {
        if (emp == null || emp.getOrganization() == null) {
            return null;
        }
        return emp.getOrganization().getId();
    }

    private int getWorkingDaysInMonth(YearMonth month) {
        LocalDate start = month.atDay(1), end = month.atEndOfMonth();
        int count = 0;
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            if (d.getDayOfWeek() == DayOfWeek.SATURDAY ||
                    d.getDayOfWeek() == DayOfWeek.SUNDAY) {
                continue;
            }
            count++;
        }
        return count;
    }


    private double calculatePayableDays(List<Attendance> atts) {
        double days = 0;
        for (Attendance a : atts) {
            String s = a.getStatus();
            if ("PRESENT".equalsIgnoreCase(s) || "HOLIDAY".equalsIgnoreCase(s) || (s != null && s.startsWith("LEAVE"))) {
                days += 1;
            } else if ("HALF_DAY".equalsIgnoreCase(s)) {
                days += 0.5;
            }
        }
        return days;
    }

    private double safe(Double d) {
        return d != null ? d : 0;
    }

    private String convertNumberToWords(int number) {
        // implement or hook in a library
        if (number == 0) return "Zero";
        return String.valueOf(number);
    }
}
