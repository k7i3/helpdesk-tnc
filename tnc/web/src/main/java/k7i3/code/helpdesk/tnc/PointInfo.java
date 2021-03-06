package k7i3.code.helpdesk.tnc;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Date;

/**
 * Created by k7i3 on 13.02.15.
 */
@Entity
public class PointInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotNull
    @AttributeOverrides({
            @AttributeOverride(name="date", column= @Column(name="modification_Date")),
            @AttributeOverride(name="didBy", column= @Column(name="modification_DidBy"))
    })
    @Embedded
    private LifeCycleInfo modification;
    @NotNull
    private Double latitude = 54.720800;
    @NotNull
    private Double longitude = 55.956039;
    private Integer speed = 0;
    @NotNull
    @Temporal(TemporalType.TIMESTAMP)
    private Date date = new Date(0);

    public PointInfo() {
    }

    public PointInfo(LifeCycleInfo modification, Double latitude, Double longitude, Date date) {
        this.modification = modification;
        this.latitude = latitude;
        this.longitude = longitude;
        this.date = date;
    }

    public PointInfo(PointInfo pointInfo) {
        this.modification = pointInfo.getModification();
        this.latitude = pointInfo.getLatitude();
        this.longitude = pointInfo.getLongitude();
        this.speed = pointInfo.getSpeed();
        this.date = pointInfo.getDate();
    }

    public Long getId() {
        return id;
    }

    public LifeCycleInfo getModification() {
        return modification;
    }

    public void setModification(LifeCycleInfo modification) {
        this.modification = modification;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Integer getSpeed() {
        return speed;
    }

    public void setSpeed(Integer speed) {
        this.speed = speed;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }
}
