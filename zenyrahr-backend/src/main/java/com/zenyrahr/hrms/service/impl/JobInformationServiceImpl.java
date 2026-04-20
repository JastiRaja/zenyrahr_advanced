package com.zenyrahr.hrms.service.impl;

import com.zenyrahr.hrms.model.JobInformation;
import com.zenyrahr.hrms.Repository.JobInformationRepository;
import com.zenyrahr.hrms.service.JobInformationService;
import com.zenyrahr.hrms.service.SequenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class JobInformationServiceImpl implements JobInformationService {

    private final JobInformationRepository jobInformationRepository;
    private final SequenceService sequenceService;

    @Override
    public JobInformation createJobInformation(JobInformation jobInformation) {
        jobInformation.setCode(sequenceService.getNextCode("JOBINFO"));
        return jobInformationRepository.save(jobInformation);
    }

    @Override
    public Optional<JobInformation> getJobInformationById(Long employeeId) {
        return jobInformationRepository.findById(employeeId);
    }

    @Override
    public List<JobInformation> getAllJobInformation() {
        return jobInformationRepository.findAll();
    }

    @Override
    public JobInformation updateJobInformation(Long id, JobInformation jobInformation) {
        if (jobInformationRepository.existsById(id)) {
            jobInformation.setId(id);
            return jobInformationRepository.save(jobInformation);
        } else {
            throw new RuntimeException("Job Information not found");
        }
    }

    @Override
    public void deleteJobInformation(Long employeeId) {
        jobInformationRepository.deleteById(employeeId);
    }
}
