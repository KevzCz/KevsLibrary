package net.pixeldreamstudios.kevslibrary.handler;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.pixeldreamstudios.kevslibrary.config.KevsLibraryConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class WeaponSwitchHandler {
    private static final WeaponSwitchHandler INSTANCE = new WeaponSwitchHandler();

    private final Map<UUID, ItemStack> lastHeldItem = new HashMap<>();
    private final Map<UUID, Long> lastSwitchTime = new HashMap<>();
    private final Map<UUID, Integer> lastAttackTick = new HashMap<>();
    private final Map<UUID, Long> lastBonusConsumedTime = new HashMap<>();
    private final Map<UUID, Boolean> hasUsedDrawTime = new HashMap<>();

    private WeaponSwitchHandler() {}

    public static WeaponSwitchHandler getInstance() {
        return INSTANCE;
    }

    public void onItemSwitch(PlayerEntity player, ItemStack newItem) {
        UUID uuid = player.getUuid();
        ItemStack previousItem = lastHeldItem.get(uuid);

        if (previousItem == null || !ItemStack.areItemsEqual(previousItem, newItem)) {
            lastHeldItem.put(uuid, newItem.copy());
            lastSwitchTime.put(uuid, System.currentTimeMillis());
            lastAttackTick.remove(uuid);
            hasUsedDrawTime.put(uuid, false);
        }
    }

    public boolean canUseFirstHitBonus(PlayerEntity player) {
        UUID uuid = player.getUuid();
        Long lastSwitch = lastSwitchTime.get(uuid);

        if (lastSwitch == null) {
            return false;
        }

        KevsLibraryConfig config = KevsLibraryConfig.getInstance();
        long currentTime = System.currentTimeMillis();
        long timeSinceSwitch = currentTime - lastSwitch;

        if (timeSinceSwitch > config.weapon_switching_config.first_hit_window_ms) {
            return false;
        }

        Long lastConsumed = lastBonusConsumedTime.get(uuid);
        if (lastConsumed != null) {
            long timeSinceConsumed = currentTime - lastConsumed;
            if (timeSinceConsumed < config.weapon_switching_config.first_hit_cooldown_ms) {
                return false;
            }
        }

        Integer lastAttack = lastAttackTick.get(uuid);
        return lastAttack == null;
    }

    public void consumeFirstHitBonus(PlayerEntity player) {
        UUID uuid = player.getUuid();
        lastAttackTick.put(uuid, player.age);
        lastBonusConsumedTime.put(uuid, System.currentTimeMillis());
    }

    public boolean isWithinSameAttackSwing(PlayerEntity player) {
        UUID uuid = player.getUuid();
        Integer lastAttack = lastAttackTick.get(uuid);

        if (lastAttack == null) {
            return false;
        }

        return player.age == lastAttack;
    }

    public boolean canApplyDrawTime(PlayerEntity player) {
        UUID uuid = player.getUuid();
        boolean hasUsed = hasUsedDrawTime.getOrDefault(uuid, true);
        return !hasUsed;
    }

    public void consumeDrawTime(PlayerEntity player) {
        UUID uuid = player.getUuid();
        hasUsedDrawTime.put(uuid, true);
    }

    public long getTimeSinceSwitch(PlayerEntity player) {
        UUID uuid = player.getUuid();
        Long lastSwitch = lastSwitchTime.get(uuid);
        if (lastSwitch == null) {
            return Long.MAX_VALUE;
        }
        return System.currentTimeMillis() - lastSwitch;
    }

    public void cleanup(UUID playerUuid) {
        lastHeldItem.remove(playerUuid);
        lastSwitchTime.remove(playerUuid);
        lastAttackTick.remove(playerUuid);
        lastBonusConsumedTime.remove(playerUuid);
        hasUsedDrawTime.remove(playerUuid);
    }
}