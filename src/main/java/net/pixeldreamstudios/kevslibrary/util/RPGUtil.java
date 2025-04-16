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
                        // ✅ /rpgutility after
                        .then(
                                CommandManager.literal("after")
                                        .then(
                                                CommandManager.argument("time", IntegerArgumentType.integer(1))
                                                        .then(
                                                                CommandManager.argument("target", EntityArgumentType.players())
                                                                        .then(
                                                                                CommandManager.literal("run")
                                                                                        .then(
                                                                                                CommandManager.literal("as")
                                                                                                        .then(
                                                                                                                CommandManager.argument("asTarget", EntityArgumentType.entity())
                                                                                                                        .then(
                                                                                                                                CommandManager.literal("at")
                                                                                                                                        .then(
                                                                                                                                                CommandManager.argument("atTarget", EntityArgumentType.entity())
                                                                                                                                                        .then(
                                                                                                                                                                CommandManager.literal("do")
                                                                                                                                                                        .then(
                                                                                                                                                                                CommandManager.argument("command", StringArgumentType.greedyString())
                                                                                                                                                                                        .executes(context ->
                                                                                                                                                                                                {
                                                                                                                                                                                                    int ticks = IntegerArgumentType.getInteger(context, "time");
                                                                                                                                                                                                    Collection<ServerPlayerEntity> targets = EntityArgumentType.getPlayers(context, "target");
                                                                                                                                                                                                    ServerCommandSource runAs = context.getSource().withEntity(
                                                                                                                                                                                                            EntityArgumentType.getEntity(context, "asTarget"));
                                                                                                                                                                                                    ServerCommandSource runAt = runAs.withPosition(
                                                                                                                                                                                                            EntityArgumentType.getEntity(context, "atTarget").getPos());
                                                                                                                                                                                                    String command = StringArgumentType.getString(context, "command");

                                                                                                                                                                                                    for (ServerPlayerEntity target : targets)
                                                                                                                                                                                                    {
                                                                                                                                                                                                        DelayedExecutor.runLater(() ->
                                                                                                                                                                                                                        context.getSource().getServer().getCommandManager().executeWithPrefix(runAt, command),
                                                                                                                                                                                                                ticks
                                                                                                                                                                                                        );
                                                                                                                                                                                                    }

                                                                                                                                                                                                    context.getSource().sendFeedback(() ->
                                                                                                                                                                                                            Text.literal("Scheduled command to run in " + ticks + " ticks."), false);
                                                                                                                                                                                                    return 1;
                                                                                                                                                                                                }
                                                                                                                                                                                        )
                                                                                                                                                                        )
                                                                                                                                                        )
                                                                                                                                        )
                                                                                                                        )
                                                                                                        )
                                                                                        )
                                                                        )
                                                        )
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
                                CommandManager.literal("repeat")
                                        .then(
                                                CommandManager.argument("interval", IntegerArgumentType.integer(1))
                                                        .then(
                                                                CommandManager.argument("times", IntegerArgumentType.integer(0))
                                                                        .then(
                                                                                CommandManager.argument("command", StringArgumentType.greedyString())
                                                                                        .executes(ctx ->
                                                                                        {
                                                                                            int interval = IntegerArgumentType.getInteger(ctx, "interval");
                                                                                            int times = IntegerArgumentType.getInteger(ctx, "times");
                                                                                            String command = StringArgumentType.getString(ctx, "command");
                                                                                            var source = ctx.getSource();

                                                                                            if (times == 0)
                                                                                            {
                                                                                                RepeatRegistry.registerInfinite(() ->
                                                                                                                source.getServer().getCommandManager().executeWithPrefix(source, command),
                                                                                                        interval
                                                                                                );
                                                                                                source.sendFeedback(() ->
                                                                                                        Text.literal("Started infinite repeat every " + interval + " ticks."), false);
                                                                                            }
                                                                                            else
                                                                                            {
                                                                                                for (int i = 0; i < times; i++)
                                                                                                {
                                                                                                    int delay = i * interval;
                                                                                                    DelayedExecutor.runLater(() ->
                                                                                                                    source.getServer().getCommandManager().executeWithPrefix(source, command),
                                                                                                            delay
                                                                                                    );
                                                                                                }

                                                                                                source.sendFeedback(() ->
                                                                                                        Text.literal("Scheduled repeat " + times + " times every " + interval + " ticks."), false);
                                                                                            }

                                                                                            return 1;
                                                                                        })
                                                                        )
                                                        )
                                        )
                        )
                        .then(
                                CommandManager.literal("clear")
                                        .then(
                                                CommandManager.literal("repeat")
                                                        .executes(ctx ->
                                                        {
                                                            RepeatRegistry.clearAll();
                                                            ctx.getSource().sendFeedback(() ->
                                                                    Text.literal("Cleared all active infinite repeats."), false);
                                                            return 1;
                                                        })
                                        )
                        )
        );
    }
}
