package com.medilink.connector;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class HospitalConnectorRegistry {
    private final Map<String, HospitalConnector> connectors = new LinkedHashMap<>();

    public void register(String hospitalId, HospitalConnector connector) {
        connectors.put(hospitalId, connector);
    }

    public HospitalConnector getConnector(String hospitalId) {
        HospitalConnector c = connectors.get(hospitalId);
        if (c == null) throw new IllegalArgumentException("Unknown hospital: " + hospitalId);
        return c;
    }

    public Collection<HospitalConnector> getAll() { return connectors.values(); }
}