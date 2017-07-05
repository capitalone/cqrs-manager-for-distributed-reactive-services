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
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class CommandProcessor implements InitializingBean, DisposableBean {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private KafkaStreams kafkaStreams;
    private String commandsTopic;
    private String eventsTopic;
    private String customersTopic;

    @Resource
    private CustomerStore customerStore;

    @Resource
    private StreamsConfig kafkaStreamsConfig;

    @Bean
    public StreamsConfig kafkaStreamsConfig(@Value("${application.id}") String applicationIdConfig,
                                            @Value("${bootstrap.servers}") String bootstrapServersConfig,
                                            @Value("${commands.topic}") String commandsTopic,
                                            @Value("${events.topic}") String eventsTopic,
                                            @Value("${customers.topic}") String customersTopic) {
        this.commandsTopic = commandsTopic;
        this.eventsTopic = eventsTopic;
        this.customersTopic = customersTopic;
        Map<String, Object> props = new HashMap<>();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, applicationIdConfig);
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServersConfig);
        return new StreamsConfig(props);
    }

    public StreamsConfig getKafkaStreamsConfig() {
        return kafkaStreamsConfig;
    }

    public void setKafkaStreamsConfig(StreamsConfig kafkaStreamsConfig) {
        this.kafkaStreamsConfig = kafkaStreamsConfig;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.start();
    }

    @Override
    public void destroy() throws Exception {
        this.stop();
    }

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

    public void stop() {
        this.kafkaStreams.close();
    }

}
