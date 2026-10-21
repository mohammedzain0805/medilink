package com.medilink.ui;

import com.medilink.model.*;
import com.medilink.service.RecordService;
import com.medilink.service.UserService;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class RecordViewDialog extends JDialog {
    private final User viewer;
    private final MedicalRecord record;
    private final RecordService records;
    private final UserService userService;
    private final JTextArea text = new JTextArea();

    private void pushToHospital(Doctor doctor) {
        List<Hospital> hospitals = records.listHospitals();
        if (hospitals.isEmpty()) return;
        Object choice = JOptionPane.showInputDialog(this, "Send this record to:", "Send to hospital",
                JOptionPane.QUESTION_MESSAGE, null, hospitals.toArray(), hospitals.get(0));
        if (!(choice instanceof Hospital hospital)) return;

        runAction(() -> {
            boolean ok = records.pushRecord(doctor, record, hospital.getHospitalId());
            JOptionPane.showMessageDialog(this, ok
                    ? "Record accepted by " + hospital.getName()
                    : hospital.getName() + " did not accept the record (offline, or it rejected it).");
        });
    }

    public RecordViewDialog(Window owner, User viewer, MedicalRecord record,
                            RecordService records, UserService userService) {
        super(owner, "Record " + record.getRecordId(), ModalityType.APPLICATION_MODAL);
        this.viewer = viewer;
        this.record = record;
        this.records = records;
        this.userService = userService;

        text.setEditable(false);
        text.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        text.setMargin(new Insets(8, 8, 8, 8));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 8));
        if (viewer instanceof Doctor doctor) {
            JButton noteBtn = new JButton("Add handoff note");
            JButton rxBtn = new JButton("Prescribe");
            JButton opinionBtn = new JButton("Request second opinion");
            noteBtn.addActionListener(e -> addNote(doctor));
            rxBtn.addActionListener(e -> prescribe(doctor));
            opinionBtn.addActionListener(e -> requestOpinion(doctor));
            buttons.add(noteBtn);
            buttons.add(rxBtn);
            buttons.add(opinionBtn);
            JButton pushBtn = new JButton("Send to hospital");
            pushBtn.addActionListener(e -> pushToHospital(doctor));
            buttons.add(pushBtn);
        } else {
            JButton historyBtn = new JButton("Who accessed this record?");
            historyBtn.addActionListener(e -> showAccessHistory());
            buttons.add(historyBtn);
        }
        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> dispose());
        buttons.add(closeBtn);

        add(new JScrollPane(text), BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        refresh();
        setSize(660, 560);
        setLocationRelativeTo(owner);
    }

    private void refresh() {
        StringBuilder sb = new StringBuilder();
        if (viewer instanceof Doctor d && d.isEmergencyOnly(record))
            sb.append("*** EMERGENCY ACCESS: every action is audited ***\n\n");

        sb.append("Record:   ").append(record.getRecordId()).append('\n');
        sb.append("Patient:  ").append(record.getPatientId()).append('\n');
        sb.append("Hospital: ").append(record.getHospitalId()).append('\n');
        sb.append("Date:     ").append(record.getTimestamp().toLocalDate()).append("\n\n");

        section(sb, "ALLERGIES", records.getAllergies(viewer, record).stream()
                .map(a -> a.getSubstance() + " (" + a.getSeverity() + ")").toList());
        section(sb, "DIAGNOSES / CHRONIC CONDITIONS", record.getDiagnoses().stream()
                .map(d -> d.getCondition() + " [" + d.getIcdCode() + "]").toList());
        section(sb, "PRESCRIPTIONS", record.getPrescriptions().stream()
                .map(p -> p.getDrugName() + " " + p.getDosage()).toList());
        section(sb, "TEST RESULTS", record.getTestResults().stream()
                .map(t -> t.getTestName() + ": " + t.getResult()).toList());

        sb.append("HANDOFF NOTES\n");
        sb.append(record.getHandoffNotes().isEmpty() ? "  none" : record.getHandoffNotes()).append('\n');

        text.setText(sb.toString());
        text.setCaretPosition(0);
    }

    private static void section(StringBuilder sb, String title, List<String> lines) {
        sb.append(title).append('\n');
        if (lines.isEmpty()) sb.append("  none\n");
        for (String l : lines) sb.append("  ").append(l).append('\n');
        sb.append('\n');
    }

    // Runs a service call, shows access errors nicely, then redraws the record.
    private void runAction(Runnable action) {
        try {
            action.run();
            refresh();
        } catch (SecurityException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Not allowed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void addNote(Doctor doctor) {
        String note = JOptionPane.showInputDialog(this, "Handoff note:");
        if (note == null || note.isBlank()) return;
        runAction(() -> doctor.addHandoffNote(records, record, note.trim()));
    }

    private void prescribe(Doctor doctor) {
        String drug = JOptionPane.showInputDialog(this, "Drug name:");
        if (drug == null || drug.isBlank()) return;
        String dose = JOptionPane.showInputDialog(this, "Dosage (e.g. 5 mg):");
        if (dose == null || dose.isBlank()) return;
        Prescription rx = new Prescription(drug.trim(), dose.trim());

        runAction(() -> {
            List<DrugAlert> alerts = records.prescribe(doctor, record, rx, false);
            if (alerts.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Prescription saved.");
                return;
            }
            StringBuilder sb = new StringBuilder("Safety alerts for " + rx.getDrugName() + ":\n\n");
            for (DrugAlert a : alerts) sb.append("  - ").append(a).append('\n');
            sb.append("\nPrescribe anyway? (An override is recorded in the audit log.)");
            int choice = JOptionPane.showConfirmDialog(this, sb.toString(), "Drug safety alert",
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (choice == JOptionPane.YES_OPTION) records.prescribe(doctor, record, rx, true);
        });
    }

    private void requestOpinion(Doctor doctor) {
        List<Doctor> colleagues = userService.listDoctors(doctor);
        if (colleagues.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No other doctors are registered.");
            return;
        }
        Object choice = JOptionPane.showInputDialog(this, "Choose a colleague:", "Second opinion",
                JOptionPane.QUESTION_MESSAGE, null, colleagues.toArray(), colleagues.get(0));
        if (!(choice instanceof Doctor colleague)) return;

        String question = JOptionPane.showInputDialog(this,
                "Your question for " + colleague.getFullName() + ":");
        if (question == null || question.isBlank()) return;
        runAction(() -> doctor.requestSecondOpinion(records, record, colleague, question.trim()));
    }

    private void showAccessHistory() {
        List<AuditLog> logs = records.getAccessHistory(viewer, record);
        StringBuilder sb = new StringBuilder();
        if (logs.isEmpty()) sb.append("No activity recorded for this record.");
        for (AuditLog l : logs)
            sb.append(l.getTimestamp().withNano(0)).append("   ")
                    .append(userService.nameOf(l.getActorId())).append("   ")
                    .append(l.getAction()).append('\n');
        JTextArea area = new JTextArea(sb.toString(), 14, 64);
        area.setEditable(false);
        JOptionPane.showMessageDialog(this, new JScrollPane(area),
                "Who accessed record " + record.getRecordId(), JOptionPane.INFORMATION_MESSAGE);
    }
}