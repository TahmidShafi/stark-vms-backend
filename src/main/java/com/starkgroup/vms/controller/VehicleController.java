package com.starkgroup.vms.controller;

import com.starkgroup.vms.model.Driver;
import com.starkgroup.vms.model.Vehicle;
import com.starkgroup.vms.repository.DriverRepository;
import com.starkgroup.vms.repository.MaintenanceRecordRepository;
import com.starkgroup.vms.repository.VehicleDocumentRepository;
import com.starkgroup.vms.repository.VehicleRepository;
import jakarta.annotation.PostConstruct;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping("/api/vehicles")
@CrossOrigin(origins = "*")
public class VehicleController {

    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;
    private final VehicleDocumentRepository vehicleDocumentRepository;
    private final MaintenanceRecordRepository maintenanceRecordRepository;

    public VehicleController(
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

    // Seeds 2 initial database rows if the table is completely empty
    @PostConstruct
    public void seedInitialVehicles() {
        if (vehicleRepository.count() == 0) {
            vehicleRepository.save(new Vehicle("SG-101", "Toyota", "Hilux", 2024, "ACTIVE"));
            vehicleRepository.save(new Vehicle("SG-102", "Mitsubishi", "Outlander", 2023, "MAINTENANCE"));
        }
    }

    // 1. Vehicle List (GET /api/vehicles) - Dual-mode (Legacy List or Paginated)
    @GetMapping
    public ResponseEntity<?> getAllVehicles(
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size,
        @RequestParam(required = false) String search,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String sortBy,
        @RequestParam(required = false) String sortDir
    ) {
        // Strict Legacy Trigger: Return List<Vehicle> ONLY when no pagination, search, filter, or sort params are supplied
        boolean isLegacyRequest = (page == null && size == null
                && (search == null || search.isBlank())
                && (status == null || status.isBlank())
                && (sortBy == null || sortBy.isBlank())
                && (sortDir == null || sortDir.isBlank()));

        if (isLegacyRequest) {
            return ResponseEntity.ok(vehicleRepository.findAll());
        }

        // Paginated Mode
        int pageNum = (page != null && page >= 0) ? page : 0;
        int pageSize = (size != null && size > 0) ? size : 5;

        // Whitelist sort fields to prevent injection or exception
        String safeSortBy = (sortBy != null && Set.of("id", "vehicleNumber", "brand", "model", "manufacturingYear", "status").contains(sortBy))
                ? sortBy
                : "id";

        Sort.Direction direction = ("desc".equalsIgnoreCase(sortDir)) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(pageNum, pageSize, Sort.by(direction, safeSortBy));

        String cleanSearch = (search != null) ? search.trim() : "";
        String cleanStatus = (status != null) ? status.trim().toUpperCase() : "";
        if ("ALL".equals(cleanStatus)) {
            cleanStatus = "";
        }

        Page<Vehicle> vehiclePage = vehicleRepository.searchAndFilterVehicles(cleanSearch, cleanStatus, pageable);

        Map<String, Object> response = Map.of(
            "content", vehiclePage.getContent(),
            "currentPage", vehiclePage.getNumber(),
            "pageSize", vehiclePage.getSize(),
            "totalItems", vehiclePage.getTotalElements(),
            "totalPages", vehiclePage.getTotalPages()
        );

        return ResponseEntity.ok(response);
    }

    // 2. Add/Register Vehicle (POST /api/vehicles) + Duplicate Validation
    @PostMapping
    public ResponseEntity<?> createVehicle(@Valid @RequestBody Vehicle vehicle) {
        String normalizedNumber = vehicle.getVehicleNumber().trim().toUpperCase();

        if (vehicleRepository.existsByVehicleNumberIgnoreCase(normalizedNumber)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Duplicate Vehicle Number: '" + normalizedNumber + "' already exists."));
        }

        vehicle.setVehicleNumber(normalizedNumber);
        Vehicle saved = vehicleRepository.save(vehicle);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // 3. Edit Vehicle (PUT /api/vehicles/{id}) + Duplicate Validation
    @PutMapping("/{id}")
    public ResponseEntity<?> updateVehicle(@PathVariable Long id, @Valid @RequestBody Vehicle updated) {
        return vehicleRepository.findById(id).map(existing -> {
            String normalizedNumber = updated.getVehicleNumber().trim().toUpperCase();

            if (vehicleRepository.existsByVehicleNumberIgnoreCaseAndIdNot(normalizedNumber, id)) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("message",
                                "Duplicate Vehicle Number: '" + normalizedNumber + "' belongs to another vehicle."));
            }

            existing.setVehicleNumber(normalizedNumber);
            existing.setBrand(updated.getBrand().trim());
            existing.setModel(updated.getModel().trim());
            existing.setManufacturingYear(updated.getManufacturingYear());
            existing.setStatus(updated.getStatus());

            return ResponseEntity.ok(vehicleRepository.save(existing));
        }).orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", "Vehicle not found.")));
    }

    // 4. Protective Delete Vehicle (DELETE /api/vehicles/{id})
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteVehicle(@PathVariable Long id) {
        return vehicleRepository.findById(id).map(vehicle -> {
            if (vehicleDocumentRepository.existsByVehicleId(id) || maintenanceRecordRepository.existsByVehicleId(id)) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("message", "Cannot delete vehicle '" + vehicle.getVehicleNumber()
                                + "' because it has linked documents or maintenance records. Set its status to INACTIVE or delete its records first."));
            }
            vehicleRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }).orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", "Vehicle not found.")));
    }

    // 5. Assign/Change/Unassign Driver (PUT /api/vehicles/{id}/driver)
    @PutMapping("/{id}/driver")
    public ResponseEntity<?> assignDriver(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> payload) {
        Optional<Vehicle> vehicleOpt = vehicleRepository.findById(id);
        if (vehicleOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Vehicle not found."));
        }

        Vehicle vehicle = vehicleOpt.get();

        if (payload == null || !payload.containsKey("driverId") || payload.get("driverId") == null) {
            vehicle.setDriver(null);
            Vehicle saved = vehicleRepository.save(vehicle);
            return ResponseEntity.ok(saved);
        }

        Object rawDriverId = payload.get("driverId");
        Long driverId;
        if (rawDriverId instanceof Number number) {
            driverId = number.longValue();
        } else if (rawDriverId instanceof String str) {
            try {
                driverId = Long.parseLong(str.trim());
            } catch (NumberFormatException e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "Invalid driverId format."));
            }
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Invalid driverId format."));
        }

        Optional<Driver> driverOpt = driverRepository.findById(driverId);
        if (driverOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Driver not found."));
        }

        vehicle.setDriver(driverOpt.get());
        Vehicle saved = vehicleRepository.save(vehicle);
        return ResponseEntity.ok(saved);
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
