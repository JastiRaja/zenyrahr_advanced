package com.zenyrahr.hrms.Repository;

import com.zenyrahr.hrms.model.TravelRequest;
import com.zenyrahr.hrms.model.TravelStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TravelRequestRepository extends JpaRepository<TravelRequest, Long> {
    // Method to find travel requests by employee ID
    List<TravelRequest> findByEmployee_Id(Long employeeId);

    List<TravelRequest> findByFirstLevelApprovalStatus(TravelStatus status);
    List<TravelRequest> findBySecondLevelApprovalStatus(TravelStatus status);
    List<TravelRequest> findByFirstLevelApprovalStatusAndEmployee_Id(TravelStatus status, Long employeeId);
    List<TravelRequest> findBySecondLevelApprovalStatusAndEmployee_Id(TravelStatus status, Long employeeId);
    List<TravelRequest> findByFirstLevelApprovalStatusAndSecondLevelApprovalStatus(TravelStatus firstLevelStatus, TravelStatus secondLevelStatus);
    List<TravelRequest> findByFirstLevelApprovalStatusAndSecondLevelApprovalStatusAndEmployee_Id(TravelStatus firstLevelStatus, TravelStatus secondLevelStatus, Long employeeId);
}
