package com.starkgroup.vms.controller;

import com.starkgroup.vms.dto.VehicleDocumentMetadata;
import com.starkgroup.vms.dto.VehicleDocumentUploadRequest;
import com.starkgroup.vms.model.Vehicle;
import com.starkgroup.vms.model.VehicleDocument;
import com.starkgroup.vms.repository.VehicleDocumentRepository;
import com.starkgroup.vms.repository.VehicleRepository;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@RestController
@CrossOrigin(origins = "*")
public class DocumentController {

    private static final Set<String> ALLOWED_DOC_TYPES = Set.of(
            "REGISTRATION", "TAX_TOKEN", "INSURANCE", "FITNESS_CERTIFICATE", "OTHER"
    );

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".pdf", ".jpg", ".jpeg", ".png"
    );

    private final VehicleDocumentRepository vehicleDocumentRepository;
    private final VehicleRepository vehicleRepository;

    public DocumentController(VehicleDocumentRepository vehicleDocumentRepository, VehicleRepository vehicleRepository) {
        this.vehicleDocumentRepository = vehicleDocumentRepository;
        this.vehicleRepository = vehicleRepository;
    }

    // 1. List Vehicle Documents (Metadata only, SQL projection excludes file_data)
    @GetMapping("/api/vehicles/{vehicleId}/documents")
    public ResponseEntity<?> getVehicleDocuments(@PathVariable Long vehicleId) {
        if (!vehicleRepository.existsById(vehicleId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Vehicle not found."));
        }
        List<VehicleDocumentMetadata> metadataList = vehicleDocumentRepository.findMetadataByVehicleId(vehicleId);
        return ResponseEntity.ok(metadataList);
    }

    // 2. Upload Document with 1 MB Limit & Base64 Validation
    @PostMapping("/api/vehicles/{vehicleId}/documents")
    public ResponseEntity<?> uploadDocument(
            @PathVariable Long vehicleId,
            @Valid @RequestBody VehicleDocumentUploadRequest request
    ) {
        Optional<Vehicle> vehicleOpt = vehicleRepository.findById(vehicleId);
        if (vehicleOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Vehicle not found."));
        }

        String docType = request.getDocumentType() != null ? request.getDocumentType().trim().toUpperCase() : "";
        if (!ALLOWED_DOC_TYPES.contains(docType)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Invalid document type. Allowed types: REGISTRATION, TAX_TOKEN, INSURANCE, FITNESS_CERTIFICATE, OTHER."));
        }

        String fileName = request.getFileName() != null ? request.getFileName().trim() : "";
        if (fileName.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "File name is required."));
        }

        String lowerName = fileName.toLowerCase();
        boolean hasValidExt = ALLOWED_EXTENSIONS.stream().anyMatch(lowerName::endsWith);
        if (!hasValidExt) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Invalid file format. Only PDF, JPG, JPEG, and PNG files are allowed (.pdf, .jpg, .jpeg, .png)."));
        }

        if (request.getExpiryDate() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Expiry date is required."));
        }

        String rawBase64 = request.getFileData();
        if (rawBase64 == null || rawBase64.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "File data is required."));
        }

        if (rawBase64.contains(",")) {
            rawBase64 = rawBase64.substring(rawBase64.indexOf(",") + 1);
        }
        rawBase64 = rawBase64.replaceAll("\\s+", "");

        byte[] decodedBytes;
        try {
            decodedBytes = Base64.getDecoder().decode(rawBase64);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Invalid Base64 file data."));
        }

        if (decodedBytes.length == 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "File cannot be empty."));
        }

        if (decodedBytes.length > 1_048_576) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "File size exceeds 1 MB limit (maximum 1,048,576 bytes)."));
        }

        String contentType = request.getContentType() != null ? request.getContentType().trim() : "";
        if (contentType.isBlank() || "application/octet-stream".equalsIgnoreCase(contentType)) {
            if (lowerName.endsWith(".pdf")) {
                contentType = "application/pdf";
            } else if (lowerName.endsWith(".png")) {
                contentType = "image/png";
            } else if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg")) {
                contentType = "image/jpeg";
            }
        }

        VehicleDocument document = new VehicleDocument();
        document.setVehicle(vehicleOpt.get());
        document.setDocumentType(docType);
        document.setFileName(fileName);
        document.setContentType(contentType);
        document.setExpiryDate(request.getExpiryDate());
        document.setUploadedAt(LocalDateTime.now());
        document.setFileData(rawBase64);

        VehicleDocument saved = vehicleDocumentRepository.save(document);

        VehicleDocumentMetadata metadata = new VehicleDocumentMetadata(
                saved.getId(),
                saved.getVehicle().getId(),
                saved.getDocumentType(),
                saved.getFileName(),
                saved.getContentType(),
                saved.getExpiryDate(),
                saved.getUploadedAt()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(metadata);
    }

    // 3. Binary Document Download
    @GetMapping("/api/documents/{id}/download")
    public ResponseEntity<?> downloadDocument(@PathVariable Long id) {
        Optional<VehicleDocument> docOpt = vehicleDocumentRepository.findById(id);
        if (docOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Document not found."));
        }

        VehicleDocument doc = docOpt.get();
        String rawBase64 = doc.getFileData();
        if (rawBase64 != null && rawBase64.contains(",")) {
            rawBase64 = rawBase64.substring(rawBase64.indexOf(",") + 1);
        }
        if (rawBase64 != null) {
            rawBase64 = rawBase64.replaceAll("\\s+", "");
        }

        byte[] fileBytes;
        try {
            fileBytes = (rawBase64 != null) ? Base64.getDecoder().decode(rawBase64) : new byte[0];
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Corrupted document data."));
        }

        String contentType = doc.getContentType() != null && !doc.getContentType().isBlank()
                ? doc.getContentType()
                : MediaType.APPLICATION_OCTET_STREAM_VALUE;

        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(contentType);
        } catch (Exception e) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + doc.getFileName() + "\"")
                .contentType(mediaType)
                .contentLength(fileBytes.length)
                .body(fileBytes);
    }

    // 4. Delete Document
    @DeleteMapping("/api/documents/{id}")
    public ResponseEntity<?> deleteDocument(@PathVariable Long id) {
        if (!vehicleDocumentRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Document not found."));
        }
        vehicleDocumentRepository.deleteById(id);
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
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "Data integrity violation."));
    }
}

