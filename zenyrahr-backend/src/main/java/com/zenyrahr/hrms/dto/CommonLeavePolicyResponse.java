package com.zenyrahr.hrms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommonLeavePolicyResponse {
    private String yearMode;
    private Integer yearStart;
    private Integer employeeCount;
    private List<Allocation> allocations;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Allocation {
        private Long leaveTypeId;
        private String leaveTypeName;
        private Integer days;
    }
}
