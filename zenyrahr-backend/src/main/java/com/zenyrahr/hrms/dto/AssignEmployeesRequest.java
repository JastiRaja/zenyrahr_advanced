package com.zenyrahr.hrms.dto;

import lombok.Data;

import java.util.List;

@Data
public class AssignEmployeesRequest {
    private List<Long> employeeIds;
}
