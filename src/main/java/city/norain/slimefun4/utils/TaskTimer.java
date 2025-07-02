package city.norain.slimefun4.utils;

public class TaskTimer {
    private volatile long sampleTime;

    public TaskTimer() {
        sampleTime = System.currentTimeMillis();
    }

    public long peek() {
        return System.currentTimeMillis() - sampleTime;
    }

    public void reset() {
        sampleTime = System.currentTimeMillis();
    }

    public long getDuration() {
        long duration = peek();

        reset();

        return duration;
    }
}
