package com.starkgroup.vms.controller;

import com.starkgroup.vms.model.VehicleDocument;
import com.starkgroup.vms.repository.DriverRepository;
import com.starkgroup.vms.repository.MaintenanceRecordRepository;
import com.starkgroup.vms.repository.VehicleDocumentRepository;
import com.starkgroup.vms.repository.VehicleRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*")
public class DashboardController {

    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;
    private final VehicleDocumentRepository vehicleDocumentRepository;
    private final MaintenanceRecordRepository maintenanceRecordRepository;

    public DashboardController(
            VehicleRepository vehicleRepository,
            DriverRepository driverRepository,
            VehicleDocumentRepository vehicleDocumentRepository,
            MaintenanceRecordRepository maintenanceRecordRepository
    ) {
        this.vehicleRepository = vehicleRepository;
        this.driverRepository = driverRepository;
        this.vehicleDocumentRepository = vehicleDocumentRepository;
        this.maintenanceRecordRepository = maintenanceRecordRepository;
    }

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getDashboardSummary() {
        LocalDate today = LocalDate.now();
        LocalDate thresholdDate = today.plusDays(VehicleDocument.EXPIRING_SOON_DAYS);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalVehicles", vehicleRepository.count());
        summary.put("activeVehicles", vehicleRepository.countByStatus("ACTIVE"));
        summary.put("maintenanceVehicles", vehicleRepository.countByStatus("MAINTENANCE"));
        summary.put("inactiveVehicles", vehicleRepository.countByStatus("INACTIVE"));
        summary.put("totalDrivers", driverRepository.count());
        summary.put("expiredDocumentsCount", vehicleDocumentRepository.countByExpiryDateBefore(today));
        summary.put("expiringSoonDocumentsCount", vehicleDocumentRepository.countByExpiryDateBetween(today, thresholdDate));
        summary.put("validDocumentsCount", vehicleDocumentRepository.countByExpiryDateAfter(thresholdDate));
        summary.put("totalMaintenanceRecords", maintenanceRecordRepository.count());
        summary.put("totalMaintenanceCost", maintenanceRecordRepository.sumTotalMaintenanceCost());

        return ResponseEntity.ok(summary);
    }
}

