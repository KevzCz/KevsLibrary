package net.pixeldreamstudios.kevslibrary.mixin;

import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.pixeldreamstudios.kevslibrary.KevsLibrary;
import net.pixeldreamstudios.kevslibrary.config.KevsLibraryConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class PlayerMixin {

    @Inject(method = "createPlayerAttributes", at = @At("RETURN"), cancellable = true)
    private static void injectCustomAttributes(CallbackInfoReturnable<DefaultAttributeContainer.Builder> cir) {
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

        if (config.isAttributeEnabled("pet_inheritance_ratio") && KevsLibrary.PET_INHERITANCE_RATIO != null)
            builder.add(KevsLibrary.PET_INHERITANCE_RATIO, 100.0);

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

        if (config.isAttributeEnabled("pet_damage_bonus") && KevsLibrary.PET_DAMAGE_BONUS != null)
            builder.add(KevsLibrary.PET_DAMAGE_BONUS, 0.0);

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

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void migrateOldAttributes(NbtCompound nbt, CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity) (Object) this;

        if (!nbt.contains("KevsLibraryMigratedV2")) {
            migrateAttribute(player, KevsLibrary.CRIT_CHANCE, 100.0);
            migrateAttribute(player, KevsLibrary.CRIT_DAMAGE, 150.0);
            migrateAttribute(player, KevsLibrary.MULTISTRIKE_CHANCE, 100.0);
            migrateAttribute(player, KevsLibrary.MULTISTRIKE_COUNT, 2.0);
            migrateAttribute(player, KevsLibrary.MULTISTRIKE_DAMAGE, 100.0);
            migrateAttribute(player, KevsLibrary.DAMAGE, 100.0);
            migrateAttribute(player, KevsLibrary.CHAIN_LIGHTNING_CHANCE, 100.0);
            migrateAttribute(player, KevsLibrary.CHAIN_LIGHTNING_COUNT, 3.0);
            migrateAttribute(player, KevsLibrary.CHAIN_LIGHTNING_OVERLOAD_CHANCE, 100.0);
            migrateAttribute(player, KevsLibrary.FIRE_TORNADO_CHANCE, 100.0);
            migrateAttribute(player, KevsLibrary.FIRE_TORNADO_OVERLOAD_CHANCE, 100.0);
            migrateAttribute(player, KevsLibrary.FROST_NOVA_CHANCE, 100.0);
            migrateAttribute(player, KevsLibrary.FROST_NOVA_COUNT, 3.0);
            migrateAttribute(player, KevsLibrary.FROST_NOVA_OVERLOAD_CHANCE, 100.0);
            migrateAttribute(player, KevsLibrary.PET_INHERITANCE_RATIO, 100.0);
            migrateAttribute(player, KevsLibrary.PET_DAMAGE_BONUS, 0.0);
            migrateAttribute(player, KevsLibrary.SOUL_LINK_CHANCE, 100.0);
            migrateAttribute(player, KevsLibrary.SOUL_LINK_DAMAGE, 100.0);
            migrateAttribute(player, KevsLibrary.ARCANE_RUPTURE_CHANCE, 100.0);
            migrateAttribute(player, KevsLibrary.ARCANE_RUPTURE_DAMAGE, 105.0);
            migrateAttribute(player, KevsLibrary.ARCANE_RUPTURE_OVERLOAD_CHANCE, 100.0);
            migrateAttribute(player, KevsLibrary.TRIDENT_DAMAGE_MULTIPLIER, 100.0);
            migrateAttribute(player, KevsLibrary.ARMOR_PENETRATION_FLAT, 0.0);
            migrateAttribute(player, KevsLibrary.ARMOR_PENETRATION, 100.0);
            migrateAttribute(player, KevsLibrary.THORNS_CHANCE, 100.0);
            migrateAttribute(player, KevsLibrary.THORNS_AMP, 30.0);
            migrateAttribute(player, KevsLibrary.THORNS_TRUE_DAMAGE_CHANCE, 100.0);
            migrateAttribute(player, KevsLibrary.CLEAVE_CHANCE, 100.0);
            migrateAttribute(player, KevsLibrary.CLEAVE_DAMAGE_MULTIPLIER, 100.0);
            migrateAttribute(player, KevsLibrary.CLEAVE_RANGE, 4.0);
            migrateAttribute(player, KevsLibrary.PIERCING_CHANCE, 100.0);
            migrateAttribute(player, KevsLibrary.BARRAGE_CHANCE, 100.0);
            migrateAttribute(player, KevsLibrary.PROJECTILE_STORM_CHANCE, 100.0);
            migrateAttribute(player, KevsLibrary.PROJECTILE_STORM_RANGE, 2.5);
            migrateAttribute(player, KevsLibrary.PROJECTILE_STORM_DURATION, 60.0);
            migrateAttribute(player, KevsLibrary.HUNGER_CONSUMPTION, 100.0);
        }
    }

    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    private void markAsMigrated(NbtCompound nbt, CallbackInfo ci) {
        nbt.putBoolean("KevsLibraryMigratedV2", true);
    }

    private void migrateAttribute(PlayerEntity player,
                                  net.minecraft.registry.entry.RegistryEntry<net.minecraft.entity.attribute.EntityAttribute> attribute,
                                  double newDefault) {
        if (attribute == null) return;
        EntityAttributeInstance instance = player.getAttributeInstance(attribute);
        if (instance != null) {
            instance.setBaseValue(newDefault);
        }
    }
}