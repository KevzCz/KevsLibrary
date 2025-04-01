
package net.pixeldreamstudios.kevslibrary.handler;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.pixeldreamstudios.kevslibrary.KevsDamageTypes;
import net.pixeldreamstudios.kevslibrary.KevsLibrary;
import net.pixeldreamstudios.kevslibrary.entity.MultistrikeArrowEntity;

import java.util.*;

public class MultistrikeHandler {
    private static final Set<UUID> handledProjectiles = Collections.newSetFromMap(new WeakHashMap<>());

    public static boolean tryMarkProjectile(UUID uuid) {
        return handledProjectiles.add(uuid); // true = first time seen
    }
    private static final Map<String, MultistrikeBomb> bombs = new HashMap<>();
    private static final Map<UUID, List<HoveringArrow>> hoveringArrows = new HashMap<>();
    private static final Map<UUID, LaunchTiming> launchDelays = new HashMap<>();

    public static void triggerMultistrike(LivingEntity attacker, LivingEntity target, float damage, ItemStack weaponUsed) {
        String key = attacker.getUuid() + "|" + target.getUuid();
        bombs.compute(key, (k, existing) -> {
            if (existing == null) {

                return new MultistrikeBomb(attacker, target, damage, weaponUsed);
            } else {

                existing.addStack(damage);
                return existing;
            }
        });
    }

    public static void spawnHoveringArrows(LivingEntity attacker, LivingEntity target, float baseDamage, PersistentProjectileEntity sourceProjectile, ItemStack weaponUsed) {
        if (!(attacker.getWorld() instanceof ServerWorld world)) return;

        EntityAttributeInstance countAttr = attacker.getAttributeInstance(KevsLibrary.MULTISTRIKE_COUNT);
        int count = countAttr != null ? (int) countAttr.getValue() : 1;

        EntityAttributeInstance dmgAttr = attacker.getAttributeInstance(KevsLibrary.MULTISTRIKE_DAMAGE);
        float multiplier = dmgAttr != null ? (float) dmgAttr.getValue() : 0.5f;

        float finalDamage = baseDamage * multiplier * 0.5f;
        List<HoveringArrow> arrows = hoveringArrows.computeIfAbsent(attacker.getUuid(), k -> new ArrayList<>());

        for (int i = 0; i < count; i++) {
            // 🆕 Use the actual MultistrikeArrowEntity
            MultistrikeArrowEntity arrow = new MultistrikeArrowEntity(world, attacker);
            arrow.setMultistrikeActive(true);
            arrow.setDamage(finalDamage);
            arrow.setSilent(true);
            arrow.setGlowing(true);
            arrow.setCritical(false);
            arrow.setNoGravity(true);
            arrow.pickupType = PersistentProjectileEntity.PickupPermission.DISALLOWED;

            arrow.setPosition(attacker.getX(), attacker.getY() + attacker.getHeight() + 2.0, attacker.getZ());
            arrow.setVelocity(Vec3d.ZERO);

            world.spawnEntity(arrow);

            double angleOffset = ((2 * Math.PI) / count) * i;
            arrows.add(new HoveringArrow(arrow, attacker, target, angleOffset));
        }
    }

    public static void tick(ServerWorld world) {
        // 🔥 Tick all multistrike bombs
        Iterator<Map.Entry<String, MultistrikeBomb>> bombIt = bombs.entrySet().iterator();
        while (bombIt.hasNext()) {
            Map.Entry<String, MultistrikeBomb> entry = bombIt.next();
            MultistrikeBomb bomb = entry.getValue();
            boolean done = bomb.tick(world);
            if (done) {
                bombIt.remove();
            }
        }

        // 💥 Tick all hovering arrows (your original code)
        Iterator<Map.Entry<UUID, List<HoveringArrow>>> it = hoveringArrows.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, List<HoveringArrow>> entry = it.next();
            UUID attackerId = entry.getKey();
            List<HoveringArrow> arrows = entry.getValue();

            arrows.removeIf(arrow -> arrow.tick(world));
            if (arrows.isEmpty()) {
                it.remove();
                launchDelays.remove(attackerId);
                continue;
            }

            LaunchTiming timing = launchDelays.computeIfAbsent(attackerId, id -> new LaunchTiming());
            if (timing.initialDelay > 0) {
                timing.initialDelay--;
                continue;
            }
            if (timing.perArrowDelay > 0) {
                timing.perArrowDelay--;
                continue;
            }

            for (HoveringArrow arrow : arrows) {
                if (!arrow.launched) {
                    arrow.launchArrow();
                    timing.perArrowDelay = 80;
                    break;
                }
            }
        }
    }


    private static class LaunchTiming {
        int initialDelay = 60;
        int perArrowDelay = 20;
    }

    private static class HoveringArrow {
        final PersistentProjectileEntity arrow;
        final LivingEntity attacker;
        final LivingEntity target;

        boolean launched = false;
        double orbitAngle = 0;
        final double orbitAngleOffset;
        final double orbitRadius = 1.5;

        HoveringArrow(PersistentProjectileEntity arrow, LivingEntity attacker, LivingEntity target, double angleOffset) {
            this.arrow = arrow;
            this.attacker = attacker;
            this.target = target;
            this.orbitAngleOffset = angleOffset;
        }

        boolean tick(ServerWorld world) {
            if (!arrow.isAlive()) return true;
            if (!attacker.isAlive() || !target.isAlive()) {
                arrow.discard();
                return true;
            }

            if (!launched) {
                orbitAngle += 0.15;
                double angle = orbitAngle + orbitAngleOffset;
                Vec3d orbitCenter = attacker.getPos().add(0, attacker.getHeight() + 1.5, 0);
                Vec3d orbitTargetPos = orbitCenter.add(Math.cos(angle) * orbitRadius, 0, Math.sin(angle) * orbitRadius);
                arrow.setPosition(arrow.getPos().lerp(orbitTargetPos, 0.3));
                arrow.setYaw(0f);
                arrow.setPitch(-90f);
                return false;
            }

            Vec3d toTarget = target.getPos().add(0, target.getHeight() * 0.5, 0).subtract(arrow.getPos());
            arrow.setVelocity(toTarget.normalize().multiply(2.8));
            return false;
        }

        void launchArrow() {
            this.launched = true;
            arrow.setNoGravity(false);
        }



    }

    private static class MultistrikeBomb {
        private final LivingEntity target;
        private final LivingEntity source;
        private final ItemStack weaponUsed;

        private float totalDamage;
        private int totalStrikes = 0;
        private int triggerCount = 0;
        private int ticksUntilDetonate = 60;
        private float diminishingTimerAdd = 0.5f;
        private boolean detonating = false;
        private int ticksUntilNextHit = 0;

        public MultistrikeBomb(LivingEntity source, LivingEntity target, float damage, ItemStack weaponUsed) {
            this.source = source;
            this.target = target;
            this.weaponUsed = weaponUsed.copy(); // snapshot
            this.totalDamage = damage;
            this.totalStrikes = getStrikeCountFromAttributes(source);
            this.triggerCount = 1;

        }

        public void addStack(float damage) {
            totalDamage += damage;
            triggerCount++;
            int addedStrikes = getStrikeCountFromAttributes(source);
            totalStrikes += addedStrikes;
            ticksUntilDetonate += (int) (20 * Math.max(diminishingTimerAdd, 0));
            diminishingTimerAdd -= 0.1f;


        }

        public boolean tick(ServerWorld world) {
            if (!target.isAlive()) {

                return true;
            }

            if (!detonating) {
                ticksUntilDetonate--;

                if (ticksUntilDetonate == 20) {

                    world.spawnParticles(ParticleTypes.SONIC_BOOM, target.getX(), target.getY() + 1.0, target.getZ(), 2, 0.5, 0.3, 0.5, 0.05);
                }

                if (ticksUntilDetonate > 0) return false;

                detonating = true;
                ticksUntilNextHit = 2;


                world.spawnParticles(ParticleTypes.NOTE, target.getX(), target.getY() + target.getHeight() + 0.6, target.getZ(), 1, 0, 0, 0, 0);
                return false;
            }

            if (--ticksUntilNextHit > 0) return false;
            if (!target.isAlive()) {

                return true;
            }

            if (totalStrikes <= 0) {

                return true;
            }

            float avgDamage = totalDamage / triggerCount;
            EntityAttributeInstance dmgAttr = source.getAttributeInstance(KevsLibrary.MULTISTRIKE_DAMAGE);
            float multiplier = dmgAttr != null ? (float) dmgAttr.getValue() : 0.5f;
            float damage = avgDamage * multiplier;

            DamageSource source = world.getDamageSources().create(KevsDamageTypes.MULTISTRIKE, this.source);
            target.hurtTime = 0;
            target.timeUntilRegen = 0;

            boolean hit = target.damage(source, damage);


            if (hit) {
                OnHitEffectHandler.withMultistrikeContext(() -> {
                    OnHitEffectHandler.triggerAll(this.source, target, damage);
                });

                if (this.source instanceof PlayerEntity) {
                    EnchantmentHelper.onTargetDamaged(world, target, source);
                    ItemEnchantmentsComponent component = weaponUsed.get(DataComponentTypes.ENCHANTMENTS);
                    if (component != null && !component.getEnchantments().isEmpty()) {

                    }
                }
            }

            world.playSound(null, target.getBlockPos(), SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1f, 1f);
            world.spawnParticles(ParticleTypes.SONIC_BOOM, target.getX(), target.getY() + 1, target.getZ(), 2, 0.5, 0.3, 0.5, 0.05);

            totalStrikes--;
            ticksUntilNextHit = 2;
            return totalStrikes <= 0;
        }

        private int getStrikeCountFromAttributes(LivingEntity source) {
            EntityAttributeInstance countAttr = source.getAttributeInstance(KevsLibrary.MULTISTRIKE_COUNT);
            int val = countAttr != null ? (int) countAttr.getValue() : 1;

            return Math.max(val, 1);
        }
    }


        private int getStrikeCountFromAttributes(LivingEntity source) {
            EntityAttributeInstance countAttr = source.getAttributeInstance(KevsLibrary.MULTISTRIKE_COUNT);
            return countAttr != null ? (int) countAttr.getValue() : 1;
        }

    }

