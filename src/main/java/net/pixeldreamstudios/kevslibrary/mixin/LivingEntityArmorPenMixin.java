package net.pixeldreamstudios.kevslibrary.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.damage.DamageSource;
import net.pixeldreamstudios.kevslibrary.KevsLibrary;
import net.pixeldreamstudios.kevslibrary.config.KevsLibraryConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntity.class)
public abstract class LivingEntityArmorPenMixin {

    @WrapOperation(
            method = "applyArmorToDamage",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/DamageUtil;getDamageLeft(Lnet/minecraft/entity/LivingEntity;FLnet/minecraft/entity/damage/DamageSource;FF)F")
    )
    private float kevslibrary$applyArmorPenetration(
            LivingEntity entity,
            float damage,
            DamageSource source,
            float armor,
            float toughness,
            Operation<Float> original
    ) {
        KevsLibraryConfig config = KevsLibraryConfig.getInstance();

        if (config.isArmorPenetrationEnabled() && source.getAttacker() instanceof LivingEntity attacker) {
            double flatPen = 0.0;
            double percentPen = 0.0;

            EntityAttributeInstance flatAttr = attacker.getAttributeInstance(KevsLibrary.ARMOR_PENETRATION_FLAT);
            if (flatAttr != null) flatPen = flatAttr.getValue();

            EntityAttributeInstance percentAttr = attacker.getAttributeInstance(KevsLibrary.ARMOR_PENETRATION);
            if (percentAttr != null) percentPen = percentAttr.getValue();

            percentPen = Math.max(0.0, Math.min(1.0, percentPen));

            float effectiveArmor = Math.max(armor - (float) flatPen, 0f);
            effectiveArmor *= (1.0f - (float) percentPen);

            armor = effectiveArmor;
        }

        return original.call(entity, damage, source, armor, toughness);
    }
}