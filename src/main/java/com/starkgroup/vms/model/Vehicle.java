package com.starkgroup.vms.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "vehicles")
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Vehicle number is required")
    @Column(name = "vehicle_number", nullable = false, unique = true)
    private String vehicleNumber;

    @NotBlank(message = "Brand is required")
    @Column(nullable = false)
    private String brand;

    @NotBlank(message = "Model is required")
    @Column(nullable = false)
    private String model;

    @NotNull(message = "Manufacturing year is required")
    @Column(name = "manufacturing_year", nullable = false)
    private Integer manufacturingYear;

    @NotBlank(message = "Status is required")
    @Column(nullable = false)
    private String status = "ACTIVE";

    public Vehicle() {
    }

    public Vehicle(String vehicleNumber, String brand, String model, Integer manufacturingYear, String status) {
        this.vehicleNumber = vehicleNumber != null ? vehicleNumber.trim().toUpperCase() : null;
        this.brand = brand;
        this.model = model;
        this.manufacturingYear = manufacturingYear;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getVehicleNumber() {
        return vehicleNumber;
    }

    public void setVehicleNumber(String vehicleNumber) {
        this.vehicleNumber = vehicleNumber != null ? vehicleNumber.trim().toUpperCase() : null;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Integer getManufacturingYear() {
        return manufacturingYear;
    }

    public void setManufacturingYear(Integer manufacturingYear) {
        this.manufacturingYear = manufacturingYear;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}