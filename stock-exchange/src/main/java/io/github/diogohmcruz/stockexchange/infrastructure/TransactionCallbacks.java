package io.github.diogohmcruz.stockexchange.infrastructure;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import lombok.extern.slf4j.Slf4j;

/**
 * Runs side effects (book updates, Kafka publishing) only once the surrounding transaction outcome is known, so a
 * rolled back transaction never leaks an event or a book change. Without an active transaction the action runs
 * immediately (commit) or is dropped (rollback).
 */
@Slf4j
public final class TransactionCallbacks {

    private TransactionCallbacks() {}

    public static void afterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            run(action);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                run(action);
            }
        });
    }

    public static void afterRollback(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    run(action);
                }
            }
        });
    }

    private static void run(Runnable action) {
        try {
            action.run();
        } catch (RuntimeException e) {
            log.error("Post-transaction action failed", e);
        }
    }
}
