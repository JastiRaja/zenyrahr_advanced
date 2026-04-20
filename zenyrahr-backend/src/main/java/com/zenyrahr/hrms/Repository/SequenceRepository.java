package com.zenyrahr.hrms.Repository;

import com.zenyrahr.hrms.model.Sequence;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SequenceRepository extends JpaRepository<Sequence, Long> {
    Sequence findByPrefix(String prefix);
}
