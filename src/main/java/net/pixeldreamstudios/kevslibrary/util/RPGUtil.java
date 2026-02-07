package net.pixeldreamstudios.kevslibrary.util;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.pixeldreamstudios.kevslibrary.taming.UniversalTameable;

public class RPGUtil
{

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher)
    {
        dispatcher.register(
                CommandManager.literal("rpgutility")
                        .requires(source -> source.hasPermissionLevel(2))
                        .then(
                                CommandManager.literal("reapplyinheritance")
                                        .then(
                                                CommandManager.argument("targets", EntityArgumentType.entities())
                                                        .executes(ctx -> {
                                                            var targets = EntityArgumentType.getEntities(ctx, "targets").stream()
                                                                    .filter(e -> e instanceof LivingEntity)
                                                                    .map(e -> (LivingEntity) e)
                                                                    .toList();

                                                            int updated = 0;

                                                            for (var pet : targets) {
                                                                if (!(pet instanceof MobEntity mob)) {
                                                                    ctx.getSource().sendFeedback(() -> Text.literal("Skipped non-mob entity: " + pet.getName().getString()), false);
                                                                    continue;
                                                                }

                                                                if (mob instanceof UniversalTameable tameable && tameable.kevslib$isTamed()) {

                                                                    var world = mob.getWorld();
                                                                    if (!(world instanceof ServerWorld serverWorld)) {
                                                                        ctx.getSource().sendFeedback(() -> Text.literal("World not server-side for: " + mob.getName().getString()), false);
                                                                        continue;
                                                                    }

                                                                    var ownerUuid = tameable.kevslib$getOwnerUuid();


                                                                    var owner = serverWorld.getPlayerByUuid(ownerUuid);

                                                                    NbtCompound prev = tameable.kevslib$getInheritanceData();
                                                                    if (prev != null && prev.contains("petBaseAttributes")) {
                                                                        NbtCompound baseAttrs = prev.getCompound("petBaseAttributes");

                                                                        for (String key : baseAttrs.getKeys()) {
                                                                            var attrId = net.minecraft.util.Identifier.tryParse(key);
                                                                            if (attrId != null) {
                                                                                var reg = net.minecraft.registry.Registries.ATTRIBUTE.getEntry(attrId);
                                                                                if (reg.isPresent()) {
                                                                                    var instance = mob.getAttributeInstance(reg.get());
                                                                                    if (instance != null) {
                                                                                        double oldVal = instance.getBaseValue();
                                                                                        double baseVal = baseAttrs.getDouble(key);
                                                                                        instance.setBaseValue(baseVal);
                                                                                    }
                                                                                }
                                                                            }
                                                                        }
                                                                    }

                                                                    double ratio = owner.getAttributeValue(net.pixeldreamstudios.kevslibrary.KevsLibrary.PET_INHERITANCE_RATIO);
                                                                    var inherited = AttributeInheritanceUtil.apply(owner, mob, new NbtCompound(), ratio);
                                                                    tameable.kevslib$setInheritanceData(inherited);

                                                                    if (mob.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH) != null) {
                                                                        double newMaxHealth = mob.getAttributeValue(EntityAttributes.GENERIC_MAX_HEALTH);
                                                                        double currentHealth = mob.getHealth();
                                                                        mob.setHealth((float)Math.min(currentHealth, newMaxHealth));

                                                                    }

                                                                    updated++;
                                                                }
                                                            }
                                                            return updated;
                                                        })
                                        )
                        )


                        .then(
                                CommandManager.literal("anger")
                                        .then(
                                                CommandManager.argument("source", EntityArgumentType.entities())
                                                        .then(
                                                                CommandManager.literal("at")
                                                                        .then(
                                                                                CommandManager.argument("target", EntityArgumentType.entities())
                                                                                        .executes(ctx ->
                                                                                        {
                                                                                            var sources = EntityArgumentType.getEntities(ctx, "source").stream()
                                                                                                    .filter(e -> e instanceof MobEntity)
                                                                                                    .map(e -> (MobEntity) e)
                                                                                                    .toList();

                                                                                            var targets = EntityArgumentType.getEntities(ctx, "target").stream()
                                                                                                    .filter(e -> e instanceof LivingEntity && e.isAlive())
                                                                                                    .map(e -> (LivingEntity) e)
                                                                                                    .toList();

                                                                                            for (var source : sources)
                                                                                            {
                                                                                                if (!targets.isEmpty())
                                                                                                {
                                                                                                    var target = targets.get(0);
                                                                                                    if (source.canTarget(target))
                                                                                                    {
                                                                                                        source.setTarget(target);
                                                                                                        source.setAttacker(target);

                                                                                                        DelayedExecutor.runLater(() ->
                                                                                                        {
                                                                                                            if (source.getTarget() != null && (!source.getTarget().isAlive() || source.getTarget().isRemoved()))
                                                                                                            {
                                                                                                                source.setTarget(null);
                                                                                                                source.setAttacker(null);
                                                                                                            }
                                                                                                        }, 200);
                                                                                                    }
                                                                                                }
                                                                                            }

                                                                                            ctx.getSource().sendFeedback(() ->
                                                                                                    Text.literal("Set " + sources.size() + " mob(s) to attack " + targets.size() + " target(s)."), false);
                                                                                            return 1;
                                                                                        })
                                                                        )
                                                        )
                                        )
                        )

                        .then(
                                CommandManager.literal("blockinteraction")
                                        .then(
                                                CommandManager.argument("player", EntityArgumentType.player())
                                                        .then(
                                                                CommandManager.literal("add")
                                                                        .then(
                                                                                CommandManager.argument("type", StringArgumentType.word())
                                                                                        .suggests((ctx, builder) -> {
                                                                                            builder.suggest("left_click");
                                                                                            builder.suggest("right_click");
                                                                                            builder.suggest("placement");
                                                                                            builder.suggest("entity_interact");
                                                                                            builder.suggest("consume");
                                                                                            builder.suggest("all");
                                                                                            return builder.buildFuture();
                                                                                        })
                                                                                        .executes(ctx -> {
                                                                                            ServerPlayerEntity player = EntityArgumentType.getPlayer(ctx, "player");
                                                                                            String type = StringArgumentType.getString(ctx, "type");
                                                                                            ItemStack heldItem = player.getMainHandStack();

                                                                                            if (heldItem.isEmpty()) {
                                                                                                ctx.getSource().sendError(Text.literal("Player must be holding an item"));
                                                                                                return 0;
                                                                                            }

                                                                                            NbtCompound customData = heldItem.getOrDefault(
                                                                                                    net.minecraft.component.DataComponentTypes.CUSTOM_DATA,
                                                                                                    net.minecraft.component.type.NbtComponent.DEFAULT
                                                                                            ).copyNbt();

                                                                                            NbtCompound blocked = customData.getCompound("KevsLibraryBlockedInteractions");
                                                                                            if (!customData.contains("KevsLibraryBlockedInteractions")) {
                                                                                                customData.put("KevsLibraryBlockedInteractions", blocked);
                                                                                            }

                                                                                            if (type.equals("all")) {
                                                                                                blocked.putBoolean("left_click", true);
                                                                                                blocked.putBoolean("right_click", true);
                                                                                                blocked.putBoolean("placement", true);
                                                                                                blocked.putBoolean("entity_interact", true);
                                                                                                blocked.putBoolean("consume", true);
                                                                                                ctx.getSource().sendFeedback(() ->
                                                                                                        Text.literal("Blocked all interactions for item"), false);
                                                                                            } else {
                                                                                                blocked.putBoolean(type, true);
                                                                                                ctx.getSource().sendFeedback(() ->
                                                                                                        Text.literal("Blocked " + type + " for item"), false);
                                                                                            }

                                                                                            heldItem.set(net.minecraft.component.DataComponentTypes.CUSTOM_DATA,
                                                                                                    net.minecraft.component.type.NbtComponent.of(customData));

                                                                                            return 1;
                                                                                        })
                                                                        )
                                                        )
                                                        .then(
                                                                CommandManager.literal("remove")
                                                                        .then(
                                                                                CommandManager.argument("type", StringArgumentType.word())
                                                                                        .suggests((ctx, builder) -> {
                                                                                            builder.suggest("left_click");
                                                                                            builder.suggest("right_click");
                                                                                            builder.suggest("placement");
                                                                                            builder.suggest("entity_interact");
                                                                                            builder.suggest("consume");
                                                                                            builder.suggest("all");
                                                                                            return builder.buildFuture();
                                                                                        })
                                                                                        .executes(ctx -> {
                                                                                            ServerPlayerEntity player = EntityArgumentType.getPlayer(ctx, "player");
                                                                                            String type = StringArgumentType.getString(ctx, "type");
                                                                                            ItemStack heldItem = player.getMainHandStack();

                                                                                            if (heldItem.isEmpty()) {
                                                                                                ctx.getSource().sendError(Text.literal("Player must be holding an item"));
                                                                                                return 0;
                                                                                            }

                                                                                            NbtCompound customData = heldItem.getOrDefault(
                                                                                                    net.minecraft.component.DataComponentTypes.CUSTOM_DATA,
                                                                                                    net.minecraft.component.type.NbtComponent.DEFAULT
                                                                                            ).copyNbt();

                                                                                            if (!customData.contains("KevsLibraryBlockedInteractions")) {
                                                                                                ctx.getSource().sendError(Text.literal("Item has no blocked interactions"));
                                                                                                return 0;
                                                                                            }

                                                                                            NbtCompound blocked = customData.getCompound("KevsLibraryBlockedInteractions");

                                                                                            if (type.equals("all")) {
                                                                                                customData.remove("KevsLibraryBlockedInteractions");
                                                                                                ctx.getSource().sendFeedback(() ->
                                                                                                        Text.literal("Removed all blocked interactions from item"), false);
                                                                                            } else {
                                                                                                blocked.putBoolean(type, false);
                                                                                                ctx.getSource().sendFeedback(() ->
                                                                                                        Text.literal("Removed " + type + " block from item"), false);
                                                                                            }

                                                                                            heldItem.set(net.minecraft.component.DataComponentTypes.CUSTOM_DATA,
                                                                                                    net.minecraft.component.type.NbtComponent.of(customData));

                                                                                            return 1;
                                                                                        })
                                                                        )
                                                        )
                                                        .then(
                                                                CommandManager.literal("list")
                                                                        .executes(ctx -> {
                                                                            ServerPlayerEntity player = EntityArgumentType.getPlayer(ctx, "player");
                                                                            ItemStack heldItem = player.getMainHandStack();

                                                                            if (heldItem.isEmpty()) {
                                                                                ctx.getSource().sendError(Text.literal("Player must be holding an item"));
                                                                                return 0;
                                                                            }

                                                                            NbtCompound customData = heldItem.getOrDefault(
                                                                                    net.minecraft.component.DataComponentTypes.CUSTOM_DATA,
                                                                                    net.minecraft.component.type.NbtComponent.DEFAULT
                                                                            ).copyNbt();

                                                                            if (!customData.contains("KevsLibraryBlockedInteractions")) {
                                                                                ctx.getSource().sendFeedback(() ->
                                                                                        Text.literal("Item has no blocked interactions"), false);
                                                                                return 1;
                                                                            }

                                                                            NbtCompound blocked = customData.getCompound("KevsLibraryBlockedInteractions");
                                                                            StringBuilder message = new StringBuilder("Blocked interactions: ");
                                                                            boolean hasAny = false;

                                                                            for (String key : blocked.getKeys()) {
                                                                                if (blocked.getBoolean(key)) {
                                                                                    if (hasAny) message.append(", ");
                                                                                    message.append(key);
                                                                                    hasAny = true;
                                                                                }
                                                                            }

                                                                            if (!hasAny) {
                                                                                message.append("none");
                                                                            }

                                                                            String finalMessage = message.toString();
                                                                            ctx.getSource().sendFeedback(() -> Text.literal(finalMessage), false);
                                                                            return 1;
                                                                        })
                                                        )
                                        )
                        )

        );
    }
}