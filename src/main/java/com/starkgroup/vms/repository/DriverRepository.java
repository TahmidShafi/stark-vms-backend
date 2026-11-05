package com.starkgroup.vms.repository;

import com.starkgroup.vms.model.Driver;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DriverRepository extends JpaRepository<Driver, Long> {
    boolean existsByLicenseNumberIgnoreCase(String licenseNumber);

    boolean existsByLicenseNumberIgnoreCaseAndIdNot(String licenseNumber, Long id);
}

