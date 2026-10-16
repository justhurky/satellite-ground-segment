package io.github.justhurky.groundsegment.spacecraft;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "parameter_definition",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_parameter_definition_slot",
                        columnNames = {"spacecraft_id", "parameter_slot"}),
                @UniqueConstraint(name = "uq_parameter_definition_name",
                        columnNames = {"spacecraft_id", "name"})
        })
public class ParameterDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "spacecraft_id", nullable = false)
    private Spacecraft spacecraft;

    @Column(name = "parameter_slot", nullable = false)
    private int parameterSlot;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(nullable = false, length = 16)
    private String unit;

    @Column(name = "red_low")
    private Double redLow;

    @Column(name = "yellow_low")
    private Double yellowLow;

    @Column(name = "yellow_high")
    private Double yellowHigh;

    @Column(name = "red_high")
    private Double redHigh;

    protected ParameterDefinition() {
    }

    public ParameterDefinition(Spacecraft spacecraft, int parameterSlot, String name, String unit,
                               Double redLow, Double yellowLow, Double yellowHigh, Double redHigh) {
        this.spacecraft = spacecraft;
        this.parameterSlot = parameterSlot;
        this.name = name;
        this.unit = unit;
        applyLimits(redLow, yellowLow, yellowHigh, redHigh);
    }

    public void applyLimits(Double redLow, Double yellowLow, Double yellowHigh, Double redHigh) {
        requireAscending(redLow, yellowLow);
        requireAscending(yellowLow, yellowHigh);
        requireAscending(yellowHigh, redHigh);
        requireAscending(redLow, redHigh);
        this.redLow = redLow;
        this.yellowLow = yellowLow;
        this.yellowHigh = yellowHigh;
        this.redHigh = redHigh;
    }

    private void requireAscending(Double lower, Double upper) {
        if (lower != null && upper != null && lower > upper) {
            throw new IllegalArgumentException(
                    "Limit thresholds must ascend from red low to red high, got " + lower + " before " + upper);
        }
    }

    public Long getId() {
        return id;
    }

    public Spacecraft getSpacecraft() {
        return spacecraft;
    }

    public int getParameterSlot() {
        return parameterSlot;
    }

    public String getName() {
        return name;
    }

    public String getUnit() {
        return unit;
    }

    public Double getRedLow() {
        return redLow;
    }

    public Double getYellowLow() {
        return yellowLow;
    }

    public Double getYellowHigh() {
        return yellowHigh;
    }

    public Double getRedHigh() {
        return redHigh;
    }
}
