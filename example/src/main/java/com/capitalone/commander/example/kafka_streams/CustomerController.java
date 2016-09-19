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

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
public class CustomerController {
    @Resource
    private CustomerStore customerStore;

    public CustomerStore getCustomerStore() {
        return customerStore;
    }

    public void setCustomerStore(CustomerStore customerStore) {
        this.customerStore = customerStore;
    }

    @RequestMapping("/customers")
    public List<Customer> customers() {
        return customerStore.getCustomers();
    }

    @RequestMapping("/customers/:id")
    public Customer customer(@RequestParam(value="id") String idStr) {
        UUID id = UUID.fromString(idStr);
        return customerStore.getCustomer(id);
    }
}
