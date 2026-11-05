package com.starkgroup.vms.repository;

import com.starkgroup.vms.model.Vehicle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    // Used when adding a new vehicle
    boolean existsByVehicleNumberIgnoreCase(String vehicleNumber);

    // Used when editing an existing vehicle (ignores its own ID)
    boolean existsByVehicleNumberIgnoreCaseAndIdNot(String vehicleNumber, Long id);

    List<Vehicle> findByDriverId(Long driverId);

    long countByStatus(String status);

    @Modifying
    @Query("UPDATE Vehicle v SET v.driver = null WHERE v.driver.id = :driverId")
    void clearDriverByDriverId(@Param("driverId") Long driverId);

    @Query("SELECT v FROM Vehicle v WHERE " +
           "(:status IS NULL OR :status = '' OR v.status = :status) AND " +
           "(:search IS NULL OR :search = '' OR " +
           "LOWER(v.vehicleNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(v.brand) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(v.model) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Vehicle> searchAndFilterVehicles(
        @Param("search") String search,
        @Param("status") String status,
        Pageable pageable
    );
}
