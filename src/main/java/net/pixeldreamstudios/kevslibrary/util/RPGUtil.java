package net.pixeldreamstudios.kevslibrary.util;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.Collection;

public class RPGUtil
{

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher)
    {
        dispatcher.register(
                CommandManager.literal("rpgutility")
                        .requires(source -> source.hasPermissionLevel(2))
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
