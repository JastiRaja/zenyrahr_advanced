package com.zenyrahr.hrms.service;

import com.zenyrahr.hrms.model.JobInformation;

import java.util.List;
import java.util.Optional;

public interface JobInformationService {

    JobInformation createJobInformation(JobInformation jobInformation);

    Optional<JobInformation> getJobInformationById(Long id);

    List<JobInformation> getAllJobInformation();

    JobInformation updateJobInformation(Long id, JobInformation jobInformation);

    void deleteJobInformation(Long id);
}
