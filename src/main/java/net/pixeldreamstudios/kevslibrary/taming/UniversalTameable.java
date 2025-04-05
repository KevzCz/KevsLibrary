package net.pixeldreamstudios.kevslibrary.taming;

import java.util.UUID;

public interface UniversalTameable {
    UUID kevslib$getOwnerUuid();
    void kevslib$setOwnerUuid(UUID uuid);
    boolean kevslib$isTamed();
}
