package com.starkgroup.vms.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "vehicle_documents")
public class VehicleDocument {

    public static final int EXPIRING_SOON_DAYS = 30;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_id", nullable = false, foreignKey = @ForeignKey(name = "fk_vehicle_documents_vehicle"))
    @JsonIgnore
    private Vehicle vehicle;

    @NotBlank(message = "Document type is required")
    @Pattern(regexp = "^(REGISTRATION|TAX_TOKEN|INSURANCE|FITNESS_CERTIFICATE|OTHER)$",
            message = "Document type must be REGISTRATION, TAX_TOKEN, INSURANCE, FITNESS_CERTIFICATE, or OTHER")
    @Column(name = "document_type", nullable = false, length = 50)
    private String documentType;

    @NotBlank(message = "File name is required")
    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @NotBlank(message = "Content type is required")
    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @NotNull(message = "Expiry date is required")
    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;

    @JsonIgnore
    @NotBlank(message = "File data is required")
    @Column(name = "file_data", columnDefinition = "LONGTEXT", nullable = false)
    private String fileData;

    public VehicleDocument() {
    }

    public VehicleDocument(Vehicle vehicle, String documentType, String fileName, String contentType, LocalDate expiryDate, String fileData) {
        this.vehicle = vehicle;
        this.documentType = documentType;
        this.fileName = fileName;
        this.contentType = contentType;
        this.expiryDate = expiryDate;
        this.fileData = fileData;
        this.uploadedAt = LocalDateTime.now();
    }

    @PrePersist
    public void onPrePersist() {
        if (this.uploadedAt == null) {
            this.uploadedAt = LocalDateTime.now();
        }
    }

    public static String computeExpiryStatus(LocalDate expiryDate) {
        if (expiryDate == null) {
            return "UNKNOWN";
        }
        LocalDate today = LocalDate.now();
        LocalDate thresholdDate = today.plusDays(EXPIRING_SOON_DAYS);
        if (expiryDate.isBefore(today)) {
            return "EXPIRED";
        } else if (!expiryDate.isAfter(thresholdDate)) {
            return "EXPIRING_SOON";
        } else {
            return "VALID";
        }
    }

    @Transient
    public String getExpiryStatus() {
        return computeExpiryStatus(this.expiryDate);
    }

    @Transient
    public Long getVehicleId() {
        return vehicle != null ? vehicle.getId() : null;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public void setVehicle(Vehicle vehicle) {
        this.vehicle = vehicle;
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
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public String getFileData() {
        return fileData;
    }

    public void setFileData(String fileData) {
        this.fileData = fileData;
    }
}

