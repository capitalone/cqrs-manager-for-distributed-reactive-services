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

import io.dropwizard.lifecycle.Managed;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KStreamBuilder;
import org.apache.kafka.streams.processor.StateStoreSupplier;
import org.apache.kafka.streams.state.Stores;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CommandProcessor implements Managed {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private KafkaStreams kafkaStreams;
    private final String commandsTopic;
    private final String eventsTopic;
    private final String customersTopic;

    private final CustomerStore customerStore;

    private final StreamsConfig kafkaStreamsConfig;

    public CommandProcessor(CustomerStore customerStore,
                            StreamsConfig kafkaStreamsConfig,
                            String commandsTopic,
                            String eventsTopic,
                            String customersTopic) {
        this.customerStore = customerStore;
        this.kafkaStreamsConfig = kafkaStreamsConfig;
        this.commandsTopic = commandsTopic;
        this.eventsTopic = eventsTopic;
        this.customersTopic = customersTopic;
    }

    @Override
    public void start() {
        KStreamBuilder builder = new KStreamBuilder();

        Serde<UUID> keySerde = new FressianSerde();
        Serde<Map> valSerde = new FressianSerde();

        KStream<UUID, Map> commands = builder.stream(keySerde, valSerde, commandsTopic);
        KStream<UUID, Map> customerEvents = commands
                .filter((id, command) -> command.get(new Keyword("action")).equals(new Keyword("create-customer")))
                .map((id, command) -> {
                    logger.debug("Command received");
                    Map userEvent = new HashMap(command);
                    userEvent.put(new Keyword("action"), new Keyword("customer-created"));
                    userEvent.put(new Keyword("parent"), id);
                    Map userValue = (Map) userEvent.get(new Keyword("data"));
                    userValue.put(new Keyword("id"), UUID.randomUUID());
                    return new KeyValue<>(UUID.randomUUID(), userEvent);
        }).through(keySerde, valSerde, eventsTopic);

        KStream<UUID, Map> customers = customerEvents
                .map((id, event) -> {
                    Map customer = (Map) event.get(new Keyword("data"));
                    UUID customerId = (UUID) customer.get(new Keyword("id"));
                    return new KeyValue<UUID, Map>(customerId, customer);
                });

        customers.through(keySerde, valSerde, customersTopic);

        StateStoreSupplier store = Stores.create("Customers")
                .withKeys(keySerde)
                .withValues(valSerde)
                .persistent()
                .build();
        builder.addStateStore(store);

        customers.process(customerStore, "Customers");

        this.kafkaStreams = new KafkaStreams(builder, kafkaStreamsConfig);
        this.kafkaStreams.start();
    }

    @Override
    public void stop() {
        this.kafkaStreams.close();
    }

}
