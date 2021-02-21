package com.example.accessingdatajpa;

import io.opentracing.Span;
import io.opentracing.Tracer.SpanBuilder;
import javax.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class CustomerService {

    private final CustomerRepository repository;

    public CustomerService(CustomerRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void modifyCustomer(Customer customer) {
        customer = repository.save(customer);
        customer.setFirstName(customer.getFirstName() + " - " + System.currentTimeMillis());

        Customer customer2 = repository.findById((long)customer.getId());
        System.out.println("same instance: " + (customer2 == customer));
    }

    @Transactional
    public void parallelMethodTx(Span parentSpan) {
        parallelMethodImpl();
    }

    public void parallelMethod(Span parentSpan) {
        io.opentracing.Tracer openTracer = io.opentracing.util.GlobalTracer.get();
        SpanBuilder spanBuilder = openTracer.buildSpan("method");
        spanBuilder.asChildOf(parentSpan);
        Span span = spanBuilder.start();
        openTracer.activateSpan(span);
        span.setOperationName("parallel-no-tx2");
        span.setBaggageItem("name", "parallel-no-tx2");
        parallelMethodImpl();
        span.finish();
    }

    private void parallelMethodImpl() {

        repository.save(new Customer("Kim", "Bauer"));
        repository.save(new Customer("David", "Palmer"));
        repository.save(new Customer("Michelle", "Dessler"));

        try {
            Thread.sleep(15);
        } catch (InterruptedException e) {
        }
    }
}
