package com.starkgroup.vms.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "maintenance_records")
public class MaintenanceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_id", nullable = false, foreignKey = @ForeignKey(name = "fk_maintenance_records_vehicle"))
    @JsonIgnore
    private Vehicle vehicle;

    @NotNull(message = "Service date is required")
    @Column(name = "service_date", nullable = false)
    private LocalDate serviceDate;

    @NotBlank(message = "Maintenance type is required")
    @Column(name = "maintenance_type", nullable = false, length = 100)
    private String maintenanceType;

    @NotNull(message = "Maintenance cost is required")
    @DecimalMin(value = "0.00", message = "Cost cannot be negative")
    @Digits(integer = 10, fraction = 2, message = "Invalid cost format")
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal cost;

    @NotBlank(message = "Workshop/vendor is required")
    @Column(name = "workshop_vendor", nullable = false, length = 150)
    private String workshopVendor;

    @Column(length = 500)
    private String notes;

    public MaintenanceRecord() {
    }

    public MaintenanceRecord(Vehicle vehicle, LocalDate serviceDate, String maintenanceType, BigDecimal cost, String workshopVendor, String notes) {
        this.vehicle = vehicle;
        this.serviceDate = serviceDate;
        this.maintenanceType = maintenanceType != null ? maintenanceType.trim() : null;
        this.cost = cost;
        this.workshopVendor = workshopVendor != null ? workshopVendor.trim() : null;
        this.notes = notes != null ? notes.trim() : null;
    }

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

    public LocalDate getServiceDate() {
        return serviceDate;
    }

    public void setServiceDate(LocalDate serviceDate) {
        this.serviceDate = serviceDate;
    }

    public String getMaintenanceType() {
        return maintenanceType;
    }

    public void setMaintenanceType(String maintenanceType) {
        this.maintenanceType = maintenanceType != null ? maintenanceType.trim() : null;
    }

    public BigDecimal getCost() {
        return cost;
    }

    public void setCost(BigDecimal cost) {
        this.cost = cost;
    }

    public String getWorkshopVendor() {
        return workshopVendor;
    }

    public void setWorkshopVendor(String workshopVendor) {
        this.workshopVendor = workshopVendor != null ? workshopVendor.trim() : null;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes != null ? notes.trim() : null;
    }
}

