package com.starkgroup.vms.controller;

import com.starkgroup.vms.model.MaintenanceRecord;
import com.starkgroup.vms.model.Vehicle;
import com.starkgroup.vms.repository.MaintenanceRecordRepository;
import com.starkgroup.vms.repository.VehicleRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@CrossOrigin(origins = "*")
public class MaintenanceController {

    private final MaintenanceRecordRepository maintenanceRecordRepository;
    private final VehicleRepository vehicleRepository;

    public MaintenanceController(MaintenanceRecordRepository maintenanceRecordRepository, VehicleRepository vehicleRepository) {
        this.maintenanceRecordRepository = maintenanceRecordRepository;
        this.vehicleRepository = vehicleRepository;
    }

    // 1. List Vehicle Maintenance Records (Ordered by service date desc, id desc)
    @GetMapping("/api/vehicles/{vehicleId}/maintenance")
    public ResponseEntity<?> getVehicleMaintenanceRecords(@PathVariable Long vehicleId) {
        if (!vehicleRepository.existsById(vehicleId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Vehicle not found."));
        }
        List<MaintenanceRecord> records = maintenanceRecordRepository.findByVehicleIdOrderByServiceDateDescIdDesc(vehicleId);
        return ResponseEntity.ok(records);
    }

    // 2. Add Maintenance Record
    @PostMapping("/api/vehicles/{vehicleId}/maintenance")
    public ResponseEntity<?> addMaintenanceRecord(
            @PathVariable Long vehicleId,
            @Valid @RequestBody MaintenanceRecord record
    ) {
        Optional<Vehicle> vehicleOpt = vehicleRepository.findById(vehicleId);
        if (vehicleOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Vehicle not found."));
        }

        record.setVehicle(vehicleOpt.get());
        MaintenanceRecord saved = maintenanceRecordRepository.save(record);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // 3. Delete Maintenance Record
    @DeleteMapping("/api/maintenance/{id}")
    public ResponseEntity<?> deleteMaintenanceRecord(@PathVariable Long id) {
        if (!maintenanceRecordRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Maintenance record not found."));
        }
        maintenanceRecordRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .findFirst()
                .orElse("Validation failed");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", errorMessage));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleReadableException(HttpMessageNotReadableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Malformed JSON request body"));
    }
}

