package io.github.justhurky.groundsegment.groundstation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "ground_station")
public class GroundStation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 16)
    private String code;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(name = "latitude_degrees", nullable = false)
    private double latitudeDegrees;

    @Column(name = "longitude_degrees", nullable = false)
    private double longitudeDegrees;

    @Column(name = "altitude_metres", nullable = false)
    private double altitudeMetres;

    @Column(nullable = false)
    private boolean active;

    protected GroundStation() {
    }

    public GroundStation(String code, String name, double latitudeDegrees, double longitudeDegrees,
                         double altitudeMetres) {
        this.code = code;
        this.name = name;
        this.latitudeDegrees = latitudeDegrees;
        this.longitudeDegrees = longitudeDegrees;
        this.altitudeMetres = altitudeMetres;
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    public UUID getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public double getLatitudeDegrees() {
        return latitudeDegrees;
    }

    public double getLongitudeDegrees() {
        return longitudeDegrees;
    }

    public double getAltitudeMetres() {
        return altitudeMetres;
    }

    public boolean isActive() {
        return active;
    }
}
