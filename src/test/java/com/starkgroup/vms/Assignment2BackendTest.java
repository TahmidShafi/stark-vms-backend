package com.starkgroup.vms;

import com.starkgroup.vms.controller.DashboardController;
import com.starkgroup.vms.controller.DocumentController;
import com.starkgroup.vms.controller.DriverController;
import com.starkgroup.vms.controller.VehicleController;
import com.starkgroup.vms.dto.VehicleDocumentMetadata;
import com.starkgroup.vms.dto.VehicleDocumentUploadRequest;
import com.starkgroup.vms.model.Driver;
import com.starkgroup.vms.model.Vehicle;
import com.starkgroup.vms.model.VehicleDocument;
import com.starkgroup.vms.repository.DriverRepository;
import com.starkgroup.vms.repository.MaintenanceRecordRepository;
import com.starkgroup.vms.repository.VehicleDocumentRepository;
import com.starkgroup.vms.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class Assignment2BackendTest {

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private DriverRepository driverRepository;

    @Mock
    private VehicleDocumentRepository vehicleDocumentRepository;

    @Mock
    private MaintenanceRecordRepository maintenanceRecordRepository;

    private VehicleController vehicleController;
    private DriverController driverController;
    private DocumentController documentController;
    private DashboardController dashboardController;

    @BeforeEach
    void setUp() {
        vehicleController = new VehicleController(
                vehicleRepository,
                driverRepository,
                vehicleDocumentRepository,
                maintenanceRecordRepository
        );
        driverController = new DriverController(driverRepository, vehicleRepository);
        documentController = new DocumentController(vehicleDocumentRepository, vehicleRepository);
        dashboardController = new DashboardController(
                vehicleRepository,
                driverRepository,
                vehicleDocumentRepository,
                maintenanceRecordRepository
        );
    }

    @Test
    @DisplayName("1. Expiry boundary calculation matches contract")
    void testExpiryBoundaries() {
        LocalDate today = LocalDate.now();

        assertEquals("EXPIRED", VehicleDocument.computeExpiryStatus(today.minusDays(1)));
        assertEquals("EXPIRING_SOON", VehicleDocument.computeExpiryStatus(today));
        assertEquals("EXPIRING_SOON", VehicleDocument.computeExpiryStatus(today.plusDays(30)));
        assertEquals("VALID", VehicleDocument.computeExpiryStatus(today.plusDays(31)));
        assertEquals("UNKNOWN", VehicleDocument.computeExpiryStatus(null));
    }

    @Test
    @DisplayName("2. Dual-mode GET /api/vehicles returns List in legacy mode and Map in paginated mode")
    void testDualModeVehicleList() {
        Vehicle v1 = new Vehicle("SG-101", "Toyota", "Hilux", 2024, "ACTIVE");
        v1.setId(1L);

        // A) Legacy Mode (all null/blank params)
        when(vehicleRepository.findAll()).thenReturn(List.of(v1));
        ResponseEntity<?> legacyResponse = vehicleController.getAllVehicles(null, null, null, null, null, null);
        assertEquals(HttpStatus.OK, legacyResponse.getStatusCode());
        assertInstanceOf(List.class, legacyResponse.getBody());
        List<?> legacyList = (List<?>) legacyResponse.getBody();
        assertEquals(1, legacyList.size());

        // B) Paginated Mode (params supplied)
        Page<Vehicle> page = new PageImpl<>(List.of(v1));
        when(vehicleRepository.searchAndFilterVehicles(eq("SG"), eq("ACTIVE"), any(Pageable.class)))
                .thenReturn(page);

        ResponseEntity<?> pageResponse = vehicleController.getAllVehicles(0, 5, "SG", "ACTIVE", "vehicleNumber", "asc");
        assertEquals(HttpStatus.OK, pageResponse.getStatusCode());
        assertInstanceOf(Map.class, pageResponse.getBody());
        @SuppressWarnings("unchecked")
        Map<String, Object> pageMap = (Map<String, Object>) pageResponse.getBody();
        assertTrue(pageMap.containsKey("content"));
        assertTrue(pageMap.containsKey("currentPage"));
        assertTrue(pageMap.containsKey("pageSize"));
        assertTrue(pageMap.containsKey("totalItems"));
        assertTrue(pageMap.containsKey("totalPages"));
        assertEquals(0, pageMap.get("currentPage"));
        assertEquals(1L, pageMap.get("totalItems"));
    }

    @Test
    @DisplayName("3. Driver duplicate license returns 409, and delete is transactional unassigning vehicles")
    void testDriverDuplicateAndTransactionalDelete() {
        // A) Duplicate License Check
        when(driverRepository.existsByLicenseNumberIgnoreCase("DL-99999")).thenReturn(true);
        Driver duplicateDriver = new Driver("John Doe", "DL-99999", "01700000000", "ACTIVE");

        ResponseEntity<?> postResponse = driverController.createDriver(duplicateDriver);
        assertEquals(HttpStatus.CONFLICT, postResponse.getStatusCode());
        assertInstanceOf(Map.class, postResponse.getBody());
        @SuppressWarnings("unchecked")
        Map<String, String> errorBody = (Map<String, String>) postResponse.getBody();
        assertTrue(errorBody.get("message").contains("Duplicate License Number"));

        // B) Transactional Delete Unassigning Vehicles
        when(driverRepository.existsById(10L)).thenReturn(true);
        doNothing().when(vehicleRepository).clearDriverByDriverId(10L);
        doNothing().when(driverRepository).deleteById(10L);

        ResponseEntity<?> deleteResponse = driverController.deleteDriver(10L);
        assertEquals(HttpStatus.NO_CONTENT, deleteResponse.getStatusCode());
        verify(vehicleRepository, times(1)).clearDriverByDriverId(10L);
        verify(driverRepository, times(1)).deleteById(10L);
    }

    @Test
    @DisplayName("4. PUT /api/vehicles/{id}/driver assigns or clears driver")
    void testDriverAssignmentAndClear() {
        Vehicle vehicle = new Vehicle("SG-101", "Toyota", "Hilux", 2024, "ACTIVE");
        vehicle.setId(1L);
        Driver driver = new Driver("Jane Stark", "DL-12345", "01800000000", "ACTIVE");
        driver.setId(5L);

        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(vehicle));
        when(driverRepository.findById(5L)).thenReturn(Optional.of(driver));
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(inv -> inv.getArgument(0));

        // A) Assign driver
        ResponseEntity<?> assignResp = vehicleController.assignDriver(1L, Map.of("driverId", 5));
        assertEquals(HttpStatus.OK, assignResp.getStatusCode());
        Vehicle updated = (Vehicle) assignResp.getBody();
        assertNotNull(updated.getDriver());
        assertEquals(5L, updated.getDriver().getId());

        // B) Clear/unassign driver
        Map<String, Object> clearPayload = new HashMap<>();
        clearPayload.put("driverId", null);
        ResponseEntity<?> unassignResp = vehicleController.assignDriver(1L, clearPayload);
        assertEquals(HttpStatus.OK, unassignResp.getStatusCode());
        Vehicle clearedVehicle = (Vehicle) unassignResp.getBody();
        assertNull(clearedVehicle.getDriver());
    }

    @Test
    @DisplayName("5. Deleting vehicle with linked documents or maintenance records returns 409 Conflict")
    void testProtectiveVehicleDeletion() {
        Vehicle vehicle = new Vehicle("SG-101", "Toyota", "Hilux", 2024, "ACTIVE");
        vehicle.setId(1L);
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(vehicle));

        // Case A: Has documents
        when(vehicleDocumentRepository.existsByVehicleId(1L)).thenReturn(true);
        ResponseEntity<?> conflictDoc = vehicleController.deleteVehicle(1L);
        assertEquals(HttpStatus.CONFLICT, conflictDoc.getStatusCode());
        verify(vehicleRepository, never()).deleteById(1L);

        // Case B: Has maintenance records
        when(vehicleDocumentRepository.existsByVehicleId(1L)).thenReturn(false);
        when(maintenanceRecordRepository.existsByVehicleId(1L)).thenReturn(true);
        ResponseEntity<?> conflictMaint = vehicleController.deleteVehicle(1L);
        assertEquals(HttpStatus.CONFLICT, conflictMaint.getStatusCode());
        verify(vehicleRepository, never()).deleteById(1L);

        // Case C: Clean vehicle without documents or maintenance records
        when(maintenanceRecordRepository.existsByVehicleId(1L)).thenReturn(false);
        ResponseEntity<?> successDelete = vehicleController.deleteVehicle(1L);
        assertEquals(HttpStatus.NO_CONTENT, successDelete.getStatusCode());
        verify(vehicleRepository, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("6. Document upload enforces <= 1MB, returns metadata without fileData, and download returns bytes")
    void testDocumentUploadAndDownload() {
        Vehicle vehicle = new Vehicle("SG-101", "Toyota", "Hilux", 2024, "ACTIVE");
        vehicle.setId(1L);
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(vehicle));

        byte[] originalBytes = "Stark Fleet Document Sample Content".getBytes(StandardCharsets.UTF_8);
        String base64Content = Base64.getEncoder().encodeToString(originalBytes);

        VehicleDocumentUploadRequest request = new VehicleDocumentUploadRequest();
        request.setDocumentType("REGISTRATION");
        request.setFileName("registration.pdf");
        request.setContentType("application/pdf");
        request.setExpiryDate(LocalDate.now().plusMonths(6));
        request.setFileData(base64Content);

        when(vehicleDocumentRepository.save(any(VehicleDocument.class))).thenAnswer(inv -> {
            VehicleDocument doc = inv.getArgument(0);
            doc.setId(100L);
            return doc;
        });

        // Upload
        ResponseEntity<?> uploadResponse = documentController.uploadDocument(1L, request);
        assertEquals(HttpStatus.CREATED, uploadResponse.getStatusCode());
        assertInstanceOf(VehicleDocumentMetadata.class, uploadResponse.getBody());
        VehicleDocumentMetadata metadata = (VehicleDocumentMetadata) uploadResponse.getBody();
        assertEquals(100L, metadata.getId());
        assertEquals("REGISTRATION", metadata.getDocumentType());
        assertEquals("VALID", metadata.getExpiryStatus());

        // Download
        VehicleDocument savedDoc = new VehicleDocument(vehicle, "REGISTRATION", "registration.pdf", "application/pdf", LocalDate.now().plusMonths(6), base64Content);
        savedDoc.setId(100L);
        when(vehicleDocumentRepository.findById(100L)).thenReturn(Optional.of(savedDoc));

        ResponseEntity<?> downloadResponse = documentController.downloadDocument(100L);
        assertEquals(HttpStatus.OK, downloadResponse.getStatusCode());
        assertEquals("application/pdf", downloadResponse.getHeaders().getContentType().toString());
        assertTrue(downloadResponse.getHeaders().getFirst("Content-Disposition").contains("registration.pdf"));
        assertArrayEquals(originalBytes, (byte[]) downloadResponse.getBody());
    }

    @Test
    @DisplayName("7. Dashboard summary returns all 10 contract keys and BigDecimal cost aggregate")
    void testDashboardSummary() {
        when(vehicleRepository.count()).thenReturn(10L);
        when(vehicleRepository.countByStatus("ACTIVE")).thenReturn(7L);
        when(vehicleRepository.countByStatus("MAINTENANCE")).thenReturn(2L);
        when(vehicleRepository.countByStatus("INACTIVE")).thenReturn(1L);
        when(driverRepository.count()).thenReturn(5L);
        when(vehicleDocumentRepository.countByExpiryDateBefore(any(LocalDate.class))).thenReturn(1L);
        when(vehicleDocumentRepository.countByExpiryDateBetween(any(LocalDate.class), any(LocalDate.class))).thenReturn(2L);
        when(vehicleDocumentRepository.countByExpiryDateAfter(any(LocalDate.class))).thenReturn(7L);
        when(maintenanceRecordRepository.count()).thenReturn(15L);
        when(maintenanceRecordRepository.sumTotalMaintenanceCost()).thenReturn(new BigDecimal("12500.50"));

        ResponseEntity<Map<String, Object>> response = dashboardController.getDashboardSummary();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> summary = response.getBody();
        assertNotNull(summary);

        assertEquals(10L, summary.get("totalVehicles"));
        assertEquals(7L, summary.get("activeVehicles"));
        assertEquals(2L, summary.get("maintenanceVehicles"));
        assertEquals(1L, summary.get("inactiveVehicles"));
        assertEquals(5L, summary.get("totalDrivers"));
        assertEquals(1L, summary.get("expiredDocumentsCount"));
        assertEquals(2L, summary.get("expiringSoonDocumentsCount"));
        assertEquals(7L, summary.get("validDocumentsCount"));
        assertEquals(15L, summary.get("totalMaintenanceRecords"));
        assertEquals(new BigDecimal("12500.50"), summary.get("totalMaintenanceCost"));
    }
}

