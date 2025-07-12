package city.norain.slimefun4.dough;

import java.util.function.IntConsumer;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang.Validate;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

class TaskNode {

    private final IntConsumer runnable;

    @Getter
    private final boolean asynchronous;

    @Getter
    private int delay = 0;

    @Nullable @Getter
    @Setter
    private Location location;

    @Nullable @Getter
    @Setter
    private Entity entity;

    private TaskNode nextNode;

    protected TaskNode(@Nonnull IntConsumer consumer, boolean async) {
        this.runnable = consumer;
        this.asynchronous = async;
    }

    protected TaskNode(@Nonnull IntConsumer consumer, int delay, boolean async) {
        this.runnable = consumer;
        this.delay = delay;
        this.asynchronous = async;
    }

    protected boolean hasNextNode() {
        return nextNode != null;
    }

    public @Nullable TaskNode getNextNode() {
        return nextNode;
    }

    public void setNextNode(@Nullable TaskNode node) {
        this.nextNode = node;
    }

    public void execute(int index) {
        runnable.accept(index);
    }

    public void setDelay(int delay) {
        Validate.isTrue(delay >= 0, "The delay cannot be negative.");

        this.delay = delay;
    }
}
