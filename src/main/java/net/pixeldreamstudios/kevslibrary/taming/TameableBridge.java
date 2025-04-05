package net.pixeldreamstudios.kevslibrary.taming;

import java.util.UUID;

public interface TameableBridge {
    UUID getOwnerUuid(); // must match name exactly
    boolean isTamed();
}
