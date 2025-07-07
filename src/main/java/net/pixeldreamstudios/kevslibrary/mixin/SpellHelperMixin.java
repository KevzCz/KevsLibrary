package net.pixeldreamstudios.kevslibrary.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.pixeldreamstudios.kevslibrary.KevsLibrary;
import net.pixeldreamstudios.kevslibrary.handler.MultistrikeHandler;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.internals.SpellHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@SuppressWarnings("unused")
@Mixin(SpellHelper.class)
public class SpellHelperMixin {
    @Inject(
            method = "projectileImpact",
            at = @At("TAIL"),
            cancellable = false
    )
    private static void onProjectileImpact(
            LivingEntity caster,
            Entity projectile,
            Entity target,
            RegistryEntry<Spell> spell,
            SpellHelper.ImpactContext context,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!(target instanceof LivingEntity targetLiving)) return;

        if (projectile.getCommandTags().contains("multistrike_spell")) {
            return;
        }

        EntityAttributeInstance attrInstance = caster.getAttributeInstance(KevsLibrary.MULTISTRIKE_CHANCE);
        double multistrikeChance = attrInstance != null ? attrInstance.getValue() : 0.0;

        if (caster.getRandom().nextDouble() <= multistrikeChance) {
            MultistrikeHandler.spawnHoveringProjectiles(
                    caster,
                    targetLiving,
                    (float) context.power().nonCriticalValue(),
                    projectile,
                    ItemStack.EMPTY
            );
        }
    }
}


