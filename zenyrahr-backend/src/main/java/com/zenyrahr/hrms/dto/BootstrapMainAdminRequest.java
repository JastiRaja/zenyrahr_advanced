package com.zenyrahr.hrms.dto;

import lombok.Data;

@Data
public class BootstrapMainAdminRequest {
    private String firstName;
    private String lastName;
    private String username;
    private String password;
    private String workLocation;
}
