package com.zenyrahr.hrms.service;

import com.zenyrahr.hrms.model.Payscale;
import java.util.List;

public interface PayscaleService {
    Payscale createPayscale(Payscale payscale);
    Payscale updatePayscale(Long id, Payscale payscale);
    void deletePayscale(Long id);
    Payscale getPayscaleById(Long id);
    List<Payscale> getAllPayscales();

    List<Payscale> getPayscalesByOrganizationId(Long organizationId);
    List<Payscale> getPayscalesByEmployeeId(Long employeeId);
    Payscale getActivePayscaleByEmployeeId(Long employeeId);
    void deactivatePayscale(Long id);
} 