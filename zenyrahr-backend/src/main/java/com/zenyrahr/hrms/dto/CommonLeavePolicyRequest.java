package com.zenyrahr.hrms.dto;

import lombok.Data;

import java.util.List;

@Data
public class CommonLeavePolicyRequest {
    private String yearMode; // CALENDAR | FINANCIAL
    private Integer yearStart;
    private List<AllocationItem> allocations;

    @Data
    public static class AllocationItem {
        private Long leaveTypeId;
        private Integer days;
    }
}
