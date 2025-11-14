package net.pixeldreamstudios.kevslibrary.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.pixeldreamstudios.kevslibrary.KevsLibrary;
import net.pixeldreamstudios.kevslibrary.api.CritEvents;
import net.pixeldreamstudios.kevslibrary.config.KevsLibraryConfig;
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
    private static final ThreadLocal<Boolean> IS_CRIT_FLAG = new ThreadLocal<>();

    @Inject(method = "isInvulnerableTo", at = @At("HEAD"), cancellable = true)
    private void bypassMultistrike(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        if (source.getName().equals("multistrike") || source.getName().equals("multistrike_ranged")) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "createLivingAttributes", at = @At("RETURN"))
    private static void injectGlobalAttributes(CallbackInfoReturnable<DefaultAttributeContainer.Builder> cir) {
        KevsLibraryConfig config = KevsLibraryConfig.getInstance();
        DefaultAttributeContainer.Builder builder = cir.getReturnValue();

        if (config.isAttributeEnabled("crit_chance") && KevsLibrary.CRIT_CHANCE != null)
            builder.add(KevsLibrary.CRIT_CHANCE, 0.0);

        if (config.isAttributeEnabled("crit_damage") && KevsLibrary.CRIT_DAMAGE != null)
            builder.add(KevsLibrary.CRIT_DAMAGE, 1.5);

        if (config.isAttributeEnabled("multistrike_chance") && KevsLibrary.MULTISTRIKE_CHANCE != null)
            builder.add(KevsLibrary.MULTISTRIKE_CHANCE, 0.0);

        if (config.isAttributeEnabled("multistrike_count") && KevsLibrary.MULTISTRIKE_COUNT != null)
            builder.add(KevsLibrary.MULTISTRIKE_COUNT, 2.0);

        if (config.isAttributeEnabled("multistrike_damage") && KevsLibrary.MULTISTRIKE_DAMAGE != null)
            builder.add(KevsLibrary.MULTISTRIKE_DAMAGE, 0.5);

        if (config.isAttributeEnabled("damage") && KevsLibrary.DAMAGE != null)
            builder.add(KevsLibrary.DAMAGE, 1);

        if (config.isAttributeEnabled("chain_lightning_chance") && KevsLibrary.CHAIN_LIGHTNING_CHANCE != null)
            builder.add(KevsLibrary.CHAIN_LIGHTNING_CHANCE, 0);

        if (config.isAttributeEnabled("chain_lightning_count") && KevsLibrary.CHAIN_LIGHTNING_COUNT != null)
            builder.add(KevsLibrary.CHAIN_LIGHTNING_COUNT, 3.0);

        if (config.isAttributeEnabled("chain_lightning_overload_chance") && KevsLibrary.CHAIN_LIGHTNING_OVERLOAD_CHANCE != null)
            builder.add(KevsLibrary.CHAIN_LIGHTNING_OVERLOAD_CHANCE, 0.0);

        if (config.isAttributeEnabled("fire_tornado_chance") && KevsLibrary.FIRE_TORNADO_CHANCE != null)
            builder.add(KevsLibrary.FIRE_TORNADO_CHANCE, 0);

        if (config.isAttributeEnabled("fire_tornado_overload_chance") && KevsLibrary.FIRE_TORNADO_OVERLOAD_CHANCE != null)
            builder.add(KevsLibrary.FIRE_TORNADO_OVERLOAD_CHANCE, 0.0);

        if (config.isAttributeEnabled("frost_nova_chance") && KevsLibrary.FROST_NOVA_CHANCE != null)
            builder.add(KevsLibrary.FROST_NOVA_CHANCE, 0);

        if (config.isAttributeEnabled("frost_nova_count") && KevsLibrary.FROST_NOVA_COUNT != null)
            builder.add(KevsLibrary.FROST_NOVA_COUNT, 3);

        if (config.isAttributeEnabled("frost_nova_overload_chance") && KevsLibrary.FROST_NOVA_OVERLOAD_CHANCE != null)
            builder.add(KevsLibrary.FROST_NOVA_OVERLOAD_CHANCE, 0.0);

        if (config.isAttributeEnabled("soul_link_chance") && KevsLibrary.SOUL_LINK_CHANCE != null)
            builder.add(KevsLibrary.SOUL_LINK_CHANCE, 0);

        if (config.isAttributeEnabled("soul_link_damage") && KevsLibrary.SOUL_LINK_DAMAGE != null)
            builder.add(KevsLibrary.SOUL_LINK_DAMAGE, 1);

        if (config.isAttributeEnabled("arcane_rupture_chance") && KevsLibrary.ARCANE_RUPTURE_CHANCE != null)
            builder.add(KevsLibrary.ARCANE_RUPTURE_CHANCE, 0);

        if (config.isAttributeEnabled("arcane_rupture_damage") && KevsLibrary.ARCANE_RUPTURE_DAMAGE != null)
            builder.add(KevsLibrary.ARCANE_RUPTURE_DAMAGE, 1);

        if (config.isAttributeEnabled("arcane_rupture_overload_chance") && KevsLibrary.ARCANE_RUPTURE_OVERLOAD_CHANCE != null)
            builder.add(KevsLibrary.ARCANE_RUPTURE_OVERLOAD_CHANCE, 0);

        if (config.isAttributeEnabled("trident_damage_multiplier") && KevsLibrary.TRIDENT_DAMAGE_MULTIPLIER != null)
            builder.add(KevsLibrary.TRIDENT_DAMAGE_MULTIPLIER, 1.0);

        if (config.isAttributeEnabled("armor_penetration") && KevsLibrary.ARMOR_PENETRATION != null)
            builder.add(KevsLibrary.ARMOR_PENETRATION, 0.0);

        if (config.isAttributeEnabled("armor_penetration_flat") && KevsLibrary.ARMOR_PENETRATION_FLAT != null)
            builder.add(KevsLibrary.ARMOR_PENETRATION_FLAT, 0.0);

        if (config.isAttributeEnabled("thorns_chance") && KevsLibrary.THORNS_CHANCE != null)
            builder.add(KevsLibrary.THORNS_CHANCE, 0.0);

        if (config.isAttributeEnabled("thorns_amp") && KevsLibrary.THORNS_AMP != null)
            builder.add(KevsLibrary.THORNS_AMP, 0.0);

        if (config.isAttributeEnabled("thorns_true_damage_chance") && KevsLibrary.THORNS_TRUE_DAMAGE_CHANCE != null)
            builder.add(KevsLibrary.THORNS_TRUE_DAMAGE_CHANCE, 0.0);

        if (config.isAttributeEnabled("cleave_damage_multiplier") && KevsLibrary.CLEAVE_DAMAGE_MULTIPLIER != null)
            builder.add(KevsLibrary.CLEAVE_DAMAGE_MULTIPLIER, 1.0);

        if (config.isAttributeEnabled("cleave_range") && KevsLibrary.CLEAVE_RANGE != null)
            builder.add(KevsLibrary.CLEAVE_RANGE, 4.0);

        if (config.isAttributeEnabled("cleave_chance") && KevsLibrary.CLEAVE_CHANCE != null)
            builder.add(KevsLibrary.CLEAVE_CHANCE, 0);

        if (config.isAttributeEnabled("pierce_chance") && KevsLibrary.PIERCING_CHANCE != null)
            builder.add(KevsLibrary.PIERCING_CHANCE, 0.0);

        if (config.isAttributeEnabled("barrage_chance") && KevsLibrary.BARRAGE_CHANCE != null)
            builder.add(KevsLibrary.BARRAGE_CHANCE, 0.0);

        if (config.isAttributeEnabled("projectile_storm_chance") && KevsLibrary.PROJECTILE_STORM_CHANCE != null)
            builder.add(KevsLibrary.PROJECTILE_STORM_CHANCE, 0.0);

        if (config.isAttributeEnabled("projectile_storm_range") && KevsLibrary.PROJECTILE_STORM_RANGE != null)
            builder.add(KevsLibrary.PROJECTILE_STORM_RANGE, 5);

        if (config.isAttributeEnabled("projectile_storm_duration") && KevsLibrary.PROJECTILE_STORM_DURATION != null)
            builder.add(KevsLibrary.PROJECTILE_STORM_DURATION, 60.0);

        if (config.isAttributeEnabled("hunger_consumption") && KevsLibrary.HUNGER_CONSUMPTION != null)
            builder.add(KevsLibrary.HUNGER_CONSUMPTION, 1.0);
    }

    @ModifyVariable(method = "damage", at = @At("HEAD"), index = 2, argsOnly = true)
    private float applyCritToDamage(float amount, DamageSource source) {
        KevsLibraryConfig config = KevsLibraryConfig.getInstance();

        if (!(source.getAttacker() instanceof LivingEntity attacker)) {
            CRIT_DAMAGE_TRACKER.set(null);
            IS_CRIT_FLAG.set(null);
            return amount;
        }

        if (source.getName().equals("multistrike") || source.getName().equals("multistrike_ranged")) {
            CRIT_DAMAGE_TRACKER.set(amount);
            IS_CRIT_FLAG.set(false);
            return amount;
        }

        if (config.isTridentEnabled() && source.getName().equals("trident")) {
            EntityAttributeInstance tridentMultiplierAttr = attacker.getAttributeInstance(KevsLibrary.TRIDENT_DAMAGE_MULTIPLIER);
            if (tridentMultiplierAttr != null) {
                amount *= tridentMultiplierAttr.getValue();
            }
        }

        TagKey<DamageType> magicTag = TagKey.of(RegistryKeys.DAMAGE_TYPE, Identifier.of("c", "is_magic"));
        boolean allowCrit = !source.isIn(magicTag) && config.isCritEnabled();

        boolean isVanillaCrit = false;
        boolean isGroundedCrit = false;

        if (allowCrit) {
            if (attacker instanceof PlayerEntity player) {
                isVanillaCrit = player.fallDistance > 0.0F && !player.isOnGround();
            }

                EntityAttributeInstance critChanceAttr = attacker.getAttributeInstance(KevsLibrary.CRIT_CHANCE);
                double critChance = critChanceAttr != null ? critChanceAttr.getValue() : 0.0;
                isGroundedCrit = attacker.isOnGround() && attacker.getRandom().nextFloat() < critChance;

        }

        boolean isCrit = isVanillaCrit || isGroundedCrit;

        float finalDamage = amount;

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
        IS_CRIT_FLAG.set(isCrit);
        return finalDamage;
    }

    @Inject(method = "damage", at = @At("RETURN"))
    private void onDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) {
            CRIT_DAMAGE_TRACKER.remove();
            IS_CRIT_FLAG.remove();
            return;
        }

        LivingEntity target = (LivingEntity)(Object) this;
        KevsLibraryConfig config = KevsLibraryConfig.getInstance();

        if (OnHitEffectHandler.isInMultistrikeContext()) {
            CRIT_DAMAGE_TRACKER.remove();
            IS_CRIT_FLAG.remove();
            return;
        }

        String name = source.getName();
        if (name.equals("multistrike") ||
                name.equals("multistrike_ranged") ||
                name.equals("icicle") ||
                name.equals("frost_nova") ||
                name.equals("chain_lightning") ||
                name.equals("fire_tornado")) {
            CRIT_DAMAGE_TRACKER.remove();
            IS_CRIT_FLAG.remove();
            return;
        }

        if (source.getSource() instanceof PersistentProjectileEntity pp && pp.getCommandTags().contains("multistrike_arrow")) {
            CRIT_DAMAGE_TRACKER.remove();
            IS_CRIT_FLAG.remove();
            return;
        }

        if (source.getAttacker() instanceof SpellProjectile spell) {
            if (spell.getCommandTags().contains("real_spell_projectile") || spell.getCommandTags().contains("multistrike_spell")) {
                CRIT_DAMAGE_TRACKER.remove();
                IS_CRIT_FLAG.remove();
                return;
            }
        }

        if (!(source.getAttacker() instanceof LivingEntity attacker)) {
            CRIT_DAMAGE_TRACKER.remove();
            IS_CRIT_FLAG.remove();
            return;
        }

        if (!target.getWorld().isClient()) {
            boolean nearMultistrikeSpell = target.getWorld()
                    .getEntitiesByClass(SpellProjectile.class, target.getBoundingBox().expand(1.5), s ->
                            s.getCommandTags().contains("multistrike_spell") && s.getOwner() == attacker)
                    .size() > 0;
            if (nearMultistrikeSpell) {
                CRIT_DAMAGE_TRACKER.remove();
                IS_CRIT_FLAG.remove();
                return;
            }
        }

        if (source.getSource() instanceof PersistentProjectileEntity proj) {
            if (!MultistrikeHandler.tryMarkProjectile(proj.getUuid())) {
                CRIT_DAMAGE_TRACKER.remove();
                IS_CRIT_FLAG.remove();
                return;
            }
        }

        float finalDamage = CRIT_DAMAGE_TRACKER.get() != null ? CRIT_DAMAGE_TRACKER.get() : amount;
        boolean isCrit = IS_CRIT_FLAG.get() != null && IS_CRIT_FLAG.get();

        if (config.isThornsEnabled()) {
            ThornsHandler.tryReflectThorns(target, attacker, amount);
        }

        if (config.isCritEnabled()) {
            CritEvents.CRIT.invoker().onCrit(new CritEvents.Context(
                    target, attacker, source, finalDamage, isCrit
            ));
        }

        CRIT_DAMAGE_TRACKER.remove();
        IS_CRIT_FLAG.remove();

        PlayerEntity player = attacker instanceof PlayerEntity p ? p : null;
        ItemStack weaponUsed = player != null ? player.getMainHandStack().copy() : ItemStack.EMPTY;

        boolean stormHit = false;
        if (config.isProjectileStormEnabled()) {
            if (source.getSource() instanceof PersistentProjectileEntity pp2 && ProjectileStormHandler.isStormTag(pp2)) {
                stormHit = true;
            } else if (source.getSource() instanceof SpellProjectile sp2 && ProjectileStormHandler.isStormTag(sp2)) {
                stormHit = true;
            }
        }

        if (!stormHit && config.isMultistrikeEnabled()) {
            EntityAttributeInstance multistrikeChanceAttr = attacker.getAttributeInstance(KevsLibrary.MULTISTRIKE_CHANCE);
            double multistrikeChance = multistrikeChanceAttr != null ? multistrikeChanceAttr.getValue() : 0.0;

            if (attacker.getRandom().nextDouble() <= multistrikeChance) {
                boolean hasNearbyRealSpellProjectile = target.getWorld().getEntitiesByClass(
                        SpellProjectile.class,
                        target.getBoundingBox().expand(3.0),
                        proj2 -> {
                            boolean isOwned = proj2.getOwner() != null;
                            boolean hasSpell = proj2.getSpellEntry() != null;
                            boolean isLikelySpell = !proj2.getCommandTags().isEmpty();
                            boolean isNotMultistrike = !proj2.getCommandTags().contains("multistrike_spell");
                            return isOwned && isNotMultistrike && (hasSpell || isLikelySpell);
                        }
                ).size() > 0;

                if (!hasNearbyRealSpellProjectile) {
                    if (source.getSource() != null) {
                        float msBase = finalDamage;

                        if (source.getSource() instanceof net.minecraft.entity.projectile.TridentEntity) {
                            EntityAttributeInstance triAttr = attacker.getAttributeInstance(KevsLibrary.TRIDENT_DAMAGE_MULTIPLIER);
                            float triMul = triAttr != null ? (float) triAttr.getValue() : 1.0f;
                            if (triMul != 0.0f) msBase = finalDamage / triMul;
                        }

                        EntityAttributeInstance dmgAttr0 = attacker.getAttributeInstance(KevsLibrary.DAMAGE);
                        float dmgMul0 = dmgAttr0 != null ? (float) dmgAttr0.getValue() : 1.0f;
                        if (dmgMul0 != 0.0f) msBase = msBase / dmgMul0;

                        MultistrikeHandler.spawnHoveringProjectiles(attacker, target, msBase, source.getSource(), weaponUsed);
                    } else {
                        MultistrikeHandler.triggerMultistrike(attacker, target, finalDamage, weaponUsed);
                    }
                }
            }
        }

        if (config.isFrostNovaEnabled()) {
            EntityAttributeInstance frostNovaAttr = attacker.getAttributeInstance(KevsLibrary.FROST_NOVA_CHANCE);
            double frostChance = frostNovaAttr != null ? frostNovaAttr.getValue() : 0.0;
            if (frostChance > 0.0 && attacker.getRandom().nextDouble() < frostChance) {
                FrostNovaHandler.triggerFrostNova(attacker);
            }
        }

        if (config.isChainLightningEnabled()) {
            EntityAttributeInstance lightningChanceAttr = attacker.getAttributeInstance(KevsLibrary.CHAIN_LIGHTNING_CHANCE);
            double lightningChance = lightningChanceAttr != null ? lightningChanceAttr.getValue() : 0.0;
            if (lightningChance > 0.0) {
                double roll = attacker.getRandom().nextDouble();
                if (roll < lightningChance) {
                    ChainLightningHandler.spawnChainLightning(attacker, target);
                }
            }
        }

        if (config.isFireTornadoEnabled()) {
            EntityAttributeInstance fireTornadoAttr = attacker.getAttributeInstance(KevsLibrary.FIRE_TORNADO_CHANCE);
            double fireTornadoChance = fireTornadoAttr != null ? fireTornadoAttr.getValue() : 0.0;
            if (fireTornadoChance > 0.0 && attacker.getRandom().nextDouble() < fireTornadoChance) {
                FireTornadoHandler.spawnFireTornado(attacker, target);
            }
        }

        if (config.isSoulLinkEnabled()) {
            EntityAttributeInstance soulLinkAttr = attacker.getAttributeInstance(KevsLibrary.SOUL_LINK_CHANCE);
            double soulLinkChance = soulLinkAttr != null ? soulLinkAttr.getValue() : 0.0;

            if (soulLinkChance > 0.0 && attacker.getRandom().nextDouble() < soulLinkChance) {
                SoulLinkHandler.triggerSoulLink(attacker, target);
            }

            SoulLinkTracker.getGroup(target).ifPresent(linkData -> {
                if (!attacker.getUuid().equals(linkData.attacker().getUuid())) return;
                SoulLinkHandler.handleLinkedDamage(linkData.attacker(), target, finalDamage, linkData.group(), linkData.soulPower());
            });

            SoulLinkTracker.getGroup(target).ifPresent(linkData -> {
                if (attacker.getRandom().nextDouble() < soulLinkChance) {
                    SoulLinkHandler.tryExtendLink(attacker, target);
                }
            });
        }

        if (config.isArcaneRuptureEnabled()) {
            EntityAttributeInstance arcaneChanceAttr = attacker.getAttributeInstance(KevsLibrary.ARCANE_RUPTURE_CHANCE);
            double arcaneChance = arcaneChanceAttr != null ? arcaneChanceAttr.getValue() : 0.0;
            if (arcaneChance > 0.0 && attacker.getRandom().nextDouble() < arcaneChance && !source.getName().equals("arcane_shard")) {
                ArcaneRuptureHandler.trigger(attacker, target);
            }
        }

        if (config.isCleaveEnabled() && !IN_CLEAVE_CONTEXT.get() && source.getSource() == attacker) {
            IN_CLEAVE_CONTEXT.set(true);
            CleaveHandler.triggerCleave(attacker, finalDamage);
            IN_CLEAVE_CONTEXT.set(false);
        }

        if (config.isPiercingEnabled()) {
            if (PiercingHandler.isPiercingActive(attacker)) {
                PiercingHandler.consumePiercing(attacker);
                float pierceDamage = finalDamage * 0.75f;
                PiercingHandler.applyLineDamage(attacker, pierceDamage, 6.0f, 1.0f);
            } else {
                PiercingHandler.tryActivatePiercing(attacker);
            }
        }
    }
}