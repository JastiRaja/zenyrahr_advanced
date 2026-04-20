package com.zenyrahr.hrms.service;

import com.zenyrahr.hrms.Repository.SequenceRepository;
import com.zenyrahr.hrms.model.Sequence;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SequenceService {
    private final SequenceRepository sequenceRepository;

    @Transactional
    public String getNextCode(String prefix) {
        Sequence sequence = sequenceRepository.findByPrefix(prefix);
        if (sequence == null) {
            sequence = new Sequence();
            sequence.setPrefix(prefix);
            sequence.setCurrentValue(1);
        } else {
            sequence.setCurrentValue(sequence.getCurrentValue() + 1);
        }
        sequenceRepository.save(sequence);
        return String.format("%s-%05d", prefix, sequence.getCurrentValue());
    }
}
