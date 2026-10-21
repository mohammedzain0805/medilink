package com.medilink.ui;

import com.medilink.model.Doctor;
import com.medilink.model.User;
import com.medilink.service.*;
import com.medilink.model.Patient;
import javax.swing.*;
import java.awt.*;
import com.medilink.model.Admin;

public class MainFrame extends JFrame {
    private static final String LOGIN = "LOGIN";
    private static final String DASHBOARD = "DASHBOARD";

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cards = new JPanel(cardLayout);
    private final LoginPanel loginPanel;
    private final AuthenticationService auth;
    private final RecordService records;
    private final AuditService audit;
    private final NotificationService notifications;
    private final UserService userService;
    private JPanel dashboard;

    public MainFrame(AuthenticationService auth, RecordService records, AuditService audit,
                     NotificationService notifications, UserService userService) {
        super("MediLink");
        this.auth = auth;
        this.records = records;
        this.audit = audit;
        this.notifications = notifications;
        this.userService = userService;

        loginPanel = new LoginPanel(auth, this::showDashboard);
        cards.add(loginPanel, LOGIN);
        add(cards);

        getRootPane().setDefaultButton(loginPanel.getLoginButton());
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1000, 650);
        setLocationRelativeTo(null);
    }

    private void showDashboard(User user) {
        if (dashboard != null) cards.remove(dashboard);
        Runnable logout = () -> logout(user);
        dashboard = switch (user.getRole()) {
            case DOCTOR  -> new DoctorDashboard((Doctor) user, records, notifications, userService, logout);
            case ADMIN   -> new AdminPanel((Admin) user, audit, userService, logout);
            case PATIENT -> new PatientDashboard((Patient) user, records, notifications, userService, logout);
        };
        cards.add(dashboard, DASHBOARD);
        getRootPane().setDefaultButton(null);
        cardLayout.show(cards, DASHBOARD);
    }

    private void logout(User user) {
        auth.logout(user);
        getRootPane().setDefaultButton(loginPanel.getLoginButton());
        cardLayout.show(cards, LOGIN);
    }
}