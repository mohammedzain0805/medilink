package com.medilink.ui;

import com.medilink.model.*;
import com.medilink.service.*;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DoctorDashboard extends JPanel {
    private static final String MODE_ID = "Record ID";
    private static final String MODE_NAME_DOB = "Patient name + DOB";
    private static final String MODE_HOSPITAL = "Hospital + Record ID";

    private final Doctor doctor;
    private final RecordService records;
    private final NotificationService notifications;
    private final UserService userService;

    private final JComboBox<String> modeBox =
            new JComboBox<>(new String[]{MODE_ID, MODE_NAME_DOB, MODE_HOSPITAL});
    private final JLabel labelA = new JLabel();
    private final JLabel labelB = new JLabel();
    private final JTextField fieldA = new JTextField(14);
    private final JTextField fieldB = new JTextField(14);
    private final DefaultListModel<Patient> patientModel = new DefaultListModel<>();
    private final JList<Patient> patientList = new JList<>(patientModel);
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{"Record", "Patient", "Hospital", "Date", "Diagnoses", "Prescriptions"}, 0) {
        @Override public boolean isCellEditable(int row, int col) { return false; }
    };
    private final JTable table = new JTable(tableModel);
    private final JLabel status = new JLabel(" ");
    private List<MedicalRecord> shown = new ArrayList<>();

    private void emergencyAccess() {
        JTextField name = new JTextField(20);
        JTextField dob = new JTextField(20);
        JTextField reason = new JTextField(20);
        JPanel form = new JPanel(new GridLayout(0, 1, 4, 4));
        form.add(new JLabel("<html>EMERGENCY ACCESS is recorded in the audit log<br>"
                + "and the patient is notified.</html>"));
        form.add(new JLabel("Patient name:"));
        form.add(name);
        form.add(new JLabel("Date of birth (yyyy-MM-dd):"));
        form.add(dob);
        form.add(new JLabel("Reason (required):"));
        form.add(reason);

        int ok = JOptionPane.showConfirmDialog(this, form, "Emergency access",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ok != JOptionPane.OK_OPTION) return;

        try {
            List<MedicalRecord> result = records.emergencyAccess(doctor, name.getText().trim(),
                    LocalDate.parse(dob.getText().trim()), reason.getText());
            showRecords(result, "Emergency access granted, but this patient has no records yet.");
            status.setText(status.getText() + "  (EMERGENCY ACCESS, audited)");
        } catch (DateTimeParseException ex) {
            status.setText("DOB must look like 2001-05-14");
        } catch (IllegalArgumentException ex) {
            status.setText(ex.getMessage());
        }
    }

    public DoctorDashboard(Doctor doctor, RecordService records, NotificationService notifications,
                           UserService userService, Runnable onLogout) {
        this.doctor = doctor;
        this.records = records;
        this.notifications = notifications;
        this.userService = userService;

        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        // ----- header -----
        JLabel title = new JLabel(doctor.getFullName() + "  |  " + doctor.getSpecialization());
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));
        JButton emergencyBtn = new JButton("Emergency access");
        emergencyBtn.setForeground(Color.RED);
        JButton notifBtn = new JButton("Notifications");
        JButton logoutBtn = new JButton("Log out");
        JPanel headerButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        headerButtons.add(emergencyBtn);
        headerButtons.add(notifBtn);
        headerButtons.add(logoutBtn);
        JPanel header = new JPanel(new BorderLayout());
        header.add(title, BorderLayout.WEST);
        header.add(headerButtons, BorderLayout.EAST);

        // ----- left: my patients -----
        for (Patient p : doctor.getPatientsUnderCare()) patientModel.addElement(p);
        JButton historyBtn = new JButton("Load full history");
        JPanel left = new JPanel(new BorderLayout(4, 4));
        left.setBorder(BorderFactory.createTitledBorder("My patients"));
        left.add(new JScrollPane(patientList), BorderLayout.CENTER);
        left.add(historyBtn, BorderLayout.SOUTH);
        left.setPreferredSize(new Dimension(200, 0));

        // ----- centre: search + results -----
        JButton searchBtn = new JButton("Search");
        JPanel search = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        search.add(new JLabel("Search by:"));
        search.add(modeBox);
        search.add(labelA);
        search.add(fieldA);
        search.add(labelB);
        search.add(fieldB);
        search.add(searchBtn);

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(24);
        JButton openBtn = new JButton("Open selected record");
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(status, BorderLayout.CENTER);
        bottom.add(openBtn, BorderLayout.EAST);

        JPanel center = new JPanel(new BorderLayout(4, 4));
        center.add(search, BorderLayout.NORTH);
        center.add(new JScrollPane(table), BorderLayout.CENTER);
        center.add(bottom, BorderLayout.SOUTH);

        add(header, BorderLayout.NORTH);
        add(left, BorderLayout.WEST);
        add(center, BorderLayout.CENTER);

        // ----- events -----
        modeBox.addActionListener(e -> updateLabels());
        searchBtn.addActionListener(e -> doSearch());
        fieldA.addActionListener(e -> doSearch());      // Enter key in a field = search
        fieldB.addActionListener(e -> doSearch());
        historyBtn.addActionListener(e -> loadHistory());
        openBtn.addActionListener(e -> openSelected());
        notifBtn.addActionListener(e -> showNotifications());
        emergencyBtn.addActionListener(e -> emergencyAccess());
        logoutBtn.addActionListener(e -> onLogout.run());
        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) openSelected();
            }
        });

        updateLabels();
    }

    private void updateLabels() {
        switch ((String) modeBox.getSelectedItem()) {
            case MODE_ID -> {
                labelA.setText("Record ID:");
                labelB.setText("");
                fieldB.setText("");
                fieldB.setEnabled(false);
            }
            case MODE_NAME_DOB -> {
                labelA.setText("Name:");
                labelB.setText("DOB (yyyy-MM-dd):");
                fieldB.setEnabled(true);
            }
            default -> {
                labelA.setText("Hospital ID:");
                labelB.setText("Record ID:");
                fieldB.setEnabled(true);
            }
        }
    }

    private void doSearch() {
        String mode = (String) modeBox.getSelectedItem();
        String a = fieldA.getText().trim();
        String b = fieldB.getText().trim();
        if (a.isEmpty() || (!MODE_ID.equals(mode) && b.isEmpty())) {
            status.setText("Fill in the search fields.");
            return;
        }
        try {
            // Three different search() overloads, picked by the argument types
            List<MedicalRecord> result = switch (mode) {
                case MODE_ID -> records.search(doctor, a);
                case MODE_NAME_DOB -> records.search(doctor, a, LocalDate.parse(b));
                default -> records.search(doctor, a, b);
            };
            showRecords(result, "No accessible records found. If you don't treat this patient, "
                    + "the attempt has been logged.");
        } catch (DateTimeParseException ex) {
            status.setText("DOB must look like 2001-05-14");
        } catch (IllegalArgumentException ex) {
            status.setText(ex.getMessage());           // e.g. unknown hospital ID
        }
    }

    private void loadHistory() {
        Patient p = patientList.getSelectedValue();
        if (p == null) {
            status.setText("Select a patient first.");
            return;
        }
        showRecords(records.getFullHistory(doctor, p), "No records yet for " + p.getFullName());
    }

    private void showRecords(List<MedicalRecord> list, String emptyMessage) {
        shown = list;
        refreshTable();
        status.setText(list.isEmpty() ? emptyMessage : list.size() + " record(s) shown");
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        for (MedicalRecord r : shown) {
            String diagnoses = r.getDiagnoses().stream()
                    .map(Diagnosis::getCondition).collect(Collectors.joining(", "));
            String drugs = r.getPrescriptions().stream()
                    .map(Prescription::getDrugName).collect(Collectors.joining(", "));
            tableModel.addRow(new Object[]{r.getRecordId(), r.getPatientId(), r.getHospitalId(),
                    r.getTimestamp().toLocalDate(), diagnoses, drugs});
        }
    }

    private void openSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            status.setText("Select a record first.");
            return;
        }
        new RecordViewDialog(SwingUtilities.getWindowAncestor(this), doctor,
                shown.get(row), records, userService).setVisible(true);
        refreshTable();   // pick up any new prescriptions
    }

    private void showNotifications() {
        List<String> list = notifications.getNotifications(doctor);
        JTextArea area = new JTextArea(list.isEmpty() ? "No notifications." : String.join("\n", list), 12, 60);
        area.setEditable(false);
        JOptionPane.showMessageDialog(this, new JScrollPane(area), "Notifications",
                JOptionPane.INFORMATION_MESSAGE);
    }
}