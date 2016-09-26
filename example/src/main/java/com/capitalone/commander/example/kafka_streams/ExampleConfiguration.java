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

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.kafka.streams.StreamsConfig;

import java.util.HashMap;
import java.util.Map;

public class ExampleConfiguration extends io.dropwizard.Configuration {
    private StreamsConfigFactory streamsConfigFactory = new StreamsConfigFactory();

    private String commandsTopic;
    private String eventsTopic;
    private String customersTopic;

    public String getCommandsTopic() {
        return commandsTopic;
    }

    public void setCommandsTopic(String commandsTopic) {
        this.commandsTopic = commandsTopic;
    }

    public String getEventsTopic() {
        return eventsTopic;
    }

    public void setEventsTopic(String eventsTopic) {
        this.eventsTopic = eventsTopic;
    }

    public String getCustomersTopic() {
        return customersTopic;
    }

    public void setCustomersTopic(String customersTopic) {
        this.customersTopic = customersTopic;
    }

    @JsonProperty("kafkaStreams")
    public void setStreamsConfigFactory(StreamsConfigFactory factory) {
        this.streamsConfigFactory = factory;
    }

    @JsonProperty("kafkaStreams")
    public StreamsConfigFactory getStreamsConfigFactory() {
        return streamsConfigFactory;
    }

}
