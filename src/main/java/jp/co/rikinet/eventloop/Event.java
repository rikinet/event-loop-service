/*
 * Copyright (c) 2018 Riki Network Systems, Inc.
 * All rights reserved.
 */

package jp.co.rikinet.eventloop;

/**
 * EventLoopService で処理するイベント。
 */
public interface Event {
    /**
     * Event の内容を返す。具体的な内容はこの Event に対応する Action との
     * あいだで規定すること。
     * @return この Event を処理する Action に有用な何か
     */
    Object getValue();
}
