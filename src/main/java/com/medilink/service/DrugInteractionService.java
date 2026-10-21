package com.medilink.service;

import com.medilink.model.*;
import java.util.*;

public class DrugInteractionService {
    private final Map<String, DrugAlert> interactions = new HashMap<>();
    private final Map<String, String> drugClasses = new HashMap<>();

    public DrugInteractionService() {
        addInteraction("warfarin", "aspirin", "SEVERE", "Warfarin + aspirin: high risk of serious bleeding");
        addInteraction("sildenafil", "nitroglycerin", "SEVERE", "Sildenafil + nitroglycerin: dangerous drop in blood pressure");
        addInteraction("simvastatin", "clarithromycin", "SEVERE", "Simvastatin + clarithromycin: risk of muscle damage");
        addInteraction("lisinopril", "spironolactone", "MODERATE", "Lisinopril + spironolactone: risk of high potassium");
        addInteraction("ibuprofen", "aspirin", "MODERATE", "Ibuprofen + aspirin: more stomach bleeding, weaker aspirin heart protection");

        drugClasses.put("amoxicillin", "penicillin");
        drugClasses.put("penicillin", "penicillin");
        drugClasses.put("ibuprofen", "nsaid");
        drugClasses.put("aspirin", "nsaid");
        drugClasses.put("diclofenac", "nsaid");
        drugClasses.put("naproxen", "nsaid");
    }

    private void addInteraction(String a, String b, String severity, String message) {
        interactions.put(key(a, b), new DrugAlert(severity, message));
    }

    private static String key(String a, String b) {
        String x = a.trim().toLowerCase(), y = b.trim().toLowerCase();
        return x.compareTo(y) <= 0 ? x + "|" + y : y + "|" + x;   // pair order doesn't matter
    }

    public List<DrugAlert> checkInteractions(List<Prescription> current, Prescription newDrug) {
        List<DrugAlert> alerts = new ArrayList<>();
        for (Prescription p : current) {
            DrugAlert a = interactions.get(key(p.getDrugName(), newDrug.getDrugName()));
            if (a != null && !alerts.contains(a)) alerts.add(a);
        }
        return alerts;
    }

    public List<DrugAlert> checkAllergyConflict(Patient patient, Prescription newDrug) {
        List<DrugAlert> alerts = new ArrayList<>();
        String drug = newDrug.getDrugName().trim().toLowerCase();
        String drugClass = drugClasses.get(drug);
        for (Allergy allergy : patient.getAllergies()) {
            String substance = allergy.getSubstance().trim().toLowerCase();
            if (substance.equals(drug) || substance.equals(drugClass))
                alerts.add(new DrugAlert(allergy.getSeverity().toUpperCase(),
                        "Patient is allergic to " + allergy.getSubstance()
                                + " (" + newDrug.getDrugName() + " conflicts)"));
        }
        return alerts;
    }
}