package com.starkgroup.vms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

public class VehicleDocumentUploadRequest {

    @NotBlank(message = "Document type is required")
    @Pattern(regexp = "^(REGISTRATION|TAX_TOKEN|INSURANCE|FITNESS_CERTIFICATE|OTHER)$",
            message = "Document type must be REGISTRATION, TAX_TOKEN, INSURANCE, FITNESS_CERTIFICATE, or OTHER")
    private String documentType;

    @NotBlank(message = "File name is required")
    private String fileName;

    private String contentType;

    @NotNull(message = "Expiry date is required")
    private LocalDate expiryDate;

    @NotBlank(message = "File data is required")
    private String fileData;

    public VehicleDocumentUploadRequest() {
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

    public String getFileData() {
        return fileData;
    }

    public void setFileData(String fileData) {
        this.fileData = fileData;
    }
}

