package com.example.bloodsugartracker.service;

import com.example.bloodsugartracker.model.BloodSugarRecord;
import com.example.bloodsugartracker.repository.BloodSugarRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
public class BloodSugarService {
    private static final Logger LOGGER = Logger.getLogger(BloodSugarService.class.getName());

    @Autowired
    private BloodSugarRepository repository;

    @PostConstruct
    public void init() {
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Asia/Kolkata"));
        LOGGER.info("Set default timezone to Asia/Kolkata at " + java.time.LocalDateTime.now());
    }

    public List<BloodSugarRecord> getAllRecords() {
        LOGGER.info("Fetching all records from the database");
        List<BloodSugarRecord> records = repository.findAll();
        // Ensure HbA1c is calculated for existing records
        for (BloodSugarRecord record : records) {
            if (record.getPredictedHbA1c() == null) {
                calculateAndSetHbA1c(record);
            }
        }
        LOGGER.info("Fetched " + (records != null ? records.size() : 0) + " records from database: " + records);
        return records != null ? records : new ArrayList<>();
    }

    public List<BloodSugarRecord> getRecordsByPatientAndTimePeriod(String patientName, String period) {
        if (patientName == null || patientName.trim().isEmpty()) {
            LOGGER.warning("Invalid patient name: " + patientName);
            return new ArrayList<>();
        }
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));
        LOGGER.info("Current date (Asia/Kolkata): " + today + " for patient: " + patientName + ", period: " + period);
        List<BloodSugarRecord> filteredRecords;

        switch (period.toLowerCase()) {
            case "today":
                LOGGER.info("Querying for today: " + today);
                filteredRecords = repository.findByPatientNameAndDate(patientName, today);
                break;
            case "yesterday":
                LocalDate yesterday = today.minusDays(1);
                LOGGER.info("Querying for yesterday: " + yesterday);
                filteredRecords = repository.findByPatientNameAndDate(patientName, yesterday);
                break;
            case "week":
                LocalDate weekStart = today.minusDays(7);
                LOGGER.info("Querying for last week, start date: " + weekStart);
                filteredRecords = repository.findByPatientNameAndDateAfterInclusive(patientName, weekStart);
                break;
            case "month":
                LocalDate monthStart = today.minusDays(30);
                LOGGER.info("Querying for last month, start date: " + monthStart);
                filteredRecords = repository.findByPatientNameAndDateAfterInclusive(patientName, monthStart);
                break;
            case "year":
                LocalDate yearStart = today.minusDays(365);
                LOGGER.info("Querying for last year, start date: " + yearStart);
                filteredRecords = repository.findByPatientNameAndDateAfterInclusive(patientName, yearStart);
                break;
            default:
                LOGGER.warning("Unknown period: " + period + ", returning empty list");
                return new ArrayList<>();
        }

        // Ensure HbA1c is calculated
        for (BloodSugarRecord record : filteredRecords) {
            if (record.getPredictedHbA1c() == null) {
                calculateAndSetHbA1c(record);
                repository.save(record); // Update record in database
            }
        }

        LOGGER.info("Fetched " + (filteredRecords != null ? filteredRecords.size() : 0) + " records for patient: " + patientName + ", period: " + period + ": " + filteredRecords);
        return filteredRecords != null ? filteredRecords : new ArrayList<>();
    }

    public List<String> getAllPatientNames() {
        LOGGER.info("Fetching all patient names");
        List<BloodSugarRecord> allRecords = repository.findAll();
        LOGGER.info("Records retrieved for patient names: " + (allRecords != null ? allRecords.size() : 0) + " records: " + allRecords);
        List<String> patientNames = allRecords.stream()
                .map(BloodSugarRecord::getPatientName)
                .filter(name -> name != null && !name.trim().isEmpty())
                .distinct()
                .collect(Collectors.toList());
        LOGGER.info("Fetched " + patientNames.size() + " unique patient names: " + patientNames);
        return patientNames;
    }

    @Transactional
    public BloodSugarRecord saveRecord(BloodSugarRecord record) {
        LOGGER.info("Saving record: " + record);
        try {
            if (record.getPatientName() == null || record.getPatientName().trim().isEmpty()) {
                LOGGER.severe("Cannot save record with null or empty patient name");
                throw new IllegalArgumentException("Patient name cannot be null or empty");
            }
            // Calculate HbA1c before saving
            calculateAndSetHbA1c(record);
            BloodSugarRecord savedRecord = repository.save(record);
            LOGGER.info("Successfully saved record with ID: " + savedRecord.getId() + ": " + savedRecord);
            // Verify the record is in the database
            List<BloodSugarRecord> allRecordsAfterSave = repository.findAll();
            LOGGER.info("After saving, database contains " + allRecordsAfterSave.size() + " records: " + allRecordsAfterSave);
            return savedRecord;
        } catch (Exception e) {
            LOGGER.severe("Failed to save record: " + e.getMessage());
            throw e;
        }
    }

    public Optional<BloodSugarRecord> getRecordById(Long id) {
        LOGGER.info("Fetching record with ID: " + id);
        Optional<BloodSugarRecord> record = repository.findById(id);
        if (record.isPresent()) {
            if (record.get().getPredictedHbA1c() == null) {
                calculateAndSetHbA1c(record.get());
                repository.save(record.get());
            }
            LOGGER.info("Found record with ID: " + id + ": " + record.get());
        } else {
            LOGGER.warning("No record found with ID: " + id);
        }
        return record;
    }

    @Transactional
    public void deleteRecord(Long id) {
        LOGGER.info("Deleting record with ID: " + id);
        try {
            repository.deleteById(id);
            LOGGER.info("Successfully deleted record with ID: " + id);
        } catch (Exception e) {
            LOGGER.severe("Failed to delete record with ID " + id + ": " + e.getMessage());
            throw e;
        }
    }

    private void calculateAndSetHbA1c(BloodSugarRecord record) {
        if (record.getFastingSugarValue() != null && record.getPostPrandialSugarValue() != null) {
            double averageGlucose = (record.getFastingSugarValue() + record.getPostPrandialSugarValue()) / 2.0;
            double hbA1c = (averageGlucose + 46.7) / 28.7;
            record.setPredictedHbA1c(Math.round(hbA1c * 100.0) / 100.0); // Round to 2 decimal places
            if (hbA1c < 5.7) {
                record.setHbA1cColor("green");
            } else if (hbA1c >= 5.7 && hbA1c <= 6.4) {
                record.setHbA1cColor("yellow");
            } else {
                record.setHbA1cColor("red");
            }
            LOGGER.info("Calculated HbA1c for record ID " + record.getId() + ": " + record.getPredictedHbA1c() + "%, color: " + record.getHbA1cColor());
        } else {
            record.setPredictedHbA1c(null);
            record.setHbA1cColor(null);
            LOGGER.warning("Cannot calculate HbA1c for record ID " + record.getId() + ": missing glucose values");
        }
    }
}