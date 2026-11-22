package com.upendra.dispatch.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.springframework.data.domain.Persistable;

/**
 * A delivery order. orderId is the natural primary key, so posting the same
 * orderId again updates the existing row instead of creating a duplicate.
 *
 * Implements Persistable so brand-new orders are INSERTed directly (batched)
 * instead of Spring Data doing a SELECT per row to decide insert vs update.
 */
@Entity
@Table(name = "orders")
public class Order implements Persistable<String> {

    @Id
    @Column(name = "order_id", length = 50)
    private String orderId;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    @Column(nullable = false)
    private String address;

    @Column(name = "package_weight", nullable = false)
    private double packageWeight;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Priority priority;

    @Transient
    private boolean isNew = true;

    protected Order() {
        // required by JPA
    }

    public Order(String orderId, double latitude, double longitude, String address,
                 double packageWeight, Priority priority) {
        this.orderId = orderId;
        updateDetails(latitude, longitude, address, packageWeight, priority);
    }

    public void updateDetails(double latitude, double longitude, String address,
                                    double packageWeight, Priority priority) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
        this.packageWeight = packageWeight;
        this.priority = priority;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNew = false;
    }

    @Override
    public String getId() {
        return orderId;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    public String getOrderId() {
        return orderId;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String getAddress() {
        return address;
    }

    public double getPackageWeight() {
        return packageWeight;
    }

    public Priority getPriority() {
        return priority;
    }
}
