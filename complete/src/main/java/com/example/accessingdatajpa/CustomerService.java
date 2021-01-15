package com.example.accessingdatajpa;

import javax.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Transactional
    public void modifyCustomer(Customer customer) {
        customer = customerRepository.save(customer);
        customer.setFirstName(customer.getFirstName() + " - " + System.currentTimeMillis());

        Customer customer2 = customerRepository.findById((long)customer.getId());
        System.out.println("same instance: " + (customer2 == customer));
    }
}
