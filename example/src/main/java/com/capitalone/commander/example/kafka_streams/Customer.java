package com.capitalone.commander.example.kafka_streams;

import java.util.Map;
import java.util.UUID;

public class Customer {

    private final UUID id;
    private final String first_name;
    private final String last_name;
    private final Address address;

    public Customer(UUID id, String first_name, String last_name, Address address) {
        this.id = id;
        this.first_name = first_name;
        this.last_name = last_name;
        this.address = address;
    }

    public Customer(Map<Keyword, Object> params) {
        this((UUID) params.get(new Keyword("id")),
                (String) params.get(new Keyword("first_name")),
                (String) params.get(new Keyword("last_name")),
                new Address((Map) params.get(new Keyword("address"))));
    }

    public UUID getId() {
        return id;
    }

    public String getFirst_name() {
        return first_name;
    }

    public String getLast_name() {
        return last_name;
    }

    public Address getAddress() {
        return address;
    }
}
