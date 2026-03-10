package net.pixeldreamstudios.kevslibrary.mixin;

import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LivingEntity.class)
public interface PlayerEntityAccessor {
    @Accessor("lastAttackedTicks")
    int getLastAttackedTicks();

    @Accessor("lastAttackedTicks")
    void setLastAttackedTicks(int ticks);
}