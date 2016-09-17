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
