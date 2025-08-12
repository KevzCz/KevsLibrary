package net.pixeldreamstudios.kevslibrary.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.pixeldreamstudios.kevslibrary.KevsLibrary;
import net.pixeldreamstudios.kevslibrary.handler.*;
import net.spell_engine.entity.SpellProjectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@SuppressWarnings("unused")
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    private static final ThreadLocal<Boolean> IN_CLEAVE_CONTEXT = ThreadLocal.withInitial(() -> false);

    private static final ThreadLocal<Float> CRIT_DAMAGE_TRACKER = new ThreadLocal<>();
    @Inject(method = "isInvulnerableTo", at = @At("HEAD"), cancellable = true)
    private void bypassMultistrike(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        if (source.getName().equals("multistrike") || source.getName().equals("multistrike_ranged")) {
            cir.setReturnValue(false);
        }
    }
/*
    @Inject(method = "damage", at = @At("HEAD"))
    private void debugProjectileHits(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        Entity attacker = source.getAttacker();
        Entity actualSource = source.getSource();
        LivingEntity self = (LivingEntity) (Object) this;

        if (self.getWorld().isClient()) return;

        self.getWorld().getEntitiesByClass(
                SpellProjectile.class,
                self.getBoundingBox().expand(2.5),
                proj -> proj.getOwner() != self
        ).forEach(proj -> {
            System.out.println("[DEBUG] Nearby SpellProjectile:");
            System.out.println("  -> Pos: " + proj.getPos());
            System.out.println("  -> Owner: " + (proj.getOwner() != null ? proj.getOwner().getName().getString() : "null"));
            System.out.println("  -> Spell ID: " + proj.getSpellEntry());
            System.out.println("  -> Command Tags: " + proj.getCommandTags());
        });
    }
*/


/*
    @Inject(method = "damage", at = @At("HEAD"))
    private void captureRawDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        Entity attacker = source.getAttacker();
        System.out.println("[DEBUG] Raw damage entry: amount = " + amount +
                ", attacker = " + (attacker != null ? attacker.getName().getString() : "null") +
                ", source = " + source.getName());

        if (!(attacker instanceof LivingEntity livingAttacker)) return;
        LivingEntity target = (LivingEntity)(Object) this;

        ThornsHandler.tryReflectThorns(target, livingAttacker, amount);
    }
*/
    @Inject(method = "createLivingAttributes", at = @At("RETURN"))
    private static void injectGlobalAttributes(CallbackInfoReturnable<DefaultAttributeContainer.Builder> cir) {
        DefaultAttributeContainer.Builder builder = cir.getReturnValue();

        builder
                .add(KevsLibrary.CRIT_CHANCE, 0.0)
                .add(KevsLibrary.CRIT_DAMAGE, 1.5)
                .add(KevsLibrary.MULTISTRIKE_CHANCE, 0.0)
                .add(KevsLibrary.MULTISTRIKE_COUNT, 2.0)
                .add(KevsLibrary.MULTISTRIKE_DAMAGE, 0.5)
                .add(KevsLibrary.DAMAGE, 1)
                .add(KevsLibrary.CHAIN_LIGHTNING_CHANCE, 0)
                .add(KevsLibrary.CHAIN_LIGHTNING_COUNT, 3.0)
                .add(KevsLibrary.CHAIN_LIGHTNING_OVERLOAD_CHANCE, 0.0)
                .add(KevsLibrary.FIRE_TORNADO_CHANCE, 0)
                .add(KevsLibrary.FIRE_TORNADO_OVERLOAD_CHANCE, 0.0)
                .add(KevsLibrary.FROST_NOVA_CHANCE, 0)
                .add(KevsLibrary.FROST_NOVA_COUNT, 3)
                .add(KevsLibrary.FROST_NOVA_OVERLOAD_CHANCE, 0.0)
                .add(KevsLibrary.SOUL_LINK_CHANCE, 0)
                .add(KevsLibrary.SOUL_LINK_DAMAGE, 1)
                .add(KevsLibrary.ARCANE_RUPTURE_CHANCE, 0)
                .add(KevsLibrary.ARCANE_RUPTURE_DAMAGE, 1)
                .add(KevsLibrary.ARCANE_RUPTURE_OVERLOAD_CHANCE, 0)
                .add(KevsLibrary.TRIDENT_DAMAGE_MULTIPLIER, 1.0)
                .add(KevsLibrary.ARMOR_PENETRATION, 0.0)
                .add(KevsLibrary.ARMOR_PENETRATION_FLAT, 0.0)
                .add(KevsLibrary.THORNS_CHANCE, 0.0)
                .add(KevsLibrary.THORNS_AMP, 0.0)
                .add(KevsLibrary.THORNS_TRUE_DAMAGE_CHANCE, 0.0)
                .add(KevsLibrary.CLEAVE_DAMAGE_MULTIPLIER, 1.0)
                .add(KevsLibrary.CLEAVE_RANGE, 4.0)
                .add(KevsLibrary.CLEAVE_CHANCE, 0)
                .add(KevsLibrary.PIERCING_CHANCE, 0.0)
        ;
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
        if (source.getName().equals("trident")) {
            EntityAttributeInstance tridentMultiplierAttr = attacker.getAttributeInstance(KevsLibrary.TRIDENT_DAMAGE_MULTIPLIER);
            if (tridentMultiplierAttr != null) {
                amount *= tridentMultiplierAttr.getValue();
            }
        }
        boolean isVanillaCrit = attacker instanceof PlayerEntity player &&
                player.fallDistance > 0.0F && !player.isOnGround();


        EntityAttributeInstance critChanceAttr = attacker.getAttributeInstance(KevsLibrary.CRIT_CHANCE);
        double critChance = critChanceAttr != null ? critChanceAttr.getValue() : 0.0;
        boolean isGroundedCrit = attacker.isOnGround() &&
                attacker.getRandom().nextFloat() < critChance;


        boolean isCrit = isVanillaCrit || isGroundedCrit;

        float finalDamage = amount;
        LivingEntity target = (LivingEntity)(Object) this;

        if (isVanillaCrit) {
            finalDamage /= 1.5f;
        }


        EntityAttributeInstance dmgMultAttr = attacker.getAttributeInstance(KevsLibrary.DAMAGE);
        if (dmgMultAttr != null) {
            finalDamage *= (float) dmgMultAttr.getValue();
        }

        if (isCrit) {
            float critMultiplier = attacker.getAttributeInstance(KevsLibrary.CRIT_DAMAGE) != null
                    ? (float) attacker.getAttributeValue(KevsLibrary.CRIT_DAMAGE)
                    : 1.0f;

            finalDamage *= critMultiplier;

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
                        name.equals("chain_lightning") ||
                        name.equals("fire_tornado")

        ) return;
        if (source.getSource() instanceof PersistentProjectileEntity pp && pp.getCommandTags().contains("multistrike_arrow")) return;
        if (source.getAttacker() instanceof SpellProjectile spell) {
            if (
                    spell.getCommandTags().contains("real_spell_projectile") ||
                            spell.getCommandTags().contains("multistrike_spell")
            ) {
                return;
            }
        }


        if (!(source.getAttacker() instanceof LivingEntity attacker)) return;
        LivingEntity target = (LivingEntity)(Object) this;

        if (!target.getWorld().isClient()) {
            boolean nearMultistrikeSpell = target.getWorld()
                    .getEntitiesByClass(SpellProjectile.class, target.getBoundingBox().expand(1.5), spell ->
                            spell.getCommandTags().contains("multistrike_spell") &&
                                    spell.getOwner() == attacker
                    )
                    .size() > 0;

            if (nearMultistrikeSpell) {
                return;
            }
        }



        PersistentProjectileEntity sourceProjectile = null;

        if (source.getSource() instanceof PersistentProjectileEntity proj) {
            sourceProjectile = proj;
            if (!MultistrikeHandler.tryMarkProjectile(proj.getUuid())) return;
        }

        float finalDamage = CRIT_DAMAGE_TRACKER.get() != null ? CRIT_DAMAGE_TRACKER.get() : amount;
        ThornsHandler.tryReflectThorns(target, attacker, amount);
        CRIT_DAMAGE_TRACKER.remove();

        PlayerEntity player = attacker instanceof PlayerEntity p ? p : null;
        ItemStack weaponUsed = player != null ? player.getMainHandStack().copy() : ItemStack.EMPTY;

        EntityAttributeInstance multistrikeChanceAttr = attacker.getAttributeInstance(KevsLibrary.MULTISTRIKE_CHANCE);
        double multistrikeChance = multistrikeChanceAttr != null ? multistrikeChanceAttr.getValue() : 0.0;
        if (attacker.getRandom().nextDouble() <= multistrikeChance) {
            boolean hasNearbyRealSpellProjectile = target.getWorld().getEntitiesByClass(
                    SpellProjectile.class,
                    target.getBoundingBox().expand(3.0),
                    proj -> {
                        boolean isOwned = proj.getOwner() != null;
                        boolean hasSpell = proj.getSpellEntry() != null;
                        boolean isLikelySpell = !proj.getCommandTags().isEmpty();

                        boolean isNotMultistrike = !proj.getCommandTags().contains("multistrike_spell");
                        return isOwned && isNotMultistrike && (hasSpell || isLikelySpell);
                    }
            ).size() > 0;

            if (hasNearbyRealSpellProjectile) {
//                System.out.println("[DEBUG] Skipping multistrike: nearby real spell projectile detected.");
                return;
            }

            if (source.getSource() != null) {
                float msBase = finalDamage;
                if (source.getSource() instanceof net.minecraft.entity.projectile.TridentEntity) {
                    EntityAttributeInstance triAttr = attacker.getAttributeInstance(KevsLibrary.TRIDENT_DAMAGE_MULTIPLIER);
                    float triMul = triAttr != null ? (float) triAttr.getValue() : 1.0f;
                    if (triMul != 0.0f) {
                        msBase = finalDamage / triMul;
                    }
                }
                EntityAttributeInstance dmgAttr0 = attacker.getAttributeInstance(KevsLibrary.DAMAGE);
                float dmgMul0 = dmgAttr0 != null ? (float) dmgAttr0.getValue() : 1.0f;
                if (dmgMul0 != 0.0f) {
                    msBase = msBase / dmgMul0;
                }
                MultistrikeHandler.spawnHoveringProjectiles(attacker, target, msBase, source.getSource(), weaponUsed);

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
        EntityAttributeInstance petInRaAttr = attacker.getAttributeInstance(KevsLibrary.PET_INHERITANCE_RATIO);
        double petInheritanceRatio = petInRaAttr != null ? petInRaAttr.getValue() : 0.0;

        EntityAttributeInstance soulLinkAttr = attacker.getAttributeInstance(KevsLibrary.SOUL_LINK_CHANCE);
        double soulLinkChance = soulLinkAttr != null ? soulLinkAttr.getValue() : 0.0;

        if (soulLinkChance > 0.0 && attacker.getRandom().nextDouble() < soulLinkChance) {
            SoulLinkHandler.triggerSoulLink(attacker, target);
        }
        SoulLinkTracker.getGroup(target).ifPresent(linkData -> {
            if (!attacker.getUuid().equals(linkData.attacker().getUuid())) return;

            SoulLinkHandler.handleLinkedDamage(
                    linkData.attacker(),
                    target,
                    finalDamage,
                    linkData.group(),
                    linkData.soulPower()
            );
        });
        SoulLinkTracker.getGroup(target).ifPresent(linkData -> {
            if (attacker.getRandom().nextDouble() < soulLinkChance) {
                SoulLinkHandler.tryExtendLink(attacker, target);
            }
        });
        EntityAttributeInstance arcaneChanceAttr = attacker.getAttributeInstance(KevsLibrary.ARCANE_RUPTURE_CHANCE);
        double arcaneChance = arcaneChanceAttr != null ? arcaneChanceAttr.getValue() : 0.0;

        if (
                arcaneChance > 0.0 &&
                        attacker.getRandom().nextDouble() < arcaneChance &&
                        !source.getName().equals("arcane_shard")
        ) {
            ArcaneRuptureHandler.trigger(attacker, target);
        }
        if (!IN_CLEAVE_CONTEXT.get() && source.getSource() == attacker) {
            IN_CLEAVE_CONTEXT.set(true);
            CleaveHandler.triggerCleave(attacker, finalDamage);
            IN_CLEAVE_CONTEXT.set(false);
        }
        if (PiercingHandler.isPiercingActive(attacker)) {
            PiercingHandler.consumePiercing(attacker);
            float pierceDamage = finalDamage * 0.75f;
            PiercingHandler.applyLineDamage(attacker, pierceDamage, 6.0f, 1.0f);
        } else {
            PiercingHandler.tryActivatePiercing(attacker);
        }
    }

}
