package com.zenyrahr.hrms.Repository;

import com.zenyrahr.hrms.model.Holiday;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface HolidayRepository extends JpaRepository<Holiday, Long> {
    List<Holiday> findByYearOrderByDateAsc(Integer year);
    List<Holiday> findByOrganization_IdAndYearOrderByDateAsc(Long organizationId, Integer year);

    boolean existsByDateAndNameIgnoreCase(LocalDate date, String name);
    boolean existsByDateAndNameIgnoreCaseAndOrganization_Id(LocalDate date, String name, Long organizationId);

    boolean existsByDateAndNameIgnoreCaseAndIdNot(LocalDate date, String name, Long id);
    boolean existsByIdAndOrganization_Id(Long id, Long organizationId);
}
