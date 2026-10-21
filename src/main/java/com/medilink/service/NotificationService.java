package com.medilink.service;

import com.medilink.model.*;
import java.time.LocalDateTime;
import java.util.*;

public class NotificationService {
    private final Map<String, List<String>> inbox = new HashMap<>();

    public void sendMedicationReminder(Patient patient, Prescription prescription) {
        push(patient, "Reminder: take " + prescription.getDrugName() + " " + prescription.getDosage());
    }

    public void alertDrugInteraction(Doctor doctor, List<DrugAlert> alerts) {
        for (DrugAlert a : alerts) push(doctor, "DRUG ALERT " + a);
    }

    public void notifyUser(User user, String message) { push(user, message); }

    public List<String> getNotifications(User user) {
        return Collections.unmodifiableList(inbox.getOrDefault(user.getUserId(), List.of()));
    }

    private void push(User user, String message) {
        inbox.computeIfAbsent(user.getUserId(), k -> new ArrayList<>())
                .add(LocalDateTime.now().withNano(0) + " - " + message);
    }
}