package com.medilink;

import com.medilink.connector.*;
import com.medilink.model.*;
import com.medilink.repository.*;
import com.medilink.service.*;
import com.medilink.ui.MainFrame;
import com.medilink.util.PasswordHasher;

import javax.swing.SwingUtilities;
import java.time.LocalDate;

public class Main {
    public static void main(String[] args) {
        // ----- infrastructure -----
        UserRepository users = new InMemoryUserRepository();
        RecordRepository records = new InMemoryRecordRepository();
        HospitalConnectorRegistry registry = new HospitalConnectorRegistry();
        ApolloConnector apollo = new ApolloConnector();
        MedTrustConnector medTrust = new MedTrustConnector();
        GenericHL7Connector hl7 = new GenericHL7Connector();
        registry.register(apollo.getHospital().getHospitalId(), apollo);
        registry.register(medTrust.getHospital().getHospitalId(), medTrust);
        registry.register(hl7.getHospital().getHospitalId(), hl7);

        // ----- services -----
        AuditService audit = new AuditService(new InMemoryAuditRepository());
        NotificationService notifications = new NotificationService();
        UserService userService = new UserService(users, audit);
        AuthenticationService auth = new AuthenticationService(users, audit, userService);
        RecordService recordService = new RecordService(records, users, registry, audit,
                new DrugInteractionService(), notifications);

        // ----- demo accounts -----
        Doctor meera = new Doctor("D1", "Dr. Meera Nair", "meera@apollo.test",
                PasswordHasher.hash("doctor123"), "KL-12345", "Cardiology");
        Doctor ravi = new Doctor("D2", "Dr. Ravi Menon", "ravi@medtrust.test",
                PasswordHasher.hash("doctor123"), "KL-67890", "Neurology");
        Patient arun = new Patient("P1", "Arun Das", "arun@mail.test",
                PasswordHasher.hash("patient123"), LocalDate.of(2001, 5, 14), "B+", "StarHealth-7788");
        Patient riya = new Patient("P2", "Riya Paul", "riya@mail.test",
                PasswordHasher.hash("patient123"), LocalDate.of(1999, 2, 3), "O+", "None");
        Admin admin = new Admin("A1", "Admin One", "admin@medilink.test",
                PasswordHasher.hash("admin123"));
        for (User u : new User[]{meera, ravi, arun, riya, admin}) users.save(u);

        meera.addPatientUnderCare(arun);
        arun.addAllergy(new Allergy("Penicillin", "Severe"));
        riya.addAllergy(new Allergy("NSAID", "Moderate"));

        // ----- demo records (Arun: three sources; Riya: two) -----
        MedicalRecord r1 = new MedicalRecord("R1", "P1");                       // local
        r1.addDiagnosis(new Diagnosis("Hypertension", "I10"));
        r1.addPrescription(new Prescription("Amlodipine", "5 mg"));
        r1.addTestResult(new TestResult("Blood pressure", "150/95"));
        records.save(r1);

        MedicalRecord r2 = new MedicalRecord("R2", "P1", "APOLLO");
        r2.addDiagnosis(new Diagnosis("Atrial fibrillation", "I48"));
        r2.addPrescription(new Prescription("Warfarin", "5 mg"));
        r2.addTestResult(new TestResult("INR", "2.4"));
        r2.appendHandoffNote("Dr. Apollo Cardio: keep INR between 2 and 3");
        apollo.pushRecord(r2);

        MedicalRecord r3 = new MedicalRecord("R3", "P1", "MEDTRUST");
        r3.addDiagnosis(new Diagnosis("Type 2 diabetes", "E11"));
        r3.addPrescription(new Prescription("Metformin", "500 mg"));
        r3.addTestResult(new TestResult("HbA1c", "7.2%"));
        medTrust.pushRecord(r3);

        MedicalRecord r4 = new MedicalRecord("R4", "P2", "HL7");
        r4.addDiagnosis(new Diagnosis("Asthma", "J45"));
        r4.addPrescription(new Prescription("Salbutamol", "100 mcg"));
        hl7.pushRecord(r4);

        MedicalRecord r5 = new MedicalRecord("R5", "P2");                       // local
        r5.addDiagnosis(new Diagnosis("Migraine", "G43"));
        r5.addTestResult(new TestResult("MRI brain", "Normal"));
        records.save(r5);

        MedicalRecord r6 = new MedicalRecord("R6", "P1");                       // empty visit, no diagnosis
        records.save(r6);

        // Swing must be built on the Event Dispatch Thread
        SwingUtilities.invokeLater(() ->
                new MainFrame(auth, recordService, audit, notifications, userService).setVisible(true));
    }
}