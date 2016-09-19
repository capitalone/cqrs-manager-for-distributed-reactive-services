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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class Address {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final String street_number;
    private final String street_name;
    private final String city;
    private final String state;
    private final String zip;

    public Address (String street_number, String street_name, String city, String state, String zip) {
        this.street_number = street_number;
        this.street_name = street_name;
        this.city = city;
        this.state = state;
        this.zip = zip;
    }

    public Address(Map<Keyword, String> params) {
        if (params != null) {
            this.street_number = params.get(new Keyword("street_number"));
            this.street_name = params.get(new Keyword("street_name"));
            this.city = params.get(new Keyword("city"));
            this.state = params.get(new Keyword("state"));
            this.zip = params.get(new Keyword("zip"));
        } else {
            this.street_number = null;
            this.street_name = null;
            this.city = null;
            this.state = null;
            this.zip = null;
        }
    }

    public String getStreet_number() {
        return street_number;
    }

    public String getStreet_name() {
        return street_name;
    }

    public String getCity() {
        return city;
    }

    public String getState() {
        return state;
    }

    public String getZip() {
        return zip;
    }

}
