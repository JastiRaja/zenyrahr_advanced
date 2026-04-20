package com.zenyrahr.hrms.service.impl;

import com.zenyrahr.hrms.model.StateDetails;
import com.zenyrahr.hrms.Repository.StateDetailsRepository;
import com.zenyrahr.hrms.service.SequenceService;
import com.zenyrahr.hrms.service.StateDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StateDetailsServiceImpl implements StateDetailsService {

    private final StateDetailsRepository stateDetailsRepository;
    private final SequenceService sequenceService;

    @Override
    public StateDetails createState(StateDetails stateDetails) {
        stateDetails.setCode(sequenceService.getNextCode("STATE"));
        return stateDetailsRepository.save(stateDetails);
    }

    @Override
    public Optional<StateDetails> getStateById(Long id) {
        return stateDetailsRepository.findById(id);
    }

    @Override
    public List<StateDetails> getAllStates() {
        return stateDetailsRepository.findAll();
    }

    @Override
    public StateDetails updateState(Long id, StateDetails stateDetails) {
        if (stateDetailsRepository.existsById(id)) {
            stateDetails.setId(id);
            return stateDetailsRepository.save(stateDetails);
        } else {
            throw new RuntimeException("State not found");
        }
    }

    @Override
    public void deleteState(Long id) {
        stateDetailsRepository.deleteById(id);
    }
}
