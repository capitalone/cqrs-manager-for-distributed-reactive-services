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

import com.codahale.metrics.annotation.Timed;

import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.UUID;

@Path("/customers")
@Produces(MediaType.APPLICATION_JSON)
public class CustomerResource {
    @NotNull
    private final CustomerStore customerStore;

    public CustomerResource(CustomerStore customerStore) {
        this.customerStore = customerStore;
    }

    public CustomerStore getCustomerStore() {
        return customerStore;
    }

    @GET
    @Timed
    public Iterable<Customer> customers() {
        return customerStore.getCustomers();
    }

    @GET
    @Timed
    @Path("{id}")
    public Customer customer(@PathParam("id") UUID id) {
        return customerStore.getCustomer(id);
    }
}
