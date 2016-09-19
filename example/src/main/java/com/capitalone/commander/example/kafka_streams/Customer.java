/*
 * Copyright 2016 Capital One Services, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

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
