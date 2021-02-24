package com.example.accessingdatajpa;

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

@Component
public class LoggerBeanPostProcessor implements BeanPostProcessor, Ordered {

    protected Log logger = LogFactory.getLog("org.springframework.beans.factory.LoggerBeanPostProcessor");
    private Map<String, Long> start;
    private Map<String, Long> end;

    public LoggerBeanPostProcessor() {
        start = new HashMap<>();
        end = new HashMap<>();
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        start.put(beanName, System.currentTimeMillis());
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        end.put(beanName, System.currentTimeMillis());
        logger.debug("Init time for " + beanName + ": " + initializationTime(beanName));
        return bean;
    }

    @Override
    public int getOrder() {
        return Integer.MAX_VALUE;
    }

    // this method returns initialization time of the bean.
    public long initializationTime(String beanName) {
        return end.get(beanName) - start.get(beanName);
    }
}