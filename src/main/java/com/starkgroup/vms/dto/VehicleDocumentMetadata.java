package com.starkgroup.vms.dto;

import com.starkgroup.vms.model.VehicleDocument;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class VehicleDocumentMetadata {

    private Long id;
    private Long vehicleId;
    private String documentType;
    private String fileName;
    private String contentType;
    private LocalDate expiryDate;
    private LocalDateTime uploadedAt;
    private String expiryStatus;

    public VehicleDocumentMetadata() {
    }

    public VehicleDocumentMetadata(
            Long id,
            Long vehicleId,
            String documentType,
            String fileName,
            String contentType,
            LocalDate expiryDate,
            LocalDateTime uploadedAt
    ) {
        this.id = id;
        this.vehicleId = vehicleId;
        this.documentType = documentType;
        this.fileName = fileName;
        this.contentType = contentType;
        this.expiryDate = expiryDate;
        this.uploadedAt = uploadedAt;
        this.expiryStatus = VehicleDocument.computeExpiryStatus(expiryDate);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(Long vehicleId) {
        this.vehicleId = vehicleId;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
        this.expiryStatus = VehicleDocument.computeExpiryStatus(expiryDate);
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public String getExpiryStatus() {
        if (expiryStatus == null && expiryDate != null) {
            expiryStatus = VehicleDocument.computeExpiryStatus(expiryDate);
        }
        return expiryStatus;
    }

    public void setExpiryStatus(String expiryStatus) {
        this.expiryStatus = expiryStatus;
    }
}

