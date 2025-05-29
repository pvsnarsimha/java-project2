package com.example.bloodsugartracker.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "blood_sugar_record")
public class BloodSugarRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "patient_name")
    private String patientName;

    @Column(name = "date")
    private LocalDate date;

    @Column(name = "fasting_sugar_value")
    private Integer fastingSugarValue;

    @Column(name = "fasting_sample_time")
    private LocalTime fastingSampleTime;

    @Column(name = "post_prandial_sugar_value")
    private Integer postPrandialSugarValue;

    @Column(name = "post_prandial_sample_time")
    private LocalTime postPrandialSampleTime;

    @Column(name = "predicted_hba1c")
    private Double predictedHbA1c;

    @Column(name = "hba1c_color")
    private String hbA1cColor;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public Integer getFastingSugarValue() {
        return fastingSugarValue;
    }

    public void setFastingSugarValue(Integer fastingSugarValue) {
        this.fastingSugarValue = fastingSugarValue;
    }

    public LocalTime getFastingSampleTime() {
        return fastingSampleTime;
    }

    public void setFastingSampleTime(LocalTime fastingSampleTime) {
        this.fastingSampleTime = fastingSampleTime;
    }

    public Integer getPostPrandialSugarValue() {
        return postPrandialSugarValue;
    }

    public void setPostPrandialSugarValue(Integer postPrandialSugarValue) {
        this.postPrandialSugarValue = postPrandialSugarValue;
    }

    public LocalTime getPostPrandialSampleTime() {
        return postPrandialSampleTime;
    }

    public void setPostPrandialSampleTime(LocalTime postPrandialSampleTime) {
        this.postPrandialSampleTime = postPrandialSampleTime;
    }

    public Double getPredictedHbA1c() {
        return predictedHbA1c;
    }

    public void setPredictedHbA1c(Double predictedHbA1c) {
        this.predictedHbA1c = predictedHbA1c;
    }

    public String getHbA1cColor() {
        return hbA1cColor;
    }

    public void setHbA1cColor(String hbA1cColor) {
        this.hbA1cColor = hbA1cColor;
    }

    @Override
    public String toString() {
        return "BloodSugarRecord{" +
                "id=" + id +
                ", patientName='" + patientName + '\'' +
                ", date=" + date +
                ", fastingSugarValue=" + fastingSugarValue +
                ", fastingSampleTime=" + fastingSampleTime +
                ", postPrandialSugarValue=" + postPrandialSugarValue +
                ", postPrandialSampleTime=" + postPrandialSampleTime +
                ", predictedHbA1c=" + predictedHbA1c +
                ", hbA1cColor='" + hbA1cColor + '\'' +
                '}';
    }
}