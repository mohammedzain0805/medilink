package com.medilink.connector;

import com.medilink.model.MedicalRecord;
import java.util.ArrayList;
import java.util.List;
import com.medilink.model.Hospital;

public class GenericHL7Connector extends AbstractHospitalConnector {
    private boolean online = true;

    public GenericHL7Connector() { super(new Hospital("HL7", "Generic HL7 Hospital", "Bengaluru, Karnataka")); }
    public void setOnline(boolean online) { this.online = online; }

    @Override public boolean isAvailable() { return online; }

    @Override
    public List<MedicalRecord> fetchRecords(String patientId) {
        return online ? new ArrayList<>(recordsFor(patientId)) : new ArrayList<>();
    }
}