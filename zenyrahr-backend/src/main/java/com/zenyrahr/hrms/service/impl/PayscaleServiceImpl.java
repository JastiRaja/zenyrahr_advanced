package com.zenyrahr.hrms.service.impl;

import com.zenyrahr.hrms.model.Payscale;
import com.zenyrahr.hrms.Repository.PayscaleRepository;
import com.zenyrahr.hrms.service.PayscaleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PayscaleServiceImpl implements PayscaleService {

    private final PayscaleRepository payscaleRepository;

    @Override
    @Transactional
    public Payscale createPayscale(Payscale payscale) {
        try {
            if (payscale.getEmployee() == null || payscale.getEmployee().getId() == null) {
                throw new IllegalArgumentException("Employee and Employee ID must not be null");
            }

            // Optionally check other required fields here

            // Deactivate any existing active payscale for the employee
            payscaleRepository.findByEmployeeIdAndStatus(payscale.getEmployee().getId(), "ACTIVE")
                    .ifPresent(existing -> {
                        existing.setStatus("INACTIVE");
                        existing.setEffectiveTo(LocalDate.now());
                        payscaleRepository.save(existing);
                    });

            // Set new payscale as active
            payscale.setStatus("ACTIVE");
            if (payscale.getEffectiveFrom() == null) {
                payscale.setEffectiveFrom(LocalDate.now());
            }
            // Set audit fields
            payscale.setCreatedAt(LocalDate.now());
            // Set createdBy to the authenticated user's username
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            payscale.setCreatedBy(username);

            return payscaleRepository.save(payscale);
        } catch (Exception e) {
            // Log the error (use your logger)
            System.err.println("Error creating payscale: " + e.getMessage());
            throw e;
        }
    }

    @Override
    @Transactional
    public Payscale updatePayscale(Long id, Payscale payscale) {
        Payscale existingPayscale = payscaleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payscale not found"));

        // Update fields
        existingPayscale.setCtc(payscale.getCtc());
        existingPayscale.setBasicSalary(payscale.getBasicSalary());
        existingPayscale.setHra(payscale.getHra());
        existingPayscale.setDa(payscale.getDa());
        existingPayscale.setAllowance(payscale.getAllowance());
        existingPayscale.setMedicalAllowance(payscale.getMedicalAllowance());
        existingPayscale.setPfContribution(payscale.getPfContribution());
        existingPayscale.setProfessionalTax(payscale.getProfessionalTax());
        existingPayscale.setHealthInsurance(payscale.getHealthInsurance());

        existingPayscale.setUpdatedAt(LocalDate.now());

        return payscaleRepository.save(existingPayscale);
    }

    @Override
    @Transactional
    public void deletePayscale(Long id) {
        payscaleRepository.deleteById(id);
    }

    @Override
    public Payscale getPayscaleById(Long id) {
        return payscaleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payscale not found"));
    }

    @Override
    public List<Payscale> getAllPayscales() {
        return payscaleRepository.findAll();
    }

    @Override
    public List<Payscale> getPayscalesByOrganizationId(Long organizationId) {
        return payscaleRepository.findByEmployee_Organization_IdOrderByIdDesc(organizationId);
    }

    @Override
    public List<Payscale> getPayscalesByEmployeeId(Long employeeId) {
        return payscaleRepository.findByEmployeeId(employeeId);
    }

    @Override
    public Payscale getActivePayscaleByEmployeeId(Long employeeId) {
        return payscaleRepository.findByEmployeeIdAndStatus(employeeId, "ACTIVE")
                .orElseThrow(() -> new RuntimeException("No active payscale found for employee"));
    }

    @Override
    @Transactional
    public void deactivatePayscale(Long id) {
        Payscale payscale = getPayscaleById(id);
        payscale.setStatus("INACTIVE");
        payscale.setEffectiveTo(LocalDate.now());
        payscaleRepository.save(payscale);
    }
} 