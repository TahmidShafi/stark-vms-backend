package com.starkgroup.vms.repository;

import com.starkgroup.vms.dto.VehicleDocumentMetadata;
import com.starkgroup.vms.model.VehicleDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface VehicleDocumentRepository extends JpaRepository<VehicleDocument, Long> {

    @Query("SELECT new com.starkgroup.vms.dto.VehicleDocumentMetadata(" +
           "d.id, d.vehicle.id, d.documentType, d.fileName, d.contentType, d.expiryDate, d.uploadedAt) " +
           "FROM VehicleDocument d WHERE d.vehicle.id = :vehicleId ORDER BY d.uploadedAt DESC")
    List<VehicleDocumentMetadata> findMetadataByVehicleId(@Param("vehicleId") Long vehicleId);

    long countByExpiryDateBefore(LocalDate date);

    long countByExpiryDateBetween(LocalDate startDate, LocalDate endDate);

    long countByExpiryDateAfter(LocalDate date);

    boolean existsByVehicleId(Long vehicleId);

    void deleteByVehicleId(Long vehicleId);
}

