package com.zenyrahr.hrms.service;

import com.zenyrahr.hrms.model.SalaryAndBankDetails;

import java.util.List;
import java.util.Optional;

public interface SalaryAndBankDetailsService {

    SalaryAndBankDetails createSalaryAndBankDetails(SalaryAndBankDetails salaryAndBankDetails);

    Optional<SalaryAndBankDetails> getSalaryAndBankDetailsById(Long id);

    List<SalaryAndBankDetails> getAllSalaryAndBankDetails();

    SalaryAndBankDetails updateSalaryAndBankDetails(Long id, SalaryAndBankDetails salaryAndBankDetails);

    void deleteSalaryAndBankDetails(Long id);
}
