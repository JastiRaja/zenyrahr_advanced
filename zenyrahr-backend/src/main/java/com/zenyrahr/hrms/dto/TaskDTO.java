package com.zenyrahr.hrms.dto;

import com.zenyrahr.hrms.model.ProcessType;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter

public class TaskDTO {
    private String taskName;
    private String description;
    private boolean mandatory;
    private List<Long> employeeTaskIds;// IDs of employee tasks linked to this task
    private Long employeeId; //
    private ProcessType processType;  // Whether this task is for onboarding or offboarding


//    public String getTaskName() {
//    }
//
//    public String getDescription() {
//    }
//
//    public ProcessType getProcessType() {
//    }
//
//    public Long getEmployeeId() {
//    }
}

