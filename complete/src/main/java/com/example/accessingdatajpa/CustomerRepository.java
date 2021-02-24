package com.example.accessingdatajpa;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

public interface CustomerRepository extends CrudRepository<Customer, Long> {

	List<Customer> findByLastName(String lastName);
	List<Customer> findByLastName2(String lastName);
	List<Customer> findByLastName3(String lastName);
	List<Customer> findByLastName4(String lastName);
	List<Customer> findByLastName5(String lastName);
	List<Customer> findByLastName6(String lastName);
	List<Customer> findByFirstName(String firstName);
	List<Customer> findByFirstNameAndLastName(String firstName, String lastName);

	Customer findById(long id);
}
