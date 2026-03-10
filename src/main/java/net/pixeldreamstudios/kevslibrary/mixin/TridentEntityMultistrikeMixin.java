package net.pixeldreamstudios.kevslibrary.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.pixeldreamstudios.kevslibrary.config.KevsLibraryConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(TridentEntity.class)
public abstract class TridentEntityMultistrikeMixin {
    @WrapOperation(
            method = "onEntityHit",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;damage(Lnet/minecraft/entity/damage/DamageSource;F)Z")
    )
    private boolean kevslibrary$wrapTridentDamage(
            Entity target,
            DamageSource source,
            float amount,
            Operation<Boolean> original
    ) {
        if (!KevsLibraryConfig.getInstance().isMultistrikeEnabled()) {
            return original.call(target, source, amount);
        }

        TridentEntity self = (TridentEntity)(Object)this;
        if (self.getCommandTags().contains("multistrike_arrow")) {
            Entity owner = self.getOwner();
            DamageSource ms = self.getDamageSources().trident(self, owner);
            float msAmount = (float) self.getDamage();
            int prevRegen = target.timeUntilRegen;
            target.timeUntilRegen = 0;
            boolean hit = original.call(target, ms, msAmount);
            target.timeUntilRegen = prevRegen;
            return hit;
        }

        return original.call(target, source, amount);
    }
}