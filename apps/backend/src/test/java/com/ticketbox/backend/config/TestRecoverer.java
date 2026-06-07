package com.ticketbox.backend.config;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;

public class TestRecoverer {
    @Test
    public void test() {
        org.springframework.amqp.core.AmqpTemplate template = org.mockito.Mockito.mock(org.springframework.amqp.core.AmqpTemplate.class);
        RepublishMessageRecoverer r = new RepublishMessageRecoverer(template, "ex");
        r.setErrorRoutingKeyPrefix("");
    }
}
