package com.starkgroup.vms.repository;

import com.starkgroup.vms.model.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    // Used when adding a new vehicle
    boolean existsByVehicleNumberIgnoreCase(String vehicleNumber);

    // Used when editing an existing vehicle (ignores its own ID)
    boolean existsByVehicleNumberIgnoreCaseAndIdNot(String vehicleNumber, Long id);
}