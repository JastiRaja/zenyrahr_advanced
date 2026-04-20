package com.zenyrahr.hrms.Repository;

import com.zenyrahr.hrms.model.AdditionalInformation;
//import com.talvox.hrms.model.PersonalInformation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdditionalInformationRepository extends JpaRepository<AdditionalInformation, Long> {
//    AdditionalInformation findByUserid(User user);

    Optional<AdditionalInformation> findByEmployee_Id(Long employeeId);
}
