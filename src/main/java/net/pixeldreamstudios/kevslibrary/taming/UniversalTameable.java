package net.pixeldreamstudios.kevslibrary.taming;

import net.minecraft.nbt.NbtCompound;

import java.util.UUID;

public interface UniversalTameable {
    UUID kevslib$getOwnerUuid();
    void kevslib$setOwnerUuid(UUID uuid);
    boolean kevslib$isTamed();
    NbtCompound kevslib$getInheritanceData();

    // ✅ Add this:
    void kevslib$setInheritanceData(NbtCompound tag);
}

