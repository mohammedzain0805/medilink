package com.medilink.repository;

import com.medilink.model.Patient;
import com.medilink.model.User;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

public class InMemoryUserRepository implements UserRepository {
    private final Map<String, User> users = new HashMap<>();

    @Override public User findById(String userId) { return users.get(userId); }
    @Override public void save(User user) { users.put(user.getUserId(), user); }

    @Override
    public List<User> findAll() { return new ArrayList<>(users.values()); }

    @Override
    public Patient findPatientByNameAndDob(String name, LocalDate dob) {
        for (User u : users.values())
            if (u instanceof Patient p
                    && p.getFullName().equalsIgnoreCase(name)
                    && p.getDateOfBirth().equals(dob))
                return p;
        return null;
    }

    @Override
    public void delete(String userId) { users.remove(userId); }

    @Override
    public User findByEmail(String email) {
        for (User u : users.values())
            if (u.getEmail().equalsIgnoreCase(email)) return u;
        return null;
    }
}
