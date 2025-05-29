package net.pixeldreamstudios.kevslibrary.util;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.pixeldreamstudios.kevslibrary.taming.UniversalTameable;

import java.util.Collection;

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

        );
    }
}
