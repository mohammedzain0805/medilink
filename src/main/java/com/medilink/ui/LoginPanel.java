package com.medilink.ui;

import com.medilink.model.User;
import com.medilink.service.AuthenticationService;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.function.Consumer;

public class LoginPanel extends JPanel {
    private final AuthenticationService auth;
    private final JTextField emailField = new JTextField(20);
    private final JPasswordField passwordField = new JPasswordField(20);
    private final JLabel messageLabel = new JLabel(" ");
    private final JButton loginButton = new JButton("Log in");
    private final JButton registerButton = new JButton("Register as patient");

    public LoginPanel(AuthenticationService auth, Consumer<User> onSuccess) {
        this.auth = auth;
        setLayout(new GridBagLayout());              // centres whatever we add
        JPanel form = new JPanel(new GridLayout(0, 1, 6, 6));

        JLabel title = new JLabel("MediLink", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 28f));
        messageLabel.setHorizontalAlignment(SwingConstants.CENTER);

        form.add(title);
        form.add(new JLabel("Email"));
        form.add(emailField);
        form.add(new JLabel("Password"));
        form.add(passwordField);
        form.add(loginButton);
        form.add(registerButton);
        form.add(messageLabel);
        add(form);

        loginButton.addActionListener(e -> {
            User user = auth.login(emailField.getText().trim(),
                    new String(passwordField.getPassword()));
            passwordField.setText("");
            if (user == null) {
                showMessage("Invalid email or password", Color.RED);
            } else {
                messageLabel.setText(" ");
                onSuccess.accept(user);
            }
        });
        registerButton.addActionListener(e -> register());
    }

    public JButton getLoginButton() { return loginButton; }

    private void showMessage(String text, Color color) {
        messageLabel.setForeground(color);
        messageLabel.setText(text);
    }

    private void register() {
        JTextField name = new JTextField(20);
        JTextField email = new JTextField(20);
        JPasswordField password = new JPasswordField(20);
        JTextField dob = new JTextField(20);
        JTextField blood = new JTextField(20);

        JPanel form = new JPanel(new GridLayout(0, 1, 4, 4));
        form.add(new JLabel("Full name:"));
        form.add(name);
        form.add(new JLabel("Email:"));
        form.add(email);
        form.add(new JLabel("Password (min 6 characters):"));
        form.add(password);
        form.add(new JLabel("Date of birth (yyyy-MM-dd):"));
        form.add(dob);
        form.add(new JLabel("Blood group (e.g. B+):"));
        form.add(blood);

        int ok = JOptionPane.showConfirmDialog(this, form, "Register as patient",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (ok != JOptionPane.OK_OPTION) return;

        try {
            auth.register(name.getText(), email.getText(), new String(password.getPassword()),
                    LocalDate.parse(dob.getText().trim()), blood.getText());
            emailField.setText(email.getText().trim());
            showMessage("Account created. Log in below.", new Color(0, 120, 0));
        } catch (DateTimeParseException ex) {
            showMessage("DOB must look like 2001-05-14", Color.RED);
        } catch (IllegalArgumentException ex) {
            showMessage(ex.getMessage(), Color.RED);
        }
    }
}