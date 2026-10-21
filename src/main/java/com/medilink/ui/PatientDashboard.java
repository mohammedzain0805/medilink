package com.medilink.ui;

import com.medilink.model.*;
import com.medilink.service.*;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PatientDashboard extends JPanel {
    private final Patient patient;
    private final RecordService records;
    private final NotificationService notifications;
    private final UserService userService;

    private final DefaultTableModel recordModel =
            readOnlyModel("Record", "Hospital", "Date", "Diagnoses", "Prescriptions");
    private final JTable recordTable = new JTable(recordModel);
    private List<MedicalRecord> shown = new ArrayList<>();

    private final DefaultTableModel doctorModel =
            readOnlyModel("Doctor", "Specialization", "Has access");
    private final JTable doctorTable = new JTable(doctorModel);
    private List<Doctor> doctors = new ArrayList<>();

    private final DefaultListModel<Prescription> prescriptionModel = new DefaultListModel<>();
    private final JList<Prescription> prescriptionList = new JList<>(prescriptionModel);
    private final DefaultListModel<Prescription> reminderModel = new DefaultListModel<>();
    private final JList<Prescription> reminderList = new JList<>(reminderModel);
    private final JTextArea notificationArea = new JTextArea();

    public PatientDashboard(Patient patient, RecordService records, NotificationService notifications,
                            UserService userService, Runnable onLogout) {
        this.patient = patient;
        this.records = records;
        this.notifications = notifications;
        this.userService = userService;

        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JLabel title = new JLabel(patient.getFullName() + "  |  Patient");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));
        JButton logoutBtn = new JButton("Log out");
        logoutBtn.addActionListener(e -> onLogout.run());
        JPanel header = new JPanel(new BorderLayout());
        header.add(title, BorderLayout.WEST);
        header.add(logoutBtn, BorderLayout.EAST);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("My records", buildRecordsTab());
        tabs.addTab("Doctor access", buildAccessTab());
        tabs.addTab("Reminders", buildRemindersTab());
        tabs.addChangeListener(e -> refreshNotifications());

        add(header, BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);

        loadRecords();
        loadDoctors();
        loadReminders();
        refreshNotifications();
    }

    // ---------- tab 1: my records ----------

    private JPanel buildRecordsTab() {
        recordTable.setRowHeight(24);
        recordTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        recordTable.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) openSelectedRecord();
            }
        });
        JButton openBtn = new JButton("Open record");
        JButton refreshBtn = new JButton("Refresh");
        openBtn.addActionListener(e -> openSelectedRecord());
        refreshBtn.addActionListener(e -> loadRecords());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.add(openBtn);
        buttons.add(refreshBtn);

        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.add(new JScrollPane(recordTable), BorderLayout.CENTER);
        panel.add(buttons, BorderLayout.SOUTH);
        return panel;
    }

    private void loadRecords() {
        shown = patient.viewOwnRecords(records);     // operation from the class diagram
        recordModel.setRowCount(0);
        for (MedicalRecord r : shown) {
            String diagnoses = r.getDiagnoses().stream()
                    .map(Diagnosis::getCondition).collect(Collectors.joining(", "));
            String drugs = r.getPrescriptions().stream()
                    .map(Prescription::getDrugName).collect(Collectors.joining(", "));
            recordModel.addRow(new Object[]{r.getRecordId(), r.getHospitalId(),
                    r.getTimestamp().toLocalDate(), diagnoses, drugs});
        }
        prescriptionModel.clear();
        for (MedicalRecord r : shown)
            for (Prescription rx : r.getPrescriptions()) prescriptionModel.addElement(rx);
    }

    private void openSelectedRecord() {
        int row = recordTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a record first.");
            return;
        }
        new RecordViewDialog(SwingUtilities.getWindowAncestor(this), patient,
                shown.get(row), records, userService).setVisible(true);
    }

    // ---------- tab 2: doctor access (consent) ----------

    private JPanel buildAccessTab() {
        doctorTable.setRowHeight(24);
        doctorTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JLabel info = new JLabel("<html>Only doctors you allow can open your records. In a genuine emergency "
                + "a doctor can get temporary access; you will be notified and it is logged.</html>");
        JButton grantBtn = new JButton("Grant access");
        JButton revokeBtn = new JButton("Revoke access");
        grantBtn.addActionListener(e -> changeAccess(true));
        revokeBtn.addActionListener(e -> changeAccess(false));
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.add(grantBtn);
        buttons.add(revokeBtn);

        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.add(info, BorderLayout.NORTH);
        panel.add(new JScrollPane(doctorTable), BorderLayout.CENTER);
        panel.add(buttons, BorderLayout.SOUTH);
        return panel;
    }

    private void loadDoctors() {
        doctors = userService.listDoctors(patient);
        doctorModel.setRowCount(0);
        for (Doctor d : doctors)
            doctorModel.addRow(new Object[]{d.getFullName(), d.getSpecialization(),
                    d.getPatientsUnderCare().contains(patient) ? "Yes" : "No"});
    }

    private void changeAccess(boolean grant) {
        int row = doctorTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a doctor first.");
            return;
        }
        Doctor d = doctors.get(row);
        if (grant) {
            int ok = JOptionPane.showConfirmDialog(this,
                    "Allow " + d.getFullName() + " to see ALL your records?",
                    "Grant access", JOptionPane.YES_NO_OPTION);
            if (ok != JOptionPane.YES_OPTION) return;
            records.grantAccess(patient, d);
        } else {
            records.revokeAccess(patient, d);
        }
        loadDoctors();
    }

    // ---------- tab 3: reminders and notifications ----------

    private JPanel buildRemindersTab() {
        JButton setBtn = new JButton("Set reminder for selected");
        setBtn.addActionListener(e -> {
            Prescription rx = prescriptionList.getSelectedValue();
            if (rx == null) return;
            patient.setMedicationReminder(rx);       // operation from the class diagram
            loadReminders();
        });
        JButton sendBtn = new JButton("Send my reminders now");   // stands in for a daily scheduler
        sendBtn.addActionListener(e -> {
            for (Prescription rx : patient.getReminders())
                notifications.sendMedicationReminder(patient, rx);
            refreshNotifications();
        });
        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> refreshNotifications());
        notificationArea.setEditable(false);

        JPanel panel = new JPanel(new GridLayout(3, 1, 8, 8));
        panel.add(block("My prescriptions", new JScrollPane(prescriptionList), setBtn));
        panel.add(block("Active reminders", new JScrollPane(reminderList), sendBtn));
        panel.add(block("Notifications", new JScrollPane(notificationArea), refreshBtn));
        return panel;
    }

    private void loadReminders() {
        reminderModel.clear();
        for (Prescription rx : patient.getReminders()) reminderModel.addElement(rx);
    }

    private void refreshNotifications() {
        List<String> list = notifications.getNotifications(patient);
        notificationArea.setText(list.isEmpty() ? "No notifications." : String.join("\n", list));
    }

    private static JPanel block(String title, JComponent content, JButton button) {
        JPanel p = new JPanel(new BorderLayout(4, 4));
        p.setBorder(BorderFactory.createTitledBorder(title));
        p.add(content, BorderLayout.CENTER);
        JPanel south = new JPanel(new FlowLayout(FlowLayout.LEFT));
        south.add(button);
        p.add(south, BorderLayout.SOUTH);
        return p;
    }

    private static DefaultTableModel readOnlyModel(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
    }
}