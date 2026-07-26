package io.github.thebusybiscuit.slimefun4.core.services.stability;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nonnull;

/** A small, thread-safe half-open circuit breaker for repeatedly failing machine locations. */
public final class MachineCircuitBreaker<K> {

    private final Map<K, CircuitState> circuits = new ConcurrentHashMap<>();

    public boolean canAttempt(@Nonnull K key, long nowMillis) {
        CircuitState state = circuits.get(key);
        if (state == null) {
            return true;
        }
        if (nowMillis < state.retryAfterMillis) {
            return false;
        }
        return state.probeInProgress.compareAndSet(false, true);
    }

    public void open(@Nonnull K key, long retryAfterMillis) {
        circuits.put(key, new CircuitState(retryAfterMillis));
    }

    public boolean isOpen(@Nonnull K key) {
        return circuits.containsKey(key);
    }

    public boolean clear(@Nonnull K key) {
        return circuits.remove(key) != null;
    }

    public int clearAll() {
        int count = circuits.size();
        circuits.clear();
        return count;
    }

    public int size() {
        return circuits.size();
    }

    private static final class CircuitState {
        private final long retryAfterMillis;
        private final AtomicBoolean probeInProgress = new AtomicBoolean(false);

        private CircuitState(long retryAfterMillis) {
            this.retryAfterMillis = retryAfterMillis;
        }
    }
}
