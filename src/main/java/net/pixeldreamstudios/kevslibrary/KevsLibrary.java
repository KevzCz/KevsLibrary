package net.pixeldreamstudios.kevslibrary;

import net.fabricmc.api.ModInitializer;
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
import net.pixeldreamstudios.kevslibrary.entity.IcicleProjectileEntity;
import net.pixeldreamstudios.kevslibrary.entity.MultistrikeArrowEntity;
import net.pixeldreamstudios.kevslibrary.handler.MultistrikeHandler;
import net.pixeldreamstudios.kevslibrary.registry.RegistryHelper;
import net.pixeldreamstudios.kevslibrary.util.DelayedExecutor;

public class KevsLibrary implements ModInitializer {
	public static final String MOD_ID = "kevslibrary";

	public static final RegistryEntry<EntityAttribute> CRIT_CHANCE =
			RegistryHelper.registerAttribute("crit_chance", new ClampedEntityAttribute("attribute.name.generic.crit_chance", 0.0, 0.0, 1.0).setTracked(true));

	public static final RegistryEntry<EntityAttribute> CRIT_DAMAGE =
			RegistryHelper.registerAttribute("crit_damage", new ClampedEntityAttribute("attribute.name.generic.crit_damage", 1.5, 1.0, 10.0).setTracked(true));

	public static final RegistryEntry<EntityAttribute> MULTISTRIKE_CHANCE =
			RegistryHelper.registerAttribute("multistrike_chance",
					new ClampedEntityAttribute("attribute.name.generic.multistrike_chance", 0.0, 0.0, 1.0).setTracked(true));

	public static final RegistryEntry<EntityAttribute> MULTISTRIKE_COUNT =
			RegistryHelper.registerAttribute("multistrike_count",
					new ClampedEntityAttribute("attribute.name.generic.multistrike_count", 1.0, 1.0, 10.0).setTracked(true));

	public static final RegistryEntry<EntityAttribute> MULTISTRIKE_DAMAGE =
			RegistryHelper.registerAttribute("multistrike_damage",
					new ClampedEntityAttribute("attribute.name.generic.multistrike_damage", 1.0, 0.1, 10.0).setTracked(true));
	public static final RegistryEntry<EntityAttribute> DAMAGE =
			RegistryHelper.registerAttribute("damage",
					new ClampedEntityAttribute("attribute.name.generic.damage", 1.0, 1, 100.0).setTracked(true));
	public static final RegistryEntry<EntityAttribute> CHAIN_LIGHTNING_CHANCE =
			RegistryHelper.registerAttribute("chain_lightning_chance",
					new ClampedEntityAttribute("attribute.name.generic.chain_lightning_chance", 0, 0, 1).setTracked(true));
	public static final RegistryEntry<EntityAttribute> CHAIN_LIGHTNING_COUNT =
			RegistryHelper.registerAttribute("chain_lightning_count",
					new ClampedEntityAttribute("attribute.name.generic.chain_lightning_count", 3.0, 1.0, 10.0).setTracked(true));

	public static final RegistryEntry<EntityAttribute> CHAIN_LIGHTNING_OVERLOAD_CHANCE =
			RegistryHelper.registerAttribute("chain_lightning_overload_chance",
					new ClampedEntityAttribute("attribute.name.generic.chain_lightning_overload_chance", 0.0, 0.0, 1.0).setTracked(true));

	public static final RegistryEntry<EntityAttribute> FIRE_TORNADO_CHANCE =
			RegistryHelper.registerAttribute("fire_tornado_chance",
					new ClampedEntityAttribute("attribute.name.generic.fire_tornado_chance", 0, 0, 1).setTracked(true));
	public static final RegistryEntry<EntityAttribute> FIRE_TORNADO_OVERLOAD_CHANCE =
			RegistryHelper.registerAttribute("fire_tornado_overload_chance",
					new ClampedEntityAttribute("attribute.name.generic.fire_tornado_overload_chance", 0.0, 0.0, 1.0).setTracked(true));

	public static final RegistryEntry<EntityAttribute> FROST_NOVA_CHANCE =
			RegistryHelper.registerAttribute("frost_nova_chance",
					new ClampedEntityAttribute("attribute.name.generic.frost_nova_chance", 0, 0, 1).setTracked(true));

	public static final RegistryEntry<EntityAttribute> FROST_NOVA_COUNT =
			RegistryHelper.registerAttribute("frost_nova_count",
					new ClampedEntityAttribute("attribute.name.generic.frost_nova_count", 1, 1, 10).setTracked(true));
	public static final RegistryEntry<EntityAttribute> FROST_NOVA_OVERLOAD_CHANCE =
			RegistryHelper.registerAttribute("frost_nova_overload_chance",
					new ClampedEntityAttribute("attribute.name.generic.frost_nova_overload_chance", 0.0, 0.0, 1.0).setTracked(true));


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
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerWorld world : server.getWorlds()) {
				MultistrikeHandler.tick(world);
			}
		});

		DelayedExecutor.init();
	}

}
