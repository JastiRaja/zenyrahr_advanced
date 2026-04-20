package com.zenyrahr.hrms.dto;

import lombok.Data;

@Data
public class EmployeeCodeSettingsDTO {
    private String employeeCodePrefix;
    private Integer employeeCodePadding;
    private Integer nextEmployeeCodeNumber;
    private Boolean allowManualEmployeeCodeOverride;
}

