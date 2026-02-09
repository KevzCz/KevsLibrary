package net.pixeldreamstudios.kevslibrary;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.attribute.ClampedEntityAttribute;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.pixeldreamstudios.kevslibrary.config.KevsLibraryConfig;
import net.pixeldreamstudios.kevslibrary.entity.ArcaneShardEntity;
import net.pixeldreamstudios.kevslibrary.entity.CleaveSlashEntity;
import net.pixeldreamstudios.kevslibrary.entity.IcicleProjectileEntity;
import net.pixeldreamstudios.kevslibrary.entity.MultistrikeArrowEntity;
import net.pixeldreamstudios.kevslibrary.handler.MultistrikeHandler;
import net.pixeldreamstudios.kevslibrary.registry.ConfiguredAttributeRegistry;
import net.pixeldreamstudios.kevslibrary.util.DelayedExecutor;
import net.pixeldreamstudios.kevslibrary.util.RPGUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KevsLibrary implements ModInitializer {
	public static final String MOD_ID = "kevslibrary";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final RegistryEntry<EntityAttribute> CRIT_CHANCE =
			ConfiguredAttributeRegistry.registerAttribute("crit_chance", new ClampedEntityAttribute("attribute.name.generic.crit_chance", 100.0, 0.0, 200.0).setTracked(true));

	public static final RegistryEntry<EntityAttribute> CRIT_DAMAGE =
			ConfiguredAttributeRegistry.registerAttribute("crit_damage", new ClampedEntityAttribute("attribute.name.generic.crit_damage", 150.0, 0.0, 10000.0).setTracked(true));

	public static final RegistryEntry<EntityAttribute> MULTISTRIKE_CHANCE =
			ConfiguredAttributeRegistry.registerAttribute("multistrike_chance",
					new ClampedEntityAttribute("attribute.name.generic.multistrike_chance", 100.0, 0.0, 200.0).setTracked(true));

	public static final RegistryEntry<EntityAttribute> MULTISTRIKE_COUNT =
			ConfiguredAttributeRegistry.registerAttribute("multistrike_count",
					new ClampedEntityAttribute("attribute.name.generic.multistrike_count", 2.0, 0.0, 10000.0).setTracked(true));

	public static final RegistryEntry<EntityAttribute> MULTISTRIKE_DAMAGE =
			ConfiguredAttributeRegistry.registerAttribute("multistrike_damage",
					new ClampedEntityAttribute("attribute.name.generic.multistrike_damage", 100.0, 0.0, 10000.0).setTracked(true));

	public static final RegistryEntry<EntityAttribute> DAMAGE =
			ConfiguredAttributeRegistry.registerAttribute("damage",
					new ClampedEntityAttribute("attribute.name.generic.damage_multiplier", 100.0, 0.0, 10000.0).setTracked(true));

	public static final RegistryEntry<EntityAttribute> CHAIN_LIGHTNING_CHANCE =
			ConfiguredAttributeRegistry.registerAttribute("chain_lightning_chance",
					new ClampedEntityAttribute("attribute.name.generic.chain_lightning_chance", 100.0, 0.0, 200.0).setTracked(true));

	public static final RegistryEntry<EntityAttribute> CHAIN_LIGHTNING_COUNT =
			ConfiguredAttributeRegistry.registerAttribute("chain_lightning_count",
					new ClampedEntityAttribute("attribute.name.generic.chain_lightning_count", 3.0, 0.0, 10000.0).setTracked(true));

	public static final RegistryEntry<EntityAttribute> CHAIN_LIGHTNING_OVERLOAD_CHANCE =
			ConfiguredAttributeRegistry.registerAttribute("chain_lightning_overload_chance",
					new ClampedEntityAttribute("attribute.name.generic.chain_lightning_overload_chance", 100.0, 0.0, 200.0).setTracked(true));

	public static final RegistryEntry<EntityAttribute> FIRE_TORNADO_CHANCE =
			ConfiguredAttributeRegistry.registerAttribute("fire_tornado_chance",
					new ClampedEntityAttribute("attribute.name.generic.fire_tornado_chance", 100.0, 0.0, 200.0).setTracked(true));

	public static final RegistryEntry<EntityAttribute> FIRE_TORNADO_OVERLOAD_CHANCE =
			ConfiguredAttributeRegistry.registerAttribute("fire_tornado_overload_chance",
					new ClampedEntityAttribute("attribute.name.generic.fire_tornado_overload_chance", 100.0, 0.0, 200.0).setTracked(true));

	public static final RegistryEntry<EntityAttribute> FROST_NOVA_CHANCE =
			ConfiguredAttributeRegistry.registerAttribute("frost_nova_chance",
					new ClampedEntityAttribute("attribute.name.generic.frost_nova_chance", 100.0, 0.0, 200.0).setTracked(true));

	public static final RegistryEntry<EntityAttribute> FROST_NOVA_COUNT =
			ConfiguredAttributeRegistry.registerAttribute("frost_nova_count",
					new ClampedEntityAttribute("attribute.name.generic.frost_nova_count", 3.0, 0.0, 10000.0).setTracked(true));

	public static final RegistryEntry<EntityAttribute> FROST_NOVA_OVERLOAD_CHANCE =
			ConfiguredAttributeRegistry.registerAttribute("frost_nova_overload_chance",
					new ClampedEntityAttribute("attribute.name.generic.frost_nova_overload_chance", 100.0, 0.0, 200.0).setTracked(true));

	public static final RegistryEntry<EntityAttribute> PET_INHERITANCE_RATIO =
			ConfiguredAttributeRegistry.registerAttribute("pet_inheritance_ratio",
					new ClampedEntityAttribute("attribute.name.generic.pet_inheritance_ratio", 100.0, 0.0, 10000.0).setTracked(true));

	public static final RegistryEntry<EntityAttribute> PET_DAMAGE_BONUS =
			ConfiguredAttributeRegistry.registerAttribute("pet_damage_bonus",
					new ClampedEntityAttribute("attribute.name.generic.pet_damage_bonus", 0.0, 0.0, 10000.0).setTracked(true));

	public static final RegistryEntry<EntityAttribute> SOUL_LINK_CHANCE =
			ConfiguredAttributeRegistry.registerAttribute("soul_link_chance",
					new ClampedEntityAttribute("attribute.name.generic.soul_link_chance", 100.0, 0.0, 200.0).setTracked(true));

	public static final RegistryEntry<EntityAttribute> SOUL_LINK_DAMAGE =
			ConfiguredAttributeRegistry.registerAttribute("soul_link_damage",
					new ClampedEntityAttribute("attribute.name.generic.soul_link_damage", 100.0, 0.0, 10000.0).setTracked(true));

	public static final RegistryEntry<EntityAttribute> ARCANE_RUPTURE_CHANCE =
			ConfiguredAttributeRegistry.registerAttribute("arcane_rupture_chance",
					new ClampedEntityAttribute("attribute.name.generic.arcane_rupture_chance", 105.0, 0.0, 200.0).setTracked(true));

	public static final RegistryEntry<EntityAttribute> ARCANE_RUPTURE_DAMAGE =
			ConfiguredAttributeRegistry.registerAttribute("arcane_rupture_damage",
					new ClampedEntityAttribute("attribute.name.generic.arcane_rupture_damage", 100.0, 0.0, 10000.0).setTracked(true));

	public static final RegistryEntry<EntityAttribute> ARCANE_RUPTURE_OVERLOAD_CHANCE =
			ConfiguredAttributeRegistry.registerAttribute("arcane_rupture_overload_chance",
					new ClampedEntityAttribute("attribute.name.generic.arcane_rupture_overload_chance", 100.0, 0.0, 200.0).setTracked(true));

	public static final RegistryEntry<EntityAttribute> TRIDENT_DAMAGE_MULTIPLIER =
			ConfiguredAttributeRegistry.registerAttribute("trident_damage_multiplier",
					new ClampedEntityAttribute("attribute.name.generic.trident_damage_multiplier", 100.0, 0.0, 10000.0).setTracked(true));

	public static final RegistryEntry<EntityAttribute> ARMOR_PENETRATION_FLAT =
			ConfiguredAttributeRegistry.registerAttribute("armor_penetration_flat",
					new ClampedEntityAttribute("attribute.name.generic.armor_penetration_flat", 0, 0.0, 10000.0).setTracked(true));

	public static final RegistryEntry<EntityAttribute> ARMOR_PENETRATION =
			ConfiguredAttributeRegistry.registerAttribute("armor_penetration",
					new ClampedEntityAttribute("attribute.name.generic.armor_penetration", 100.0, 0.0, 200.0).setTracked(true));

	public static final RegistryEntry<EntityAttribute> THORNS_CHANCE =
			ConfiguredAttributeRegistry.registerAttribute("thorns_chance",
					new ClampedEntityAttribute("attribute.name.generic.thorns_chance", 100.0, 0.0, 200.0).setTracked(true));

	public static final RegistryEntry<EntityAttribute> THORNS_AMP =
			ConfiguredAttributeRegistry.registerAttribute("thorns_amp",
					new ClampedEntityAttribute("attribute.name.generic.thorns_amp", 30.0, 0.0, 10000.0).setTracked(true));

	public static final RegistryEntry<EntityAttribute> THORNS_TRUE_DAMAGE_CHANCE =
			ConfiguredAttributeRegistry.registerAttribute("thorns_true_damage_chance",
					new ClampedEntityAttribute("attribute.name.generic.thorns_true_damage_chance", 100.0, 0.0, 200.0).setTracked(true));

	public static final RegistryEntry<EntityAttribute> CLEAVE_CHANCE =
			ConfiguredAttributeRegistry.registerAttribute("cleave_chance",
					new ClampedEntityAttribute("attribute.name.generic.cleave_chance", 100.0, 0.0, 200.0).setTracked(true));

	public static final RegistryEntry<EntityAttribute> CLEAVE_DAMAGE_MULTIPLIER =
			ConfiguredAttributeRegistry.registerAttribute("cleave_damage_multiplier",
					new ClampedEntityAttribute("attribute.name.generic.cleave_damage_multiplier", 100.0, 0.0, 10000.0).setTracked(true));

	public static final RegistryEntry<EntityAttribute> CLEAVE_RANGE =
			ConfiguredAttributeRegistry.registerAttribute("cleave_range",
					new ClampedEntityAttribute("attribute.name.generic.cleave_range", 4.0, 0.0, 10000.0).setTracked(true));

	public static final RegistryEntry<EntityAttribute> PIERCING_CHANCE =
			ConfiguredAttributeRegistry.registerAttribute("pierce_chance",
					new ClampedEntityAttribute("attribute.name.generic.pierce_chance", 100.0, 0.0, 200.0).setTracked(true));

	public static final RegistryEntry<EntityAttribute> BARRAGE_CHANCE =
			ConfiguredAttributeRegistry.registerAttribute("barrage_chance",
					new ClampedEntityAttribute("attribute.name.generic.barrage_chance", 100.0, 0.0, 200.0).setTracked(true));

	public static final RegistryEntry<EntityAttribute> PROJECTILE_STORM_CHANCE =
			ConfiguredAttributeRegistry.registerAttribute("projectile_storm_chance",
					new ClampedEntityAttribute("attribute.name.generic.projectile_storm_chance", 100.0, 0.0, 200.0).setTracked(true));

	public static final RegistryEntry<EntityAttribute> PROJECTILE_STORM_RANGE =
			ConfiguredAttributeRegistry.registerAttribute("projectile_storm_range",
					new ClampedEntityAttribute("attribute.name.generic.projectile_storm_range", 2.5, 0.0, 10000.0).setTracked(true));

	public static final RegistryEntry<EntityAttribute> PROJECTILE_STORM_DURATION =
			ConfiguredAttributeRegistry.registerAttribute("projectile_storm_duration",
					new ClampedEntityAttribute("attribute.name.generic.projectile_storm_duration", 60.0, 0.0, 10000.0).setTracked(true));

	public static final RegistryEntry<EntityAttribute> HUNGER_CONSUMPTION =
			ConfiguredAttributeRegistry.registerAttribute("hunger_consumption",
					new ClampedEntityAttribute("attribute.name.generic.hunger_consumption", 100.0, 0.0, 10000.0).setTracked(true));

	public static final EntityType<CleaveSlashEntity> CLEAVE_SLASH = Registry.register(
			Registries.ENTITY_TYPE,
			Identifier.of(MOD_ID, "cleave_slash"),
			FabricEntityTypeBuilder.<CleaveSlashEntity>create(SpawnGroup.MISC, CleaveSlashEntity::new)
					.dimensions(EntityDimensions.fixed(0.1f, 0.1f))
					.trackRangeBlocks(8)
					.trackedUpdateRate(10)
					.build()
	);

	public static final EntityType<ArcaneShardEntity> ARCANE_SHARD =
			Registry.register(
					Registries.ENTITY_TYPE,
					Identifier.of(MOD_ID, "arcane_shard"),
					FabricEntityTypeBuilder.<ArcaneShardEntity>create(SpawnGroup.MISC, ArcaneShardEntity::new)
							.dimensions(EntityDimensions.fixed(0.25f, 0.25f))
							.trackRangeBlocks(6)
							.trackedUpdateRate(10)
							.build()
			);

	public static final EntityType<IcicleProjectileEntity> ICICLE_PROJECTILE =
			Registry.register(
					Registries.ENTITY_TYPE,
					Identifier.of(MOD_ID, "icicle_projectile"),
					FabricEntityTypeBuilder.<IcicleProjectileEntity>create(SpawnGroup.MISC, IcicleProjectileEntity::new)
							.dimensions(EntityDimensions.fixed(0.25f, 0.25f))
							.trackRangeBlocks(4)
							.trackedUpdateRate(10)
							.build()
			);

	public static final EntityType<MultistrikeArrowEntity> MULTISTRIKE_ARROW =
			Registry.register(
					Registries.ENTITY_TYPE,
					Identifier.of(MOD_ID, "multistrike_arrow"),
					FabricEntityTypeBuilder.<MultistrikeArrowEntity>create(SpawnGroup.MISC, MultistrikeArrowEntity::new)
							.dimensions(EntityDimensions.fixed(0.5f, 0.5f))
							.trackRangeBlocks(4)
							.trackedUpdateRate(10)
							.build()
			);

	@Override
	public void onInitialize() {

		KevsLibraryConfig.getInstance();
		LOGGER.info("KevsLibrary config loaded");

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerWorld world : server.getWorlds()) {
				try {
					MultistrikeHandler.tick(world);
				} catch (Exception e) {
					LOGGER.error("[KevsLibrary] MultistrikeHandler failed: " + e.getMessage(), e);
				}
			}
		});

		DelayedExecutor.init();
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			RPGUtil.register(dispatcher);
		});

		LOGGER.info("KevsLibrary initialized successfully");
	}
}