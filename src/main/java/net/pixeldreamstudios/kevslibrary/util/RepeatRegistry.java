package net.pixeldreamstudios.kevslibrary.util;

import java.util.*;

public class RepeatRegistry {
    private static final Map<UUID, Runnable> activeRepeats = new HashMap<>();

    public static UUID registerInfinite(Runnable task, int intervalTicks) {
        UUID id = UUID.randomUUID();
        Runnable loop = new Runnable() {
            @Override
            public void run() {
                if (!activeRepeats.containsKey(id)) return;
                task.run();
                DelayedExecutor.runLater(this, intervalTicks);
            }
        };

        activeRepeats.put(id, loop);
        DelayedExecutor.runLater(loop, intervalTicks);
        return id;
    }

    public static void clearAll() {
        activeRepeats.clear();
    }
}
