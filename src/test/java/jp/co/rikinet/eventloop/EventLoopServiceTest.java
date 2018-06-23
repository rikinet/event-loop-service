/*
 * Copyright (c) 2018 Riki Network Systems, Inc.
 * All rights reserved.
 */

package jp.co.rikinet.eventloop;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.Exchanger;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

public class EventLoopServiceTest {
    private EventLoopService service;
    private Thread serviceThread;

    public static class TestEvent implements Event {
        Exchanger<String> exchanger;
        String greeting;
        TestEvent(String greeting, Exchanger<String> exchanger) {
            this.greeting = greeting;
            this.exchanger = exchanger;
        }
        public Object getValue() {
            return greeting;
        }
    }

    public static class TestAction implements Action {
        private TestEvent event;
        public void setEvent(Event event) {
            this.event = (TestEvent) event;
        }
        @Override
        public void run() {
            try {
                event.exchanger.exchange(event.greeting);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
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
        Exchanger<String> exchanger = new Exchanger<>();
        service.entry(new TestEvent("Howdy", exchanger));
        String greeting2 = exchanger.exchange("Hello");
        assertEquals("Howdy", greeting2);
    }

    @Test
    public void interrupt() throws InterruptedException {
        serviceThread.interrupt();
        Thread.sleep(1000L);
    }
}
