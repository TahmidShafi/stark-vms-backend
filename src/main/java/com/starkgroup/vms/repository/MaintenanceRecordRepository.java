package com.starkgroup.vms.repository;

import com.starkgroup.vms.model.MaintenanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface MaintenanceRecordRepository extends JpaRepository<MaintenanceRecord, Long> {

    List<MaintenanceRecord> findByVehicleIdOrderByServiceDateDescIdDesc(Long vehicleId);

    boolean existsByVehicleId(Long vehicleId);

    @Query("SELECT COALESCE(SUM(m.cost), 0) FROM MaintenanceRecord m")
    BigDecimal sumTotalMaintenanceCost();
}

