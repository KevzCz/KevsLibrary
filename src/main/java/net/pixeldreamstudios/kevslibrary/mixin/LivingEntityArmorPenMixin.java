package net.pixeldreamstudios.kevslibrary.mixin;

import net.minecraft.entity.DamageUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.registry.tag.DamageTypeTags;
import net.pixeldreamstudios.kevslibrary.KevsLibrary;
import net.pixeldreamstudios.kevslibrary.config.KevsLibraryConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityArmorPenMixin {

    @Inject(
            method = "applyArmorToDamage",
            at = @At("HEAD"),
            cancellable = true
    )
    private void kevslibrary$applyArmorToDamage(DamageSource source, float amount, CallbackInfoReturnable<Float> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        KevsLibraryConfig config = KevsLibraryConfig.getInstance();

        if (!source.isIn(DamageTypeTags.BYPASSES_ARMOR)) {
            self.damageArmor(source, amount);

            float armor = self.getArmor();
            float toughness = (float) self.getAttributeValue(EntityAttributes.GENERIC_ARMOR_TOUGHNESS);

            if (config.isArmorPenetrationEnabled() && source.getAttacker() instanceof LivingEntity attacker) {
                double flatPen = 0.0;
                double percentPen = 0.0;

                EntityAttributeInstance flatAttr = attacker.getAttributeInstance(KevsLibrary.ARMOR_PENETRATION_FLAT);
                if (flatAttr != null) flatPen = flatAttr.getValue();

                EntityAttributeInstance percentAttr = attacker.getAttributeInstance(KevsLibrary.ARMOR_PENETRATION);
                if (percentAttr != null) percentPen = percentAttr.getValue();

                if (percentPen < 0.0) percentPen = 0.0;
                if (percentPen > 1.0) percentPen = 1.0;

                float effectiveArmor = Math.max(armor - (float) flatPen, 0f);
                effectiveArmor *= (1.0f - (float) percentPen);

                armor = effectiveArmor;
            }

            amount = DamageUtil.getDamageLeft(self, amount, source, armor, toughness);
        }

        cir.setReturnValue(amount);
    }
}