package net.pixeldreamstudios.kevslibrary.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.pixeldreamstudios.kevslibrary.config.KevsLibraryConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(TridentEntity.class)
public abstract class TridentEntityMultistrikeMixin {
    @Redirect(
            method = "onEntityHit",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/Entity;damage(Lnet/minecraft/entity/damage/DamageSource;F)Z"
            )
    )
    private boolean kevslibrary$redirectTridentDamage(Entity target, DamageSource originalSource, float originalAmount) {
        if (!KevsLibraryConfig.getInstance().isMultistrikeEnabled()) {
            return target.damage(originalSource, originalAmount);
        }

        TridentEntity self = (TridentEntity)(Object)this;
        if (self.getCommandTags().contains("multistrike_arrow")) {
            Entity owner = self.getOwner();
            DamageSource ms = self.getDamageSources().trident(self, owner);
            float amount = (float)((PersistentProjectileEntity)self).getDamage();
            int prevRegen = target.timeUntilRegen;
            target.timeUntilRegen = 0;
            boolean hit = target.damage(ms, amount);
            target.timeUntilRegen = prevRegen;
            return hit;
        }
        return target.damage(originalSource, originalAmount);
    }
}