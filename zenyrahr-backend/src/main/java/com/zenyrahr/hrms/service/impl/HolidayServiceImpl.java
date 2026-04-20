package com.zenyrahr.hrms.service.impl;

import com.zenyrahr.hrms.Repository.HolidayRepository;
import com.zenyrahr.hrms.Repository.OrganizationRepository;
import com.zenyrahr.hrms.model.Holiday;
import com.zenyrahr.hrms.model.Organization;
import com.zenyrahr.hrms.service.HolidayService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HolidayServiceImpl implements HolidayService {

    private final HolidayRepository holidayRepository;
    private final OrganizationRepository organizationRepository;

    @Override
    public List<Holiday> getHolidaysByYear(Long organizationId, Integer year) {
        return holidayRepository.findByOrganization_IdAndYearOrderByDateAsc(organizationId, year);
    }

    @Override
    public Holiday addHoliday(Long organizationId, Holiday holiday) {
        validateHoliday(holiday);
        if (holidayRepository.existsByDateAndNameIgnoreCaseAndOrganization_Id(
                holiday.getDate(),
                holiday.getName().trim(),
                organizationId
        )) {
            throw new IllegalArgumentException("Holiday already exists for the same date and name.");
        }
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found."));
        holiday.setOrganization(organization);
        holiday.setName(holiday.getName().trim());
        return holidayRepository.save(holiday);
    }

    @Override
    public List<Holiday> addHolidays(Long organizationId, List<Holiday> holidays) {
        if (holidays == null || holidays.isEmpty()) {
            throw new IllegalArgumentException("At least one holiday is required.");
        }
        return holidays.stream().map(holiday -> addHoliday(organizationId, holiday)).toList();
    }

    @Override
    public void deleteHoliday(Long organizationId, Long id) {
        if (!holidayRepository.existsByIdAndOrganization_Id(id, organizationId)) {
            throw new IllegalArgumentException("Holiday not found.");
        }
        holidayRepository.deleteById(id);
    }

    private void validateHoliday(Holiday holiday) {
        if (holiday == null) {
            throw new IllegalArgumentException("Holiday data is required.");
        }
        if (holiday.getDate() == null) {
            throw new IllegalArgumentException("Holiday date is required.");
        }
        if (holiday.getName() == null || holiday.getName().isBlank()) {
            throw new IllegalArgumentException("Holiday name is required.");
        }

        LocalDate date = holiday.getDate();
        Integer requestedYear = holiday.getYear();
        if (requestedYear == null) {
            holiday.setYear(date.getYear());
        } else if (!requestedYear.equals(date.getYear())) {
            throw new IllegalArgumentException("Holiday year must match holiday date year.");
        }
    }
}
