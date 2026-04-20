package com.zenyrahr.hrms.Repository;

import com.zenyrahr.hrms.model.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    @Query("SELECT FUNCTION('DATE_FORMAT', e.joinDate, '%Y-%m') as month, COUNT(e) FROM Employee e GROUP BY month ORDER BY month")
    List<Object[]> countJoiningsPerMonth();

    int countByJoinDateBetween(LocalDate start, LocalDate end);

    @Query("SELECT e.department, COUNT(e) FROM Employee e GROUP BY e.department")
    List<Object[]> countByDepartment();

    long countByGender(String gender);
} 