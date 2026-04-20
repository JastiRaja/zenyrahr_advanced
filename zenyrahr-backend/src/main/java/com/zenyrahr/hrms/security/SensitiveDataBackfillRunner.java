package com.zenyrahr.hrms.security;

import com.zenyrahr.hrms.Repository.EmployeeRepository;
import com.zenyrahr.hrms.Repository.SalaryAndBankDetailsRepository;
import com.zenyrahr.hrms.model.Employee;
import com.zenyrahr.hrms.model.SalaryAndBankDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class SensitiveDataBackfillRunner implements ApplicationRunner {

    private final Environment environment;
    private final EmployeeRepository employeeRepository;
    private final SalaryAndBankDetailsRepository salaryAndBankDetailsRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        boolean enabled = Boolean.parseBoolean(environment.getProperty("app.data.encryption.backfill-on-startup", "false"));
        if (!enabled) {
            return;
        }
        if (FieldCrypto.isUsingDefaultDevKey()) {
            throw new IllegalStateException("Backfill requires a non-default encryption key. Configure APP_DATA_ENCRYPTION_KEY first.");
        }

        int batchSize = Integer.parseInt(environment.getProperty("app.data.encryption.backfill-batch-size", "200"));
        if (batchSize <= 0) {
            batchSize = 200;
        }

        log.warn("Sensitive data backfill started. This should be run once after enabling field encryption.");
        long employeeRows = backfillEmployees(batchSize);
        long salaryRows = backfillSalaryAndBank(batchSize);
        log.warn("Sensitive data backfill finished. Employee rows processed: {}, SalaryAndBankDetails rows processed: {}.", employeeRows, salaryRows);
    }

    private long backfillEmployees(int batchSize) {
        long processed = 0;
        int pageNumber = 0;
        Page<Employee> page;
        do {
            page = employeeRepository.findAll(PageRequest.of(pageNumber, batchSize));
            if (!page.isEmpty()) {
                employeeRepository.saveAll(page.getContent());
                processed += page.getNumberOfElements();
            }
            pageNumber++;
        } while (page.hasNext());
        return processed;
    }

    private long backfillSalaryAndBank(int batchSize) {
        long processed = 0;
        int pageNumber = 0;
        Page<SalaryAndBankDetails> page;
        do {
            page = salaryAndBankDetailsRepository.findAll(PageRequest.of(pageNumber, batchSize));
            if (!page.isEmpty()) {
                salaryAndBankDetailsRepository.saveAll(page.getContent());
                processed += page.getNumberOfElements();
            }
            pageNumber++;
        } while (page.hasNext());
        return processed;
    }
}
