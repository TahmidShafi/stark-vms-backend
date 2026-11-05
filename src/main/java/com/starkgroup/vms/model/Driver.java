package com.starkgroup.vms.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Entity
@Table(
    name = "drivers",
    uniqueConstraints = @UniqueConstraint(name = "uk_drivers_license_number", columnNames = "license_number")
)
public class Driver {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Driver name is required")
    @Column(nullable = false, length = 100)
    private String name;

    @NotBlank(message = "License number is required")
    @Column(name = "license_number", nullable = false, unique = true, length = 50)
    private String licenseNumber;

    @NotBlank(message = "Phone number is required")
    @Column(nullable = false, length = 30)
    private String phone;

    @NotBlank(message = "Status is required")
    @Pattern(regexp = "^(ACTIVE|INACTIVE)$", message = "Driver status must be ACTIVE or INACTIVE")
    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";

    public Driver() {
    }

    public Driver(String name, String licenseNumber, String phone, String status) {
        this.name = name;
        this.licenseNumber = licenseNumber != null ? licenseNumber.trim().toUpperCase() : null;
        this.phone = phone;
        this.status = status != null ? status.trim().toUpperCase() : null;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public void setLicenseNumber(String licenseNumber) {
        this.licenseNumber = licenseNumber != null ? licenseNumber.trim().toUpperCase() : null;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status != null ? status.trim().toUpperCase() : null;
    }
}

