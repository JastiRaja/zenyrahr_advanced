package com.zenyrahr.hrms.model;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Data;
import org.antlr.v4.runtime.misc.NotNull;

import java.time.LocalDateTime;

@MappedSuperclass
@Data
 public abstract class BaseEntity2 {

        @Column(unique = true, nullable = false, updatable = false)
//        @Column(unique = true, nullable = false)
        private String code;

        @Column(nullable = false)
        private Boolean active = true;

        @Column(nullable = false)
        private Boolean deleted = false;

        @Column(nullable = false, updatable = false)
        private LocalDateTime createdAt = LocalDateTime.now();

        private LocalDateTime updatedAt;

        @PrePersist
        public void prePersist() {
            this.createdAt = LocalDateTime.now();
            this.updatedAt = LocalDateTime.now();
        }

        @PreUpdate
        public void preUpdate() {
            this.updatedAt = LocalDateTime.now();
        }
    }
