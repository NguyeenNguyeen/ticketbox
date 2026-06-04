package com.ticketbox.backend.config;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;

public class TestRecoverer {
    @Test
    public void test() {
        RepublishMessageRecoverer r = new RepublishMessageRecoverer(null, "ex");
        r.setErrorRoutingKeyPrefix("");
    }
}
