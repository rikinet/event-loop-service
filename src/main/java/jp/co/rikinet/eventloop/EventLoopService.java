/*
 * Copyright (c) 2018 Riki Network Systems, Inc.
 * All rights reserved.
 */

package jp.co.rikinet.eventloop;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * いわゆるイベントループを実行するサービス。
 */
public class EventLoopService implements Runnable {
    private static Logger logger = LoggerFactory.getLogger(EventLoopService.class);

    private static final InterruptedEvent INTERRUPTED_EVENT = new InterruptedEvent();

    /** Event を受け取るためのキュー */
    private BlockingQueue<Event> eventQueue;
    /** Event の種類に応じて実行する Action の種類を決める */
    private Map<Class<? extends Event>, Class<? extends Action>> actionMap;
    /** サービスの運転状態を表す */
    private boolean stopped;
    /** サービスの運転状態を定期的に調べるため、キューの読み出しで完全にブロックしない */
    private long timeout;
    /** ExecutorService はクライアントコードで設定を変更できるように外部からアクセス可能にする */
    private ExecutorService executorService;

    public static final long DEFAULT_TIMEOUT_MILLIS = 1000L;


    /**
     * InterruptedException の発生を Event に包んで処理する。
     */
    private static class InterruptedEvent implements Event {
        private Exception cause;
        public Object getValue() {
            return cause;
        }
        public void setCause(Exception cause) {
            this.cause = cause;
        }
    }

    public EventLoopService() {
        actionMap = new HashMap<>();
        eventQueue = new LinkedBlockingQueue<>();
        setStopped(false);
        setTimeout(DEFAULT_TIMEOUT_MILLIS);
        ExecutorService service = new ThreadPoolExecutor(1, 1, DEFAULT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>());
        setExecutorService(service);
    }

    public void run() {
        if (getExecutorService() == null) {
            logger.error("ExecutorService not set.  Abort.");
            return;
        }
        logger.info("event loop started.");
        while (!stopped) {
            Event event;
            Action action = null;

            // event を得る
            try {
                event = eventQueue.poll(timeout, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e1) {
                event = INTERRUPTED_EVENT;
                ((InterruptedEvent) event).setCause(e1);
            }
            if (event == null) {
                // poll() タイムアウトが発生した。
                // タイマーイベントが必要なら別途定義して、このサービスに追加すべし。
                continue;
            }
            if (logger.isDebugEnabled()) {
                logger.debug("event received: " + event.getClass().getName());
            }

            // action を求める
            if (event.getClass() == InterruptedEvent.class) {
                // 内部クラスにすると InstantiationException が
                // 発生してしまうのを回避するためにこうしている。
                action = new Action() {
                    @Override
                    public void setEvent(Event event) {
                        // no-op
                    }
                    @Override
                    public void run() {
                        setStopped(true);
                    }
                };
            } else if (!actionMap.containsKey(event.getClass())) {
                logger.warn(event.getClass().getName() + ": event class not registered.");
                continue;
            }
            Class<? extends Action> aClass = actionMap.get(event.getClass());
            try {
                if (aClass != null)
                    action = aClass.newInstance();
            } catch (InstantiationException e) {
                logger.warn(aClass.getName() + ": newInstance() failed.", e);
            } catch (IllegalAccessException e) {
                logger.warn(aClass.getName() + ": access denied.", e);
            }
            if (action == null) {
                logger.warn(event.getClass().getName() + ": action not registered.");
                continue;
            }

            action.setEvent(event);
            executorService.submit(action);
        }
        logger.info("end of event loop.");
    }

    /**
     * Event をサービスに登録する。
     * @param event Action に処理させるイベント
     */
    public void entry(Event event) {
        try {
            eventQueue.offer(event);
        } catch (RuntimeException e) {
            logger.warn("offering to event queue failed.", e);
        }
    }

    /**
     * Event と Action のクラスレベルの対応をサービスに登録する。
     * @param eventClass Event を実装したクラス
     * @param actionClass Action を実装したクラス
     */
    public void register(Class<? extends Event> eventClass, Class<? extends Action> actionClass) {
        actionMap.put(eventClass, actionClass);
    }

    public boolean isStopped() {
        return stopped;
    }

    public void setStopped(boolean stopped) {
        this.stopped = stopped;
        logger.info("service status changed to " + (stopped ? "stop" : "run"));
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    public void setExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
    }
}
