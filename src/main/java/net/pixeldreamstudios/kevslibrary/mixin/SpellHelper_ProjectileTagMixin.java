package net.pixeldreamstudios.kevslibrary.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.World;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.entity.SpellProjectile;
import net.spell_engine.internals.SpellHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SuppressWarnings("unused")
@Mixin(SpellHelper.class)
public class SpellHelper_ProjectileTagMixin {

    @Inject(
            method = "shootProjectile(Lnet/minecraft/world/World;Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/entity/Entity;Lnet/minecraft/registry/entry/RegistryEntry;Lnet/spell_engine/internals/SpellHelper$ImpactContext;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/World;spawnEntity(Lnet/minecraft/entity/Entity;)Z",
                    shift = At.Shift.AFTER
            )
    )
    private static void tagRealSpellProjectile(
            World world,
            LivingEntity caster,
            Entity target,
            RegistryEntry<Spell> spellEntry,
            SpellHelper.ImpactContext context,
            int sequenceIndex,
            CallbackInfo ci
    ) {
        world.getEntitiesByClass(SpellProjectile.class,
                        caster.getBoundingBox().expand(2.5),
                        proj -> proj.getOwner() == caster && proj.age < 5)
                .forEach(spellProjectile -> {
                    spellProjectile.addCommandTag("real_spell_projectile");
                });
    }
}
