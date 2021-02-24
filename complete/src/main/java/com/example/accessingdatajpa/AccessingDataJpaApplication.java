package com.example.accessingdatajpa;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.persistence.EntityManagerFactory;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.SingularAttribute;
import javax.sql.DataSource;
import org.hibernate.SessionFactory;
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

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private Gson gson;

    public static class EntitySerializer implements JsonSerializer<Object> {

        private Map<Class, EntityType> map;
        private Map<Class, Field> fieldIdMap;

        public EntitySerializer(EntityManagerFactory entityManagerFactory) {
            SessionFactory sessionFactory =
                entityManagerFactory.unwrap(SessionFactory.class);

            //Map map = sessionFactory.getAllClassMetadata();
            

            fieldIdMap = sessionFactory.getMetamodel()
                                .getEntities()
                                .stream()
                                .collect(
                                    Collectors.toMap(
                                        entityType -> entityType.getJavaType(),
                                        entityType -> (Field)entityType.getId(Object.class).getJavaMember()));
        }

        @Override
        public JsonElement serialize(Object entity, Type type, JsonSerializationContext context) {
            Field field = fieldIdMap.get(entity.getClass());
            try {
                Object id = field.get(entity);
                if (id == null) {
                    return JsonNull.INSTANCE;
                } else {
                    return new JsonPrimitive(id.toString());
                }
            } catch (IllegalAccessException e) {
                return new JsonPrimitive(e.getMessage());
            }
        }
    }

    @Bean
    public CommandLineRunner demo(CustomerRepository repository) {
        return (args) -> {

            GsonBuilder builder = new GsonBuilder();
            builder.registerTypeAdapter(Customer.class, new EntitySerializer(entityManagerFactory));
            gson = builder.create();

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

            System.out.println("loading entity:");
            // fetch an individual customer by ID
            Customer customer = repository.findById(1L);
            System.out.println("loading entity again:");
            customer = repository.findById(1L);
            System.out.println("loading non existing");
            repository.findById(1234L);
            System.out.println("finished loading");

            MyDTO myDTO = new MyDTO();
            myDTO.customer = customer;
            String str = gson.toJson(myDTO);
            System.out.println("serialized " + str);

            log.info("Customer found with findById(1L):");
            log.info("--------------------------------");
            log.info(customer.toString());
            log.info("");

            // fetch customers by last name
            log.info("Customer found with findByLastName('Bauer'):");
            log.info("--------------------------------------------");
            repository.findByLastName("Bauer")
                      .forEach(bauer -> {
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
                    sb.append(metadata.getColumnName(i))
                      .append(" = ")
                      .append(rs.getString(i))
                      .append(" ");
                }
                System.out.println(sb.toString());
            }
        }
    }

}
