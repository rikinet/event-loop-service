/*
 * Copyright (c) 2018 Riki Network Systems, Inc.
 * All rights reserved.
 */

package jp.co.rikinet.eventloop;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class EventLoopServiceTest {
    private EventLoopService service;
    private Thread serviceThread;

    public static class TestEvent implements Event {
        TestEvent(String greeting) {
            this.greeting = greeting;
        }
        private String greeting;
        public Object getValue() {
            return greeting;
        }
    }

    public static class TestAction implements Action {
        private Event event;
        public void setEvent(Event event) {
            this.event = event;
        }
        public void run() {
            System.err.println(event.getValue() + ", world.");
        }
    }

    @Before
    public void setUp() {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 2, 1000L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>());
        service = new EventLoopService(executor);
        service.register(TestEvent.class, TestAction.class);
        serviceThread = new Thread(service);
        serviceThread.start();
    }

    @After
    public void tearDown() throws InterruptedException {
        service.setStopped(true);
        serviceThread.join();
    }

    @Test
    public void entry() throws InterruptedException {
        service.entry(new TestEvent("Howdy"));
        service.entry(new TestEvent("Hello"));
        Thread.sleep(1000L);
    }

    @Test
    public void interrupt() throws InterruptedException {
        serviceThread.interrupt();
        Thread.sleep(1000L);
    }
}
