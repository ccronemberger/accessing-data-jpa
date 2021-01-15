package com.example.accessingdatajpa;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class AccessingDataJpaApplication {

	private static final Logger log = LoggerFactory.getLogger(AccessingDataJpaApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(AccessingDataJpaApplication.class);
	}

	@Autowired
	private CustomerService customerService;

	@Autowired
	private DataSource dataSource;

	@Bean
	public CommandLineRunner demo(CustomerRepository repository) {
		return (args) -> {
			// save a few customers
			repository.save(new Customer("Jack", "Bauer"));
			repository.save(new Customer("Chloe", "O'Brian"));
			repository.save(new Customer("Kim", "Bauer"));
			repository.save(new Customer("David", "Palmer"));
			repository.save(new Customer("Michelle", "Dessler"));

			// fetch all customers
			log.info("Customers found with findAll():");
			log.info("-------------------------------");
			for (Customer customer : repository.findAll()) {
				log.info(customer.toString());
			}
			log.info("");

			// fetch an individual customer by ID
			Customer customer = repository.findById(1L);
			log.info("Customer found with findById(1L):");
			log.info("--------------------------------");
			log.info(customer.toString());
			log.info("");

			// fetch customers by last name
			log.info("Customer found with findByLastName('Bauer'):");
			log.info("--------------------------------------------");
			repository.findByLastName("Bauer").forEach(bauer -> {
				log.info(bauer.toString());
			});

			customer.setLastName(customer.getLastName() + "-2");
			checkDatabase(customer);
			customerService.modifyCustomer(customer);

			checkDatabase(customer);

			log.info("end");
		};
	}

	private void checkDatabase(Customer customer) throws SQLException {
		try (Connection connection = dataSource.getConnection()) {
			Statement statement = connection.createStatement();
			ResultSet rs = statement.executeQuery("select * from customer");
			ResultSetMetaData metadata = rs.getMetaData();
			int columns = metadata.getColumnCount();
			while (rs.next()) {
				StringBuilder sb = new StringBuilder();
				for (int i = 1; i <= columns; i++) {
					sb.append(metadata.getColumnName(i)).append(" = ").append(rs.getString(i)).append(" ");
				}
				System.out.println(sb.toString());
			}
		}
	}

}
