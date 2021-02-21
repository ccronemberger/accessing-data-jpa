package com.example.accessingdatajpa;

import com.timgroup.statsd.NonBlockingStatsDClient;
import io.opentracing.Span;
import javax.persistence.EntityManagerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;

@org.springframework.context.annotation.Configuration
public class Configuration {

    private NonBlockingStatsDClient statsClient = new NonBlockingStatsDClient(null, "localhost", 8125);

    @Bean
    public PlatformTransactionManager transactionManager(EntityManagerFactory emf) {
        return new JpaTransactionManager(emf) {
            @Override
            protected void doBegin(Object transaction, TransactionDefinition definition) {
                io.opentracing.Tracer openTracer = io.opentracing.util.GlobalTracer.get();
                Span span = openTracer.activeSpan();
                String spanName = "no-span";
                if (span != null) {
                    spanName = span.getBaggageItem("name");
                }
                logger.error("starting a tx in " + spanName);
                statsClient.increment("tx-count", "ntx:" + spanName);
                super.doBegin(transaction, definition);
            }
        };
    }

}
