package com.luckxpress.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.sentry.Sentry;

@Service
@Profile({"dev", "local"})
public class ActivitySimulatorService {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    @Autowired(required = false)
    private MeterRegistry meterRegistry;

    private Counter broadcastCounter;

    private final String[] users = {
            "john***@gmail.com", "sarah***@yahoo.com", "mike***@hotmail.com"
    };

    private final String[] actions = {
            "Deposit", "Withdrawal", "KYC Submitted", "Game Started", "Bonus Claimed"
    };

    private final Random random = new Random();

    @Autowired
    public void initCounters(MeterRegistry registry) {
        if (registry != null) {
            this.broadcastCounter = Counter.builder("websocket_activity_broadcast_total")
                    .description("Total number of activity messages broadcast over WebSocket")
                    .register(registry);
        }
    }

    @Scheduled(fixedDelay = 5000) // Every 5 seconds
    public void sendActivity() {
        Map<String, Object> activity = new HashMap<>();
        activity.put("id", UUID.randomUUID().toString());
        activity.put("user", users[random.nextInt(users.length)]);
        activity.put("action", actions[random.nextInt(actions.length)]);
        activity.put("amount", new BigDecimal(random.nextInt(100) + 10));
        activity.put("timestamp", Instant.now().toString());
        activity.put("status", random.nextBoolean() ? "success" : "failed");
        try {
            messagingTemplate.convertAndSend("/topic/activity", activity);
            if (broadcastCounter != null) {
                broadcastCounter.increment();
            }
            Sentry.addBreadcrumb("Activity broadcasted to /topic/activity");
        } catch (Exception ex) {
            Sentry.captureException(ex);
        }
    }
}
