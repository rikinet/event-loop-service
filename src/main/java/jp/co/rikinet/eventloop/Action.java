/*
 * Copyright (c) 2018 Riki Network Systems, Inc.
 * All rights reserved.
 */

package jp.co.rikinet.eventloop;

/**
 * イベントに対応するアクションを実行する主体。
 * Executor で実行するため、Runnable を継承する。
 * 継承を強要してクラス設計が不自由になるのを避けるため、interface にした。
 */
public interface Action extends Runnable {
    /**
     * アクションの動作対象になるイベントをプロパティに設定する。
     * @param event 動作対象のイベント
     */
    void setEvent(Event event);
}
