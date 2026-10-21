package com.medilink.ui;

import com.medilink.model.*;
import com.medilink.service.AuditService;
import com.medilink.service.UserService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AdminPanel extends JPanel {
    private final Admin admin;
    private final AuditService audit;
    private final UserService userService;
    private final DefaultTableModel logModel = readOnlyModel("Time", "Actor", "Action", "Record");
    private final DefaultTableModel userModel = readOnlyModel("ID", "Name", "Email", "Role");
    private final JTable logTable = new JTable(logModel);
    private final JTable userTable = new JTable(userModel);

    public AdminPanel(Admin admin, AuditService audit, UserService userService, Runnable onLogout) {
        this.admin = admin;
        this.audit = audit;
        this.userService = userService;

        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JLabel title = new JLabel("Admin panel  |  " + admin.getFullName());
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));
        JButton logoutBtn = new JButton("Log out");
        logoutBtn.addActionListener(e -> onLogout.run());
        JPanel header = new JPanel(new BorderLayout());
        header.add(title, BorderLayout.WEST);
        header.add(logoutBtn, BorderLayout.EAST);

        JButton refreshLogs = new JButton("Refresh");
        refreshLogs.addActionListener(e -> loadLogs());
        JButton addBtn = new JButton("Add user");
        addBtn.addActionListener(e -> addUser());
        JButton removeBtn = new JButton("Remove selected");
        removeBtn.addActionListener(e -> removeSelectedUser());
        JButton refreshUsers = new JButton("Refresh");
        refreshUsers.addActionListener(e -> loadUsers());

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Audit logs", tabContent(logTable, refreshLogs));
        tabs.addTab("Users", tabContent(userTable, addBtn, removeBtn, refreshUsers));

        add(header, BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);

        loadLogs();
        loadUsers();
    }

    private void loadLogs() {
        List<AuditLog> logs = new ArrayList<>(admin.viewAuditLogs(audit));   // diagram operation
        Collections.reverse(logs);   // newest first
        logModel.setRowCount(0);
        for (AuditLog l : logs)
            logModel.addRow(new Object[]{l.getTimestamp().withNano(0), l.getActorId(), l.getAction(),
                    l.getTargetRecordId() == null ? "" : l.getTargetRecordId()});
    }

    private void loadUsers() {
        userModel.setRowCount(0);
        for (User u : admin.manageUserAccounts(userService))                 // diagram operation
            userModel.addRow(new Object[]{u.getUserId(), u.getFullName(), u.getEmail(), u.getRole()});
    }

    private void addUser() {
        JComboBox<Role> roleBox = new JComboBox<>(Role.values());
        JTextField name = new JTextField(20);
        JTextField email = new JTextField(20);
        JPasswordField password = new JPasswordField(20);
        JLabel label1 = new JLabel();
        JLabel label2 = new JLabel();
        JTextField extra1 = new JTextField(20);
        JTextField extra2 = new JTextField(20);

        Runnable updateLabels = () -> {
            Role r = (Role) roleBox.getSelectedItem();
            boolean doctor = r == Role.DOCTOR;
            boolean patient = r == Role.PATIENT;
            label1.setText(doctor ? "License number:" : patient ? "Date of birth (yyyy-MM-dd):" : " ");
            label2.setText(doctor ? "Specialization:" : patient ? "Blood group:" : " ");
            extra1.setEnabled(doctor || patient);
            extra2.setEnabled(doctor || patient);
        };
        roleBox.addActionListener(e -> updateLabels.run());
        updateLabels.run();

        JPanel form = new JPanel(new GridLayout(0, 1, 4, 4));
        form.add(new JLabel("Role (cannot be changed later):"));
        form.add(roleBox);
        form.add(new JLabel("Full name:"));
        form.add(name);
        form.add(new JLabel("Email:"));
        form.add(email);
        form.add(new JLabel("Password (min 6 characters):"));
        form.add(password);
        form.add(label1);
        form.add(extra1);
        form.add(label2);
        form.add(extra2);

        int ok = JOptionPane.showConfirmDialog(this, form, "Add user",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (ok != JOptionPane.OK_OPTION) return;

        try {
            Role role = (Role) roleBox.getSelectedItem();
            String pw = new String(password.getPassword());
            switch (role) {
                case DOCTOR -> userService.createDoctor(admin, name.getText(), email.getText(), pw,
                        extra1.getText(), extra2.getText());
                case PATIENT -> userService.createPatient(admin, name.getText(), email.getText(), pw,
                        LocalDate.parse(extra1.getText().trim()), extra2.getText());
                case ADMIN -> userService.createAdmin(admin, name.getText(), email.getText(), pw);
            }
            loadUsers();
        } catch (DateTimeParseException ex) {
            error("DOB must look like 2001-05-14");
        } catch (IllegalArgumentException ex) {
            error(ex.getMessage());
        }
    }

    private void removeSelectedUser() {
        int row = userTable.getSelectedRow();
        if (row < 0) {
            error("Select a user first.");
            return;
        }
        String id = (String) userModel.getValueAt(row, 0);
        String name = (String) userModel.getValueAt(row, 1);
        int ok = JOptionPane.showConfirmDialog(this,
                "Remove " + name + " (" + id + ")?\nTheir medical records are kept.",
                "Remove user", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ok != JOptionPane.YES_OPTION) return;
        try {
            userService.removeUser(admin, id);
            loadUsers();
        } catch (IllegalArgumentException ex) {
            error(ex.getMessage());
        }
    }

    private void error(String message) {
        JOptionPane.showMessageDialog(this, message, "Cannot do that", JOptionPane.ERROR_MESSAGE);
    }

    private static JPanel tabContent(JTable table, JButton... buttons) {
        table.setRowHeight(24);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT));
        for (JButton b : buttons) bottom.add(b);
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(bottom, BorderLayout.SOUTH);
        return panel;
    }

    private static DefaultTableModel readOnlyModel(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
    }
}