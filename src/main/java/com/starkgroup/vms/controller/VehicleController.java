package com.starkgroup.vms.controller;

import com.starkgroup.vms.model.Vehicle;
import com.starkgroup.vms.repository.VehicleRepository;
import jakarta.annotation.PostConstruct;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/vehicles")
@CrossOrigin(origins = "*")
public class VehicleController {

    private final VehicleRepository vehicleRepository;

    public VehicleController(VehicleRepository vehicleRepository) {
        this.vehicleRepository = vehicleRepository;
    }

    // Seeds 2 initial database rows if the table is completely empty
    @PostConstruct
    public void seedInitialVehicles() {
        if (vehicleRepository.count() == 0) {
            vehicleRepository.save(new Vehicle("SG-101", "Toyota", "Hilux", 2024, "ACTIVE"));
            vehicleRepository.save(new Vehicle("SG-102", "Mitsubishi", "Outlander", 2023, "MAINTENANCE"));
        }
    }

    // 1. Vehicle List (GET /api/vehicles)
    @GetMapping
    public List<Vehicle> getAllVehicles() {
        return vehicleRepository.findAll();
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

    // 4. Delete Vehicle (DELETE /api/vehicles/{id})
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteVehicle(@PathVariable Long id) {
        if (!vehicleRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Vehicle not found."));
        }
        vehicleRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}