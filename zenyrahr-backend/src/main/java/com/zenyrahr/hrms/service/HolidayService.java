package com.zenyrahr.hrms.service;

import com.zenyrahr.hrms.model.Holiday;

import java.util.List;

public interface HolidayService {
    List<Holiday> getHolidaysByYear(Long organizationId, Integer year);

    Holiday addHoliday(Long organizationId, Holiday holiday);

    List<Holiday> addHolidays(Long organizationId, List<Holiday> holidays);

    void deleteHoliday(Long organizationId, Long id);
}
