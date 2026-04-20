package com.zenyrahr.hrms.service.impl;

import com.zenyrahr.hrms.model.SalaryAndBankDetails;
import com.zenyrahr.hrms.Repository.SalaryAndBankDetailsRepository;
import com.zenyrahr.hrms.service.SalaryAndBankDetailsService;
import com.zenyrahr.hrms.service.SequenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SalaryAndBankDetailsServiceImpl implements SalaryAndBankDetailsService {

    private final SalaryAndBankDetailsRepository salaryAndBankDetailsRepository;
    private final SequenceService sequenceService;

    @Override
    public SalaryAndBankDetails createSalaryAndBankDetails(SalaryAndBankDetails salaryAndBankDetails) {
        // Set code using sequence service
        salaryAndBankDetails.setCode(sequenceService.getNextCode("SALARYBANK"));
        return salaryAndBankDetailsRepository.save(salaryAndBankDetails); // Save salary and bank details
    }

    @Override
    public Optional<SalaryAndBankDetails> getSalaryAndBankDetailsById(Long id) {
        return salaryAndBankDetailsRepository.findById(id); // Find by id
    }

    @Override
    public List<SalaryAndBankDetails> getAllSalaryAndBankDetails() {
        return salaryAndBankDetailsRepository.findAll(); // Get all salary and bank details records
    }

    @Override
    public SalaryAndBankDetails updateSalaryAndBankDetails(Long id, SalaryAndBankDetails salaryAndBankDetails) {
        if (salaryAndBankDetailsRepository.existsById(id)) {
            salaryAndBankDetails.setId(id); // Ensure the id is set for updating
            return salaryAndBankDetailsRepository.save(salaryAndBankDetails); // Save updated record
        } else {
            throw new RuntimeException("Salary and Bank Details not found");
        }
    }

    @Override
    public void deleteSalaryAndBankDetails(Long id) {
        salaryAndBankDetailsRepository.deleteById(id); // Delete record by id
    }
}
