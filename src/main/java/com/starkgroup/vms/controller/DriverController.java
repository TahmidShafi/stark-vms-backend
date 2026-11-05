package com.starkgroup.vms.controller;

import com.starkgroup.vms.model.Driver;
import com.starkgroup.vms.repository.DriverRepository;
import com.starkgroup.vms.repository.VehicleRepository;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/drivers")
@CrossOrigin(origins = "*")
public class DriverController {

    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;

    public DriverController(DriverRepository driverRepository, VehicleRepository vehicleRepository) {
        this.driverRepository = driverRepository;
        this.vehicleRepository = vehicleRepository;
    }

    @GetMapping
    public List<Driver> getAllDrivers() {
        return driverRepository.findAll();
    }

    @PostMapping
    public ResponseEntity<?> createDriver(@Valid @RequestBody Driver driver) {
        String normalizedLicense = driver.getLicenseNumber() != null
                ? driver.getLicenseNumber().trim().toUpperCase()
                : "";

        if (driverRepository.existsByLicenseNumberIgnoreCase(normalizedLicense)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Duplicate License Number: '" + normalizedLicense + "' already exists."));
        }

        driver.setLicenseNumber(normalizedLicense);
        Driver saved = driverRepository.save(driver);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateDriver(@PathVariable Long id, @Valid @RequestBody Driver updated) {
        return driverRepository.findById(id).map(existing -> {
            String normalizedLicense = updated.getLicenseNumber() != null
                    ? updated.getLicenseNumber().trim().toUpperCase()
                    : "";

            if (driverRepository.existsByLicenseNumberIgnoreCaseAndIdNot(normalizedLicense, id)) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("message",
                                "Duplicate License Number: '" + normalizedLicense + "' belongs to another driver."));
            }

            existing.setName(updated.getName().trim());
            existing.setLicenseNumber(normalizedLicense);
            existing.setPhone(updated.getPhone().trim());
            existing.setStatus(updated.getStatus());

            return ResponseEntity.ok(driverRepository.save(existing));
        }).orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", "Driver not found.")));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> deleteDriver(@PathVariable Long id) {
        if (!driverRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Driver not found."));
        }
        vehicleRepository.clearDriverByDriverId(id);
        driverRepository.deleteById(id);
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

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "Data integrity violation: duplicate or invalid reference."));
    }
}
