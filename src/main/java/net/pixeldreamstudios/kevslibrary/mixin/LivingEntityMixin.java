package net.pixeldreamstudios.kevslibrary.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.pixeldreamstudios.kevslibrary.KevsLibrary;
import net.pixeldreamstudios.kevslibrary.api.CritEvents;
import net.pixeldreamstudios.kevslibrary.attribute.AttributeContext;
import net.pixeldreamstudios.kevslibrary.config.KevsLibraryConfig;
import net.pixeldreamstudios.kevslibrary.handler.ArcaneRuptureHandler;
import net.pixeldreamstudios.kevslibrary.handler.ChainLightningHandler;
import net.pixeldreamstudios.kevslibrary.handler.CleaveHandler;
import net.pixeldreamstudios.kevslibrary.handler.FireTornadoHandler;
import net.pixeldreamstudios.kevslibrary.handler.FrostNovaHandler;
import net.pixeldreamstudios.kevslibrary.handler.MultistrikeHandler;
import net.pixeldreamstudios.kevslibrary.handler.OnHitEffectHandler;
import net.pixeldreamstudios.kevslibrary.handler.PiercingHandler;
import net.pixeldreamstudios.kevslibrary.handler.ProjectileStormHandler;
import net.pixeldreamstudios.kevslibrary.handler.SoulLinkHandler;
import net.pixeldreamstudios.kevslibrary.handler.SoulLinkTracker;
import net.pixeldreamstudios.kevslibrary.handler.ThornsHandler;
import net.pixeldreamstudios.kevslibrary.handler.WeaponSwitchHandler;
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

    @ModifyReturnValue(method = "isInvulnerableTo", at = @At("RETURN"))
    private boolean bypassMultistrike(boolean original, DamageSource source) {
        if (source.getName().equals("multistrike") || source.getName().equals("multistrike_ranged")) {
            return false;
        }
        return original;
    }

    @Inject(method = "createLivingAttributes", at = @At("RETURN"))
    private static void injectGlobalAttributes(CallbackInfoReturnable<DefaultAttributeContainer.Builder> cir) {
        KevsLibraryConfig config = KevsLibraryConfig.getInstance();
        DefaultAttributeContainer.Builder builder = cir.getReturnValue();

        if (config.isAttributeEnabled("crit_chance") && KevsLibrary.CRIT_CHANCE != null)
            builder.add(KevsLibrary.CRIT_CHANCE, 100.0);

        if (config.isAttributeEnabled("crit_damage") && KevsLibrary.CRIT_DAMAGE != null)
            builder.add(KevsLibrary.CRIT_DAMAGE, 150.0);

        if (config.isAttributeEnabled("multistrike_chance") && KevsLibrary.MULTISTRIKE_CHANCE != null)
            builder.add(KevsLibrary.MULTISTRIKE_CHANCE, 100.0);

        if (config.isAttributeEnabled("multistrike_count") && KevsLibrary.MULTISTRIKE_COUNT != null)
            builder.add(KevsLibrary.MULTISTRIKE_COUNT, 2.0);

        if (config.isAttributeEnabled("multistrike_damage") && KevsLibrary.MULTISTRIKE_DAMAGE != null)
            builder.add(KevsLibrary.MULTISTRIKE_DAMAGE, 100.0);

        if (config.isAttributeEnabled("damage") && KevsLibrary.DAMAGE != null)
            builder.add(KevsLibrary.DAMAGE, 100.0);

        if (config.isAttributeEnabled("chain_lightning_chance") && KevsLibrary.CHAIN_LIGHTNING_CHANCE != null)
            builder.add(KevsLibrary.CHAIN_LIGHTNING_CHANCE, 100.0);

        if (config.isAttributeEnabled("chain_lightning_count") && KevsLibrary.CHAIN_LIGHTNING_COUNT != null)
            builder.add(KevsLibrary.CHAIN_LIGHTNING_COUNT, 3.0);

        if (config.isAttributeEnabled("chain_lightning_overload_chance") && KevsLibrary.CHAIN_LIGHTNING_OVERLOAD_CHANCE != null)
            builder.add(KevsLibrary.CHAIN_LIGHTNING_OVERLOAD_CHANCE, 100.0);

        if (config.isAttributeEnabled("fire_tornado_chance") && KevsLibrary.FIRE_TORNADO_CHANCE != null)
            builder.add(KevsLibrary.FIRE_TORNADO_CHANCE, 100.0);

        if (config.isAttributeEnabled("fire_tornado_overload_chance") && KevsLibrary.FIRE_TORNADO_OVERLOAD_CHANCE != null)
            builder.add(KevsLibrary.FIRE_TORNADO_OVERLOAD_CHANCE, 100.0);

        if (config.isAttributeEnabled("frost_nova_chance") && KevsLibrary.FROST_NOVA_CHANCE != null)
            builder.add(KevsLibrary.FROST_NOVA_CHANCE, 100.0);

        if (config.isAttributeEnabled("frost_nova_count") && KevsLibrary.FROST_NOVA_COUNT != null)
            builder.add(KevsLibrary.FROST_NOVA_COUNT, 3.0);

        if (config.isAttributeEnabled("frost_nova_overload_chance") && KevsLibrary.FROST_NOVA_OVERLOAD_CHANCE != null)
            builder.add(KevsLibrary.FROST_NOVA_OVERLOAD_CHANCE, 100.0);

        if (config.isAttributeEnabled("soul_link_chance") && KevsLibrary.SOUL_LINK_CHANCE != null)
            builder.add(KevsLibrary.SOUL_LINK_CHANCE, 100.0);

        if (config.isAttributeEnabled("soul_link_damage") && KevsLibrary.SOUL_LINK_DAMAGE != null)
            builder.add(KevsLibrary.SOUL_LINK_DAMAGE, 100.0);

        if (config.isAttributeEnabled("arcane_rupture_chance") && KevsLibrary.ARCANE_RUPTURE_CHANCE != null)
            builder.add(KevsLibrary.ARCANE_RUPTURE_CHANCE, 100.0);

        if (config.isAttributeEnabled("arcane_rupture_damage") && KevsLibrary.ARCANE_RUPTURE_DAMAGE != null)
            builder.add(KevsLibrary.ARCANE_RUPTURE_DAMAGE, 105.0);

        if (config.isAttributeEnabled("arcane_rupture_overload_chance") && KevsLibrary.ARCANE_RUPTURE_OVERLOAD_CHANCE != null)
            builder.add(KevsLibrary.ARCANE_RUPTURE_OVERLOAD_CHANCE, 100.0);

        if (config.isAttributeEnabled("trident_damage_multiplier") && KevsLibrary.TRIDENT_DAMAGE_MULTIPLIER != null)
            builder.add(KevsLibrary.TRIDENT_DAMAGE_MULTIPLIER, 100.0);

        if (config.isAttributeEnabled("armor_penetration") && KevsLibrary.ARMOR_PENETRATION != null)
            builder.add(KevsLibrary.ARMOR_PENETRATION, 100.0);

        if (config.isAttributeEnabled("armor_penetration_flat") && KevsLibrary.ARMOR_PENETRATION_FLAT != null)
            builder.add(KevsLibrary.ARMOR_PENETRATION_FLAT, 0);

        if (config.isAttributeEnabled("thorns_chance") && KevsLibrary.THORNS_CHANCE != null)
            builder.add(KevsLibrary.THORNS_CHANCE, 100.0);

        if (config.isAttributeEnabled("thorns_amp") && KevsLibrary.THORNS_AMP != null)
            builder.add(KevsLibrary.THORNS_AMP, 30.0);

        if (config.isAttributeEnabled("thorns_true_damage_chance") && KevsLibrary.THORNS_TRUE_DAMAGE_CHANCE != null)
            builder.add(KevsLibrary.THORNS_TRUE_DAMAGE_CHANCE, 100.0);

        if (config.isAttributeEnabled("cleave_damage_multiplier") && KevsLibrary.CLEAVE_DAMAGE_MULTIPLIER != null)
            builder.add(KevsLibrary.CLEAVE_DAMAGE_MULTIPLIER, 100.0);

        if (config.isAttributeEnabled("cleave_range") && KevsLibrary.CLEAVE_RANGE != null)
            builder.add(KevsLibrary.CLEAVE_RANGE, 4.0);

        if (config.isAttributeEnabled("cleave_chance") && KevsLibrary.CLEAVE_CHANCE != null)
            builder.add(KevsLibrary.CLEAVE_CHANCE, 100.0);

        if (config.isAttributeEnabled("pierce_chance") && KevsLibrary.PIERCING_CHANCE != null)
            builder.add(KevsLibrary.PIERCING_CHANCE, 100.0);

        if (config.isAttributeEnabled("barrage_chance") && KevsLibrary.BARRAGE_CHANCE != null)
            builder.add(KevsLibrary.BARRAGE_CHANCE, 100.0);

        if (config.isAttributeEnabled("projectile_storm_chance") && KevsLibrary.PROJECTILE_STORM_CHANCE != null)
            builder.add(KevsLibrary.PROJECTILE_STORM_CHANCE, 100.0);

        if (config.isAttributeEnabled("projectile_storm_range") && KevsLibrary.PROJECTILE_STORM_RANGE != null)
            builder.add(KevsLibrary.PROJECTILE_STORM_RANGE, 2.5);

        if (config.isAttributeEnabled("projectile_storm_duration") && KevsLibrary.PROJECTILE_STORM_DURATION != null)
            builder.add(KevsLibrary.PROJECTILE_STORM_DURATION, 60.0);

        if (config.isAttributeEnabled("hunger_consumption") && KevsLibrary.HUNGER_CONSUMPTION != null)
            builder.add(KevsLibrary.HUNGER_CONSUMPTION, 100.0);
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

        AttributeContext context = new AttributeContext(attacker);

        if (config.isTridentEnabled() && source.getName().equals("trident")) {
            double tridentValue = context.getAttributeValue(KevsLibrary.TRIDENT_DAMAGE_MULTIPLIER);
            amount *= (float) ((tridentValue - 100.0) / 100.0 + 1.0);
        }

        TagKey<DamageType> magicTag = TagKey.of(RegistryKeys.DAMAGE_TYPE, Identifier.of("c", "is_magic"));
        boolean allowCrit = !source.isIn(magicTag) && config.isCritEnabled();

        boolean isVanillaCrit = false;
        boolean isGroundedCrit = false;

        if (allowCrit) {
            if (attacker instanceof PlayerEntity player) {
                isVanillaCrit = player.fallDistance > 0.0F && !player.isOnGround();
            }

            double critChance = context.getAttributeAsPercentage(KevsLibrary.CRIT_CHANCE);
            isGroundedCrit = attacker.isOnGround() && attacker.getRandom().nextDouble() < critChance;
        }

        boolean isCrit = isVanillaCrit || isGroundedCrit;

        float finalDamage = amount;

        if (isVanillaCrit) {
            finalDamage /= 1.5f;
        }

        double damageValue = context.getAttributeValue(KevsLibrary.DAMAGE);
        finalDamage *= (float) ((damageValue - 100.0) / 100.0 + 1.0);

        if (config.isAttributeEnabled("first_hit_damage_multiplier") &&
                KevsLibrary.FIRST_HIT_DAMAGE_MULTIPLIER != null &&
                attacker instanceof PlayerEntity player) {

            WeaponSwitchHandler handler =
                    WeaponSwitchHandler.getInstance();

            if (handler.canUseFirstHitBonus(player) || handler.isWithinSameAttackSwing(player)) {
                double firstHitValue = context.getAttributeValue(KevsLibrary.FIRST_HIT_DAMAGE_MULTIPLIER);
                float firstHitMultiplier = (float) ((firstHitValue - 100.0) / 100.0 + 1.0);

                if (firstHitMultiplier > 1.0f) {
                    finalDamage *= firstHitMultiplier;
                    handler.consumeFirstHitBonus(player);

                    player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.ENTITY_PLAYER_ATTACK_STRONG,
                            SoundCategory.PLAYERS, 1.0f, 1.3f);
                }
            }
        }

        if (isCrit) {
            double critDamageValue = context.getAttributeValue(KevsLibrary.CRIT_DAMAGE);
            float critMultiplier = (float) ((critDamageValue - 100.0) / 100.0 + 1.0);

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

        if (!(target.getWorld() instanceof ServerWorld world)) return;

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
            AttributeContext context = new AttributeContext(attacker);
            double multistrikeChance = context.getAttributeAsPercentage(KevsLibrary.MULTISTRIKE_CHANCE);

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

                        if (source.getSource() instanceof TridentEntity) {
                            double triValue = context.getAttributeValue(KevsLibrary.TRIDENT_DAMAGE_MULTIPLIER);
                            float triMul = (float) ((triValue - 100.0) / 100.0 + 1.0);
                            if (triMul != 0.0f) msBase = finalDamage / triMul;
                        }

                        double dmgValue = context.getAttributeValue(KevsLibrary.DAMAGE);
                        float dmgMul = (float) ((dmgValue - 100.0) / 100.0 + 1.0);
                        if (dmgMul != 0.0f) msBase = msBase / dmgMul;

                        MultistrikeHandler.spawnHoveringProjectiles(attacker, target, msBase, source.getSource(), weaponUsed);
                    } else {
                        MultistrikeHandler.triggerMultistrike(attacker, target, finalDamage, weaponUsed);
                    }
                }
            }
        }

        if (config.isFrostNovaEnabled()) {
            FrostNovaHandler.getInstance().tryTrigger(attacker, target, world, finalDamage);
        }

        if (config.isChainLightningEnabled()) {
            ChainLightningHandler.getInstance().tryTrigger(attacker, target, world, finalDamage);
        }

        if (config.isFireTornadoEnabled()) {
            FireTornadoHandler.getInstance().tryTrigger(attacker, target, world, finalDamage);
        }

        if (config.isSoulLinkEnabled()) {
            SoulLinkHandler.getInstance().tryTrigger(attacker, target, world, finalDamage);

            SoulLinkTracker.getGroup(target).ifPresent(linkData -> {
                if (!attacker.getUuid().equals(linkData.attacker().getUuid())) return;
                SoulLinkHandler.handleLinkedDamage(linkData.attacker(), target, finalDamage, linkData.group(), linkData.soulPower());
            });

            AttributeContext context = new AttributeContext(attacker);
            double soulLinkChance = context.getAttributeAsPercentage(KevsLibrary.SOUL_LINK_CHANCE);
            SoulLinkTracker.getGroup(target).ifPresent(linkData -> {
                if (attacker.getRandom().nextDouble() < soulLinkChance) {
                    SoulLinkHandler.tryExtendLink(attacker, target);
                }
            });
        }

        if (config.isArcaneRuptureEnabled() && !source.getName().equals("arcane_shard")) {
            ArcaneRuptureHandler.getInstance().tryTrigger(attacker, target, world, finalDamage);
        }

        if (config.isCleaveEnabled() && !IN_CLEAVE_CONTEXT.get() && source.getSource() == attacker) {
            IN_CLEAVE_CONTEXT.set(true);
            CleaveHandler.getInstance().tryTrigger(attacker, target, world, finalDamage);
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