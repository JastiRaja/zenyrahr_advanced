package com.zenyrahr.hrms.service;

import com.zenyrahr.hrms.model.StateDetails;

import java.util.List;
import java.util.Optional;

public interface StateDetailsService {

    StateDetails createState(StateDetails stateDetails);

    Optional<StateDetails> getStateById(Long id);

    List<StateDetails> getAllStates();

    StateDetails updateState(Long id, StateDetails stateDetails);

    void deleteState(Long id);
}
