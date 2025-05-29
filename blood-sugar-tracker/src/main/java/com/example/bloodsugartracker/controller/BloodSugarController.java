package com.example.bloodsugartracker.controller;

import com.example.bloodsugartracker.model.BloodSugarRecord;
import com.example.bloodsugartracker.service.BloodSugarService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

@Controller
public class BloodSugarController {
    private static final Logger LOGGER = Logger.getLogger(BloodSugarController.class.getName());

    @Autowired
    private BloodSugarService service;

    @GetMapping("/")
    public String showIntroductionPage(Model model) {
        LOGGER.info("Serving introduction page");
        try {
            // Optional: Add any model attributes needed for the introduction page
            return "index";
        } catch (Exception e) {
            LOGGER.severe("Error serving introduction page: " + e.getMessage());
            model.addAttribute("error", "Failed to load introduction page: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/records")
    public String getAllRecords(Model model) {
        LOGGER.info("Fetching all records for records page");
        try {
            addTestDataIfNeeded();
            List<BloodSugarRecord> records = service.getAllRecords();
            LOGGER.info("Fetched " + (records != null ? records.size() : 0) + " records for records page: " + records);
            model.addAttribute("records", records != null ? records : new ArrayList<>());
            return "records";
        } catch (Exception e) {
            LOGGER.severe("Error fetching records for records page: " + e.getMessage());
            model.addAttribute("error", "Failed to load records: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/dashboards")
    public String getDashboards(Model model) {
        LOGGER.info("Fetching dashboards data at " + java.time.LocalDateTime.now());
        try {
            addTestDataIfNeeded();
            List<String> patientNames = service.getAllPatientNames();
            LOGGER.info("Fetched " + (patientNames != null ? patientNames.size() : 0) + " patient names: " + patientNames);
            if (patientNames == null || patientNames.isEmpty()) {
                LOGGER.warning("No patient names found");
                model.addAttribute("patientNames", new ArrayList<String>());
                model.addAttribute("patientDashboards", new HashMap<String, Map<String, List<BloodSugarRecord>>>());
                model.addAttribute("periods", Arrays.asList("today", "yesterday", "week", "month", "year"));
                model.addAttribute("error", "No patient data available. Please add records.");
                return "dashboards";
            }

            Map<String, Map<String, List<BloodSugarRecord>>> patientDashboards = new HashMap<>();
            List<String> periods = Arrays.asList("today", "yesterday", "week", "month", "year");

            for (String patientName : patientNames) {
                if (patientName == null || patientName.trim().isEmpty()) {
                    LOGGER.warning("Skipping null or empty patient name");
                    continue;
                }
                Map<String, List<BloodSugarRecord>> timePeriodRecords = new HashMap<>();
                for (String period : periods) {
                    List<BloodSugarRecord> records = service.getRecordsByPatientAndTimePeriod(patientName, period);
                    timePeriodRecords.put(period, records != null ? records : new ArrayList<>());
                    LOGGER.info("Patient: " + patientName + ", Period: " + period + ", Records: " + (records != null ? records.size() : 0));
                }
                patientDashboards.put(patientName, timePeriodRecords);
            }

            LOGGER.info("Patient dashboards: " + patientDashboards);
            model.addAttribute("patientNames", patientNames);
            model.addAttribute("patientDashboards", patientDashboards);
            model.addAttribute("periods", periods);
            LOGGER.info("Passing to template: " + patientDashboards.size() + " patient dashboards");
            return "dashboards";
        } catch (Exception e) {
            LOGGER.severe("Error fetching dashboards: " + e.getMessage());
            model.addAttribute("error", "Failed to load dashboards: " + e.getMessage());
            return "error";
        }
    }

    @Transactional
    public void addTestDataIfNeeded() {
        List<BloodSugarRecord> records = service.getAllRecords();
        LOGGER.info("Checking existing records: " + (records != null ? records.size() : 0) + " records found: " + records);
        if (records == null || records.isEmpty()) {
            LOGGER.warning("No records found in database. Adding test data...");
            try {
                LocalDate today = LocalDate.now(java.time.ZoneId.of("Asia/Kolkata"));
                BloodSugarRecord testRecord1 = new BloodSugarRecord();
                testRecord1.setPatientName("John Doe");
                testRecord1.setDate(today);
                testRecord1.setFastingSugarValue(124);
                testRecord1.setFastingSampleTime(LocalTime.parse("08:00"));
                testRecord1.setPostPrandialSugarValue(210);
                testRecord1.setPostPrandialSampleTime(LocalTime.parse("10:00"));
                service.saveRecord(testRecord1);

                BloodSugarRecord testRecord2 = new BloodSugarRecord();
                testRecord2.setPatientName("M.Ventaka Subbama");
                testRecord2.setDate(today.minusDays(1));
                testRecord2.setFastingSugarValue(130);
                testRecord2.setFastingSampleTime(LocalTime.parse("07:30"));
                testRecord2.setPostPrandialSugarValue(200);
                testRecord2.setPostPrandialSampleTime(LocalTime.parse("09:30"));
                service.saveRecord(testRecord2);

                LOGGER.info("Successfully added test records for 'John Doe' (today) and 'M.Ventaka Subbama' (yesterday)");
                List<BloodSugarRecord> allRecords = service.getAllRecords();
                LOGGER.info("After adding test data, database contains " + (allRecords != null ? allRecords.size() : 0) + " records: " + allRecords);
            } catch (Exception e) {
                LOGGER.severe("Failed to add test record: " + e.getMessage());
                throw e;
            }
        }
    }

    @GetMapping("/add")
    public String showAddForm(Model model) {
        LOGGER.info("Showing add record form");
        model.addAttribute("record", new BloodSugarRecord());
        return "add-record";
    }

    @PostMapping("/add")
    public String createRecord(@ModelAttribute("record") BloodSugarRecord record) {
        LOGGER.info("Creating record: " + record);
        try {
            service.saveRecord(record);
            LOGGER.info("Record created successfully");
            return "redirect:/records";
        } catch (Exception e) {
            LOGGER.severe("Error creating record: " + e.getMessage());
            throw e;
        }
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        LOGGER.info("Fetching record with ID " + id + " for editing");
        try {
            Optional<BloodSugarRecord> record = service.getRecordById(id);
            if (record.isPresent()) {
                model.addAttribute("record", record.get());
                return "edit-record";
            } else {
                LOGGER.warning("Record not found with ID: " + id);
                return "redirect:/records";
            }
        } catch (Exception e) {
            LOGGER.severe("Error fetching record with ID " + id + " for editing: " + e.getMessage());
            throw e;
        }
    }

    @PostMapping("/edit/{id}")
    public String updateRecord(@PathVariable Long id, @ModelAttribute("record") BloodSugarRecord record) {
        LOGGER.info("Updating record with ID: " + id);
        try {
            Optional<BloodSugarRecord> existing = service.getRecordById(id);
            if (existing.isPresent()) {
                record.setId(id);
                service.saveRecord(record);
                LOGGER.info("Record updated with ID: " + id);
                return "redirect:/records";
            }
            LOGGER.warning("Record not found with ID: " + id);
            return "redirect:/records";
        } catch (Exception e) {
            LOGGER.severe("Error updating record with ID " + id + ": " + e.getMessage());
            throw e;
        }
    }

    @PostMapping("/delete/{id}")
    public String deleteRecord(@PathVariable Long id) {
        LOGGER.info("Deleting record with ID: " + id);
        try {
            if (service.getRecordById(id).isPresent()) {
                service.deleteRecord(id);
                LOGGER.info("Record deleted with ID: " + id);
                return "redirect:/records";
            }
            LOGGER.warning("Record not found with ID: " + id);
            return "redirect:/records";
        } catch (Exception e) {
            LOGGER.severe("Error deleting record with ID " + id + ": " + e.getMessage());
            throw e;
        }
    }
}