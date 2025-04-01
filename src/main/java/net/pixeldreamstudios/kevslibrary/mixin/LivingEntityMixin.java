package net.pixeldreamstudios.kevslibrary.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.pixeldreamstudios.kevslibrary.KevsLibrary;
import net.pixeldreamstudios.kevslibrary.handler.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    private static final ThreadLocal<Float> CRIT_DAMAGE_TRACKER = new ThreadLocal<>();
    @Inject(method = "isInvulnerableTo", at = @At("HEAD"), cancellable = true)
    private void bypassMultistrike(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        if (source.getName().equals("multistrike") || source.getName().equals("multistrike_ranged")) {
            cir.setReturnValue(false);
        }
    }

    @ModifyVariable(
            method = "damage",
            at = @At("HEAD"),
            index = 2,
            argsOnly = true
    )
    private float applyCritToDamage(float amount, DamageSource source) {
        if (!(source.getAttacker() instanceof LivingEntity attacker)) {
            CRIT_DAMAGE_TRACKER.set(null);
            return amount;
        }

        if (source.getName().equals("multistrike") || source.getName().equals("multistrike_ranged")) {
            CRIT_DAMAGE_TRACKER.set(amount);
            return amount;
        }

        boolean isVanillaCrit = attacker instanceof PlayerEntity player &&
                player.fallDistance > 0.0F && !player.isOnGround();

        EntityAttributeInstance critChanceAttr = attacker.getAttributeInstance(KevsLibrary.CRIT_CHANCE);
        double critChance = critChanceAttr != null ? critChanceAttr.getValue() : 0.0;

        boolean isGroundedCrit = attacker.isOnGround() &&
                attacker.getRandom().nextFloat() < critChance;

        boolean isCrit = isVanillaCrit || isGroundedCrit;

        float finalDamage = amount;

        // 🔥 Apply universal damage multiplier
        EntityAttributeInstance dmgMultAttr = attacker.getAttributeInstance(KevsLibrary.DAMAGE);
        if (dmgMultAttr != null) {
            finalDamage *= (float) dmgMultAttr.getValue();
        }

        if (isCrit) {
            float critMultiplier = attacker.getAttributeInstance(KevsLibrary.CRIT_DAMAGE) != null
                    ? (float) attacker.getAttributeValue(KevsLibrary.CRIT_DAMAGE)
                    : 1.0f;

            finalDamage *= critMultiplier;

            // Only play sound/message if critMultiplier > 1.0
            if (critMultiplier > 1.0f && attacker instanceof PlayerEntity player2) {
                player2.getWorld().playSound(null, player2.getX(), player2.getY(), player2.getZ(),
                        SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 1.0f, 1.0f);
            }
        }

        CRIT_DAMAGE_TRACKER.set(finalDamage);
        return finalDamage;
    }

    @Inject(method = "damage", at = @At("RETURN"))
    private void onDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) return;
        if (OnHitEffectHandler.isInMultistrikeContext()) return;
        String name = source.getName();
        if (
                        name.equals("multistrike") ||
                        name.equals("multistrike_ranged") ||
                        name.equals("icicle") ||
                        name.equals("frost_nova") ||
                        name.equals("chain_lightning")
        ) return;

        if (!(source.getAttacker() instanceof LivingEntity attacker)) return;
        LivingEntity target = (LivingEntity)(Object) this;
        int depth;
        boolean overloadTriggered;
        boolean isRanged = source.getName().equals("arrow") || source.getName().equals("trident");

        PersistentProjectileEntity sourceProjectile = null;

        if (source.getSource() instanceof PersistentProjectileEntity proj) {
            sourceProjectile = proj;
            if (!MultistrikeHandler.tryMarkProjectile(proj.getUuid())) return;
        }

        float finalDamage = CRIT_DAMAGE_TRACKER.get() != null ? CRIT_DAMAGE_TRACKER.get() : amount;
        CRIT_DAMAGE_TRACKER.remove();

        PlayerEntity player = attacker instanceof PlayerEntity p ? p : null;
        ItemStack weaponUsed = player != null ? player.getMainHandStack().copy() : ItemStack.EMPTY;

        EntityAttributeInstance multistrikeChanceAttr = attacker.getAttributeInstance(KevsLibrary.MULTISTRIKE_CHANCE);
        double multistrikeChance = multistrikeChanceAttr != null ? multistrikeChanceAttr.getValue() : 0.0;
        if (attacker.getRandom().nextDouble() <= multistrikeChance) {
            if (isRanged && sourceProjectile != null) {
                MultistrikeHandler.spawnHoveringArrows(attacker, target, finalDamage, sourceProjectile, weaponUsed);
            } else {
                MultistrikeHandler.triggerMultistrike(attacker, target, finalDamage, weaponUsed);
            }
        }
        EntityAttributeInstance frostNovaAttr = attacker.getAttributeInstance(KevsLibrary.FROST_NOVA_CHANCE);
        double frostChance = frostNovaAttr != null ? frostNovaAttr.getValue() : 0.0;

        if (frostChance > 0.0 && attacker.getRandom().nextDouble() < frostChance) {
            FrostNovaHandler.triggerFrostNova(attacker);
        }


        EntityAttributeInstance lightningChanceAttr = attacker.getAttributeInstance(KevsLibrary.CHAIN_LIGHTNING_CHANCE);
        double lightningChance = lightningChanceAttr != null ? lightningChanceAttr.getValue() : 0.0;

        if (lightningChance > 0.0) {
            double roll = attacker.getRandom().nextDouble();

            if (roll < lightningChance) {
                ChainLightningHandler.spawnChainLightning(attacker, target);


            }
        }
        EntityAttributeInstance fireTornadoAttr = attacker.getAttributeInstance(KevsLibrary.FIRE_TORNADO_CHANCE);
        double fireTornadoChance = fireTornadoAttr != null ? fireTornadoAttr.getValue() : 0.0;

        if (fireTornadoChance > 0.0 && attacker.getRandom().nextDouble() < fireTornadoChance) {
            FireTornadoHandler.spawnFireTornado(attacker, target);
        }

    }
}
