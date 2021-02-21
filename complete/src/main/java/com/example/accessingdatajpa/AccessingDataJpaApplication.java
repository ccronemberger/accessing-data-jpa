package com.example.accessingdatajpa;

import com.timgroup.statsd.NonBlockingStatsDClient;
import io.opentracing.Span;
import io.opentracing.Tracer.SpanBuilder;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import javax.sql.DataSource;
import net.logstash.logback.marker.Markers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.PlatformTransactionManager;

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

	private NonBlockingStatsDClient statsClient = new NonBlockingStatsDClient(null, "localhost", 8125);

	private Random random = new Random(System.currentTimeMillis());

	@Autowired
	private PlatformTransactionManager platformTransactionManager;

	@Bean
	public CommandLineRunner demo(CustomerRepository repository) throws InterruptedException {

		Logger logger = LoggerFactory.getLogger(getClass());

		logger.error("tx manager: " + platformTransactionManager.getClass());

		io.opentracing.Tracer openTracer = io.opentracing.util.GlobalTracer.get();
		Span firstSpan = openTracer.activeSpan();

		SpanBuilder spanBuilder = openTracer.buildSpan("method");
		Span span = spanBuilder.start();
		span.setBaggageItem("name","my-operation");
		openTracer.activateSpan(span);

		span.setOperationName("my-operation");
		span.setTag("mytag", "test-tag");
		span.log("span log");

		subMethod(logger);

		Thread.sleep(45);

		Span newSpan2 = openTracer.activeSpan();

		span.finish();

		Span newSpan = CurrentSpanThreadLocal.getCurrentSpan();
		CurrentSpanThreadLocal.setCurrentSpan(newSpan2);
		newSpan = CurrentSpanThreadLocal.getCurrentSpan();
		CurrentSpanThreadLocal.setCurrentSpan(null);

		//if (true) return (args -> {});

		return (args) -> {
			io.opentracing.Tracer openTracer2 = io.opentracing.util.GlobalTracer.get();
			SpanBuilder spanBuilder2 = openTracer2.buildSpan("method");
			Span span2 = spanBuilder2.start();
			openTracer2.activateSpan(span2);
			span2.setOperationName("db-operations-new");
			span2.setBaggageItem("name","db-operations-new");

			// save a few customers
			repository.save(new Customer("Jack", "Bauer"));
			repository.save(new Customer("Chloe", "O'Brian"));

			Thread t = new Thread(() -> customerService.parallelMethod(span2));
			Thread t2 = new Thread(() -> getParallelMethodTx(span2));
			t.start();
			t2.start();

			Thread.sleep(45);
			t.join();
			t2.join();

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
			span2.finish();
		};
	}

	private void getParallelMethodTx(Span span2) {
		io.opentracing.Tracer openTracer = io.opentracing.util.GlobalTracer.get();
		SpanBuilder spanBuilder = openTracer.buildSpan("method");
		spanBuilder.asChildOf(span2);
		Span span = spanBuilder.start();
		openTracer.activateSpan(span);
		span.setOperationName("parallel-tx2");
		span.setBaggageItem("name", "parallel-tx2");
		customerService.parallelMethodTx(span2);
		span.finish();
	}

	private void subMethod(Logger logger) {
		io.opentracing.Tracer openTracer = io.opentracing.util.GlobalTracer.get();

		Span activeSpan = openTracer.activeSpan();

		Map<String, Object> markers = new HashMap<>();
		markers.put("dd.trace_id", activeSpan.context().toTraceId());
		markers.put("dd.span_id", activeSpan.context().toSpanId());
		Marker marker = Markers.appendEntries(markers);
		logger.error(marker, "span trace:" + activeSpan.context().toSpanId() + " span id: " + activeSpan.context().toTraceId());
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
				log.debug(sb.toString());
			}
		}
	}
}
