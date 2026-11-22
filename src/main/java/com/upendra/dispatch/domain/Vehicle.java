package com.upendra.dispatch.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.springframework.data.domain.Persistable;

/**
 * A delivery vehicle. vehicleId is the natural primary key (upsert by id).
 */
@Entity
@Table(name = "vehicles")
public class Vehicle implements Persistable<String> {

    @Id
    @Column(name = "vehicle_id", length = 50)
    private String vehicleId;

    @Column(nullable = false)
    private double capacity;

    @Column(name = "current_latitude", nullable = false)
    private double currentLatitude;

    @Column(name = "current_longitude", nullable = false)
    private double currentLongitude;

    @Column(name = "current_address", nullable = false)
    private String currentAddress;

    @Transient
    private boolean isNew = true;

    protected Vehicle() {
        // required by JPA
    }

    public Vehicle(String vehicleId, double capacity, double currentLatitude,
                   double currentLongitude, String currentAddress) {
        this.vehicleId = vehicleId;
        updateDetails(capacity, currentLatitude, currentLongitude, currentAddress);
    }

    public void updateDetails(double capacity, double currentLatitude,
                                    double currentLongitude, String currentAddress) {
        this.capacity = capacity;
        this.currentLatitude = currentLatitude;
        this.currentLongitude = currentLongitude;
        this.currentAddress = currentAddress;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNew = false;
    }

    @Override
    public String getId() {
        return vehicleId;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    public String getVehicleId() {
        return vehicleId;
    }

    public double getCapacity() {
        return capacity;
    }

    public double getCurrentLatitude() {
        return currentLatitude;
    }

    public double getCurrentLongitude() {
        return currentLongitude;
    }

    public String getCurrentAddress() {
        return currentAddress;
    }
}
