package net.pixeldreamstudios.kevslibrary.util;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.world.ServerWorld;

import java.util.ArrayList;
import java.util.List;

public class DelayedExecutor {
    private static final List<DelayedTask> tasks = new ArrayList<>();

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {

            List<DelayedTask> toRun = new ArrayList<>(tasks);

            for (DelayedTask task : toRun) {
                task.ticksLeft--;
                if (task.ticksLeft <= 0) {
                    task.runnable.run();
                    tasks.remove(task);
                }
            }
        });
    }


    public static void runLater(Runnable runnable, int delayTicks) {
        tasks.add(new DelayedTask(runnable, delayTicks));
    }

    private static class DelayedTask {
        Runnable runnable;
        int ticksLeft;

        DelayedTask(Runnable runnable, int ticksLeft) {
            this.runnable = runnable;
            this.ticksLeft = ticksLeft;
        }
    }
}
