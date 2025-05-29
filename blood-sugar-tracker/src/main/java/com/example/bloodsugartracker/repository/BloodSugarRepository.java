package com.example.bloodsugartracker.repository;

import com.example.bloodsugartracker.model.BloodSugarRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.logging.Logger;

public interface BloodSugarRepository extends JpaRepository<BloodSugarRecord, Long> {
    Logger LOGGER = Logger.getLogger(BloodSugarRepository.class.getName());

    @Query("SELECT r FROM BloodSugarRecord r WHERE r.patientName = :patientName AND r.date >= :startDate")
    List<BloodSugarRecord> findByPatientNameAndDateAfterInclusive(@Param("patientName") String patientName, @Param("startDate") LocalDate startDate);

    @Query("SELECT r FROM BloodSugarRecord r WHERE r.patientName = :patientName AND r.date = :date")
    List<BloodSugarRecord> findByPatientNameAndDate(@Param("patientName") String patientName, @Param("date") LocalDate date);
}