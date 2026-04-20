package com.zenyrahr.hrms.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PayrollDeductionUpdateRequest {
    private String epfAmount;
    private String healthInsuranceDeduction;
    private String professionalTax;
    private String otherDeductions;
} 