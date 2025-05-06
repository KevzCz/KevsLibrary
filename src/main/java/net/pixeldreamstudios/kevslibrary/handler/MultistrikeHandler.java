    
    package net.pixeldreamstudios.kevslibrary.handler;
    
    import net.minecraft.component.DataComponentTypes;
    import net.minecraft.component.type.ItemEnchantmentsComponent;
    import net.minecraft.enchantment.EnchantmentHelper;
    import net.minecraft.entity.Entity;
    import net.minecraft.entity.EntityType;
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

    import static java.lang.Math.clamp;

    public class MultistrikeHandler {
        private static final Set<UUID> handledProjectiles = Collections.newSetFromMap(new WeakHashMap<>());
    
        public static boolean tryMarkProjectile(UUID uuid) {
            return handledProjectiles.add(uuid);
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
                EntityType<?> type = sourceProjectile.getType();
                Entity newArrowEntity = type.create(world);

                if (!(newArrowEntity instanceof PersistentProjectileEntity arrow)) {
                    continue;
                }

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
                int delay = 50 + (i * 15);
                arrows.add(new HoveringArrow(arrow, attacker, target, angleOffset, delay));
            }
    
        }
    
        public static void tick(ServerWorld world) {
            Iterator<Map.Entry<String, MultistrikeBomb>> bombIt = bombs.entrySet().iterator();
            while (bombIt.hasNext()) {
                Map.Entry<String, MultistrikeBomb> entry = bombIt.next();
                MultistrikeBomb bomb = entry.getValue();
                boolean done = bomb.tick(world);
                if (done) {
                    bombIt.remove();
                }
            }

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
                for (HoveringArrow arrow : arrows) {
                    if (!arrow.launched && !arrow.shouldStartLaunching) {
                        arrow.shouldStartLaunching = true;
                        break;
                    }
                }

            }
        }
    
    
        private static class LaunchTiming {
            int initialDelay = 60;
            int perArrowDelay = 80;
        }

        private static class HoveringArrow {
            final PersistentProjectileEntity arrow;
            final LivingEntity attacker;
            final LivingEntity target;

            boolean launched = false;
            boolean shouldStartLaunching = false;

            int ticksSinceSpawn = 0;
            int ticksSinceLaunch = 0;
            final int delayBeforeLaunch;

            double orbitAngle = 0;
            final double orbitAngleOffset;
            final double orbitRadius = 1.5;

            private static final int MAX_LIFESPAN = 1000;
            private static final int HOMING_DELAY = 20;

            HoveringArrow(PersistentProjectileEntity arrow, LivingEntity attacker, LivingEntity target, double angleOffset, int delayBeforeLaunch) {
                this.arrow = arrow;
                this.attacker = attacker;
                this.target = target;
                this.orbitAngleOffset = angleOffset;
                this.delayBeforeLaunch = delayBeforeLaunch;
            }

            boolean tick(ServerWorld world) {
                if (!arrow.isAlive()) return true;
                if (!attacker.isAlive() || !target.isAlive() || attacker.isRemoved()) {
                    arrow.discard();
                    return true;
                }
                if (!attacker.getWorld().equals(arrow.getWorld())) {
                    arrow.discard();
                    return true;
                }

                ticksSinceSpawn++;
                if (ticksSinceSpawn > MAX_LIFESPAN) {
                    arrow.discard();
                    return true;
                }

                if (!launched) {
                    if (!shouldStartLaunching || ticksSinceSpawn < delayBeforeLaunch) {
                        orbitAngle += 0.15;
                        double angle = orbitAngle + orbitAngleOffset;
                        Vec3d orbitCenter = attacker.getPos().add(0, attacker.getHeight() + 1.5, 0);
                        Vec3d orbitTargetPos = orbitCenter.add(Math.cos(angle) * orbitRadius, 0, Math.sin(angle) * orbitRadius);
                        arrow.setPosition(arrow.getPos().lerp(orbitTargetPos, 0.3));
                        arrow.setYaw(0f);
                        arrow.setPitch(-90f);
                        return false;
                    }

                    launchArrowUpward();
                    return false;
                }

                ticksSinceLaunch++;
                if (ticksSinceLaunch >= HOMING_DELAY) {
                    Vec3d toTarget = target.getPos().add(0, target.getHeight() * 0.5, 0).subtract(arrow.getPos());
                    Vec3d newVelocity = toTarget.normalize().multiply(0.45);
                    arrow.setVelocity(arrow.getVelocity().lerp(newVelocity, 0.3));

                    world.spawnParticles(ParticleTypes.END_ROD, arrow.getX(), arrow.getY(), arrow.getZ(), 1, 0, 0, 0, 0.001);
                }

                if (arrow.getBoundingBox().intersects(target.getBoundingBox())) {
                    float damage = (float) arrow.getDamage();
                    DamageSource source = attacker.getDamageSources().create(KevsDamageTypes.MULTISTRIKE_RANGED, attacker);
                    boolean hit = target.damage(source, damage);

                    if (hit) {
                        OnHitEffectHandler.withMultistrikeContext(() -> {
                            OnHitEffectHandler.triggerAll(attacker, target, damage);
                        });

                        SoulLinkTracker.getGroup(target).ifPresent(linkData -> {
                            SoulLinkHandler.handleLinkedDamage(
                                    linkData.attacker(),
                                    target,
                                    damage,
                                    linkData.group(),
                                    linkData.soulPower()
                            );
                        });

                        ((ServerWorld) target.getWorld()).spawnParticles(
                                ParticleTypes.SONIC_BOOM,
                                target.getX(), target.getY() + 1, target.getZ(),
                                5, 0.3, 0.3, 0.3, 0.01
                        );
                    }

                    arrow.discard();
                    return true;
                }

                return false;
            }

            void launchArrowUpward() {
                this.launched = true;
                this.ticksSinceLaunch = 0;
                arrow.setNoGravity(false);
                arrow.setVelocity(0, 1.5, 0);

                arrow.getWorld().playSound(null, arrow.getBlockPos(), SoundEvents.ENTITY_ARROW_SHOOT, SoundCategory.PLAYERS, 1.0f, 1.2f);
            }
        }





        private static class MultistrikeBomb {
            private final LivingEntity target;
            private final LivingEntity source;
            private final ItemStack weaponUsed;
            private int strikeIndex = 0;
            private float totalDamage;
            private int totalStrikes = 0;
            private int triggerCount = 0;
            private boolean detonating = false;
            private float diminishingTimerAdd = 0.5f;
    
            private int ticksUntilDetonate = 60;
            private long lastStrikeTime = 0;
    
            public MultistrikeBomb(LivingEntity source, LivingEntity target, float damage, ItemStack weaponUsed) {
                this.source = source;
                this.target = target;
                this.weaponUsed = weaponUsed.copy();
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
                if (!target.isAlive()) return true;

                long time = world.getTime();

                if (!detonating) {
                    ticksUntilDetonate--;
                    if (ticksUntilDetonate == 20) {
                        world.spawnParticles(ParticleTypes.SONIC_BOOM, target.getX(), target.getY() + 1, target.getZ(), 2, 0.5, 0.3, 0.5, 0.05);
                    }
                    if (ticksUntilDetonate > 0) return false;

                    detonating = true;
                    lastStrikeTime = time;
                    world.spawnParticles(ParticleTypes.NOTE, target.getX(), target.getY() + target.getHeight() + 0.6, target.getZ(), 1, 0, 0, 0, 0);
                    return false;
                }


                int delay;
                if (strikeIndex < 5) {
                    delay = 3 - strikeIndex;
                } else {
                    delay = 0;
                }

                if (delay > 0 && time - lastStrikeTime < delay) return false;
                lastStrikeTime = time;

                if (!target.isAlive()) return true;
                if (totalStrikes <= 0) return true;

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
                        SoulLinkTracker.getGroup(target).ifPresent(linkData -> {
                            SoulLinkHandler.handleLinkedDamage(
                                    linkData.attacker(),
                                    target,
                                    damage,
                                    linkData.group(),
                                    linkData.soulPower()
                            );
                        });
                    });
                }
                world.playSound(null, target.getBlockPos(), SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1f, 1f);
                world.spawnParticles(ParticleTypes.SONIC_BOOM, target.getX(), target.getY() + 1, target.getZ(), 2, 0.5, 0.3, 0.5, 0.05);

                totalStrikes--;
                strikeIndex++;

                return totalStrikes <= 0;
            }


            private int getStrikeCountFromAttributes(LivingEntity source) {
                EntityAttributeInstance countAttr = source.getAttributeInstance(KevsLibrary.MULTISTRIKE_COUNT);
                return countAttr != null ? (int) countAttr.getValue() : 1;
            }
        }
    
    
            private int getStrikeCountFromAttributes(LivingEntity source) {
                EntityAttributeInstance countAttr = source.getAttributeInstance(KevsLibrary.MULTISTRIKE_COUNT);
                return countAttr != null ? (int) countAttr.getValue() : 1;
            }
    
        }
    
