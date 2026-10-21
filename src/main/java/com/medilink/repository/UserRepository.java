package com.medilink.repository;

import com.medilink.model.Patient;
import com.medilink.model.User;
import java.time.LocalDate;
import java.util.List;

public interface UserRepository {
    User findById(String userId);
    User findByEmail(String email);
    void save(User user);
    void delete(String userId);
    Patient findPatientByNameAndDob(String name, LocalDate dob);
    List<User> findAll();
}
