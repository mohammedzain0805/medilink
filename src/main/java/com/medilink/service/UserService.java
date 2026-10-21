package com.medilink.service;

import com.medilink.model.*;
import com.medilink.repository.UserRepository;
import com.medilink.util.PasswordHasher;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class UserService {
    private static final int MIN_PASSWORD_LENGTH = 6;

    private final UserRepository users;
    private final AuditService audit;

    public UserService(UserRepository users, AuditService audit) {
        this.users = users;
        this.audit = audit;
    }

    // ---------- listing and directory ----------

    public List<User> listUsers(User requester) {
        requireAdmin(requester, "list user accounts");
        audit.log(requester, "LIST_USERS", null);
        return users.findAll();
    }

    // Doctors (for second opinions) and patients (to grant consent) can browse the doctor directory
    public List<Doctor> listDoctors(User requester) {
        if (requester == null
                || (requester.getRole() != Role.DOCTOR && requester.getRole() != Role.PATIENT))
            throw new SecurityException("Only doctors and patients can browse the doctor directory");
        List<Doctor> result = new ArrayList<>();
        for (User u : users.findAll())
            if (u instanceof Doctor d && !d.getUserId().equals(requester.getUserId()))
                result.add(d);
        return result;
    }

    public String nameOf(String userId) {
        User u = users.findById(userId);
        return u == null ? userId : u.getFullName();
    }

    // ---------- self-registration (patients only) ----------

    public Patient registerPatient(String name, String email, String password,
                                   LocalDate dob, String bloodGroup) {
        Patient p = buildPatient(name, email, password, dob, bloodGroup);
        users.save(p);
        audit.log(p, "REGISTER", null);
        return p;
    }

    // ---------- admin: manage accounts ----------

    public Doctor createDoctor(User requester, String name, String email, String password,
                               String license, String specialization) {
        requireAdmin(requester, "create accounts");
        checkNewAccount(email, password);
        if (clean(license).isEmpty() || clean(specialization).isEmpty())
            throw new IllegalArgumentException("License number and specialization are required");
        return saveNew(requester, new Doctor(nextId(Role.DOCTOR), clean(name), clean(email),
                PasswordHasher.hash(password), clean(license), clean(specialization)));
    }

    public Patient createPatient(User requester, String name, String email, String password,
                                 LocalDate dob, String bloodGroup) {
        requireAdmin(requester, "create accounts");
        return saveNew(requester, buildPatient(name, email, password, dob, bloodGroup));
    }

    public Admin createAdmin(User requester, String name, String email, String password) {
        requireAdmin(requester, "create accounts");
        checkNewAccount(email, password);
        return saveNew(requester, new Admin(nextId(Role.ADMIN), clean(name), clean(email),
                PasswordHasher.hash(password)));
    }

    // Removes the ACCOUNT. Medical records are kept (they belong to the hospitals' history).
    public void removeUser(User requester, String userId) {
        requireAdmin(requester, "remove accounts");
        User target = users.findById(userId);
        if (target == null) throw new IllegalArgumentException("No such user: " + userId);
        if (target.getUserId().equals(requester.getUserId()))
            throw new IllegalArgumentException("You cannot remove your own account");

        if (target instanceof Patient p) {            // take the patient off every doctor's list
            for (User u : users.findAll())
                if (u instanceof Doctor d) {
                    d.removePatientUnderCare(p);
                    d.clearEmergencyAccess(p.getUserId());
                }
        }
        users.delete(userId);
        audit.log(requester, "REMOVE_USER " + userId + " (" + target.getRole() + ")", null);
    }

    // ---------- internals ----------

    private Patient buildPatient(String name, String email, String password,
                                 LocalDate dob, String bloodGroup) {
        checkNewAccount(email, password);
        if (dob == null || dob.isAfter(LocalDate.now()))
            throw new IllegalArgumentException("Date of birth must be in the past");
        return new Patient(nextId(Role.PATIENT), clean(name), clean(email),
                PasswordHasher.hash(password), dob, clean(bloodGroup), "None");
    }

    private <T extends User> T saveNew(User requester, T user) {
        users.save(user);
        audit.log(requester, "CREATE_USER " + user.getUserId() + " as " + user.getRole(), null);
        return user;
    }

    private void checkNewAccount(String email, String password) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH)
            throw new IllegalArgumentException(
                    "Password must be at least " + MIN_PASSWORD_LENGTH + " characters");
        if (users.findByEmail(clean(email)) != null)
            throw new IllegalArgumentException("That email is already registered");
    }

    private String nextId(Role role) {
        String prefix = switch (role) {
            case DOCTOR -> "D";
            case PATIENT -> "P";
            case ADMIN -> "A";
        };
        int max = 0;
        for (User u : users.findAll()) {
            if (u.getRole() != role) continue;
            try {
                max = Math.max(max, Integer.parseInt(u.getUserId().substring(1)));
            } catch (NumberFormatException ignored) { }
        }
        return prefix + (max + 1);
    }

    private static String clean(String s) { return s == null ? "" : s.trim(); }

    private void requireAdmin(User requester, String what) {
        if (requester == null || requester.getRole() != Role.ADMIN) {
            audit.log(requester, "ACCESS_DENIED " + what, null);
            throw new SecurityException("Only admins can " + what);
        }
    }
}