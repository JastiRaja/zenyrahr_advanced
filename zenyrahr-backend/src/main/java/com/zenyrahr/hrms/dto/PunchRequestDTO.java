package com.zenyrahr.hrms.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PunchRequestDTO {
    private Double latitude;
    private Double longitude;
    /** Optional human-readable label (e.g. reverse-geocoded address). */
    private String locationLabel;
}
