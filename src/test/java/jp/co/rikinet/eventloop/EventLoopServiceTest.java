/*
 * Copyright (c) 2018 Riki Network Systems, Inc.
 * All rights reserved.
 */

package jp.co.rikinet.eventloop;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.Exchanger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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

    public static class CountUpEvent implements Event {
        int count;
        EventLoopService service;
        public Object getValue() {
            return count;
        }
    }

    public static class CountUpAction implements Action {
        private CountUpEvent event;
        public void setEvent(Event event) {
            this.event = (CountUpEvent) event;
        }
        public void run() {
            if (event.count < 10) {
                event.count += 1;
                event.service.entry(event);  // dispatch new event in action
            }
        }
    }

    @Before
    public void setUp() {
        service = new EventLoopService();
        service.register(TestEvent.class, TestAction.class);
        service.register(CountUpEvent.class, CountUpAction.class);
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
    public void countUp() throws InterruptedException {
        CountUpEvent event = new CountUpEvent();
        event.count = 0;
        event.service = service;
        service.entry(event);
        Thread.sleep(1000L); // wait for end of action's runs
        assertEquals(10, event.count);
    }

    @Test
    public void interrupt() throws InterruptedException {
        serviceThread.interrupt();
        Thread.sleep(1000L);
        assertTrue(service.isStopped());
    }
}
