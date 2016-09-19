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

import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.processor.Processor;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.processor.ProcessorSupplier;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.KeyValueStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CustomerStore implements ProcessorSupplier<UUID, Map>, Processor<UUID, Map> {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private ProcessorContext context;
    private KeyValueStore<UUID, Map> store;

    public List<Customer> getCustomers() {
        List<Customer> customers = new ArrayList<>();
        KeyValueIterator<UUID, Map> iterator = store.all();
        while (iterator.hasNext()) {
            KeyValue<UUID, Map> entry = iterator.next();
            logger.debug("getCustomers iterator entry: {}", entry);
            customers.add(new Customer(entry.value));
        }
        iterator.close();
        return customers;
    }

    public Customer getCustomer(UUID id) {
        return new Customer(store.get(id));
    }

    @Override
    public void init(ProcessorContext processorContext) {
        this.context = processorContext;
        this.store = (KeyValueStore<UUID, Map>) context.getStateStore("Customers");
    }

    @Override
    public void process(UUID uuid, Map map) {
        logger.debug("CustomerStore.process(UUID {}, Map {})", uuid, map);
        store.put(uuid, map);
        logger.debug("after put");
    }

    @Override
    public void punctuate(long l) {

    }

    @Override
    public void close() {

    }

    @Override
    public Processor<UUID, Map> get() {
        return this;
    }
}
