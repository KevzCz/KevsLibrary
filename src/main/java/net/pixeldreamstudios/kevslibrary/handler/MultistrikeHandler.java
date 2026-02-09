
    package net.pixeldreamstudios.kevslibrary.handler;

    import net.minecraft.entity.Entity;
    import net.minecraft.entity.EntityType;
    import net.minecraft.entity.LivingEntity;
    import net.minecraft.entity.attribute.EntityAttributeInstance;
    import net.minecraft.entity.damage.DamageSource;
    import net.minecraft.entity.passive.TameableEntity;
    import net.minecraft.entity.projectile.PersistentProjectileEntity;
    import net.minecraft.entity.projectile.TridentEntity;
    import net.minecraft.item.ItemStack;
    import net.minecraft.particle.ParticleTypes;
    import net.minecraft.server.world.ServerWorld;
    import net.minecraft.sound.SoundCategory;
    import net.minecraft.sound.SoundEvents;
    import net.minecraft.text.Text;
    import net.minecraft.util.math.Vec3d;
    import net.pixeldreamstudios.kevslibrary.KevsDamageTypes;
    import net.pixeldreamstudios.kevslibrary.KevsLibrary;
    import net.spell_engine.entity.SpellProjectile;

    import java.util.*;

    public class MultistrikeHandler {
        private static final Set<UUID> handledProjectiles = Collections.newSetFromMap(new WeakHashMap<>());
        private static final Map<UUID, List<HoveringSpellProjectile>> hoveringSpellProjectiles = new HashMap<>();
        public static boolean tryMarkProjectile(UUID uuid) {
            return handledProjectiles.add(uuid);
        }
        private static final Map<String, MultistrikeBomb> bombs = new HashMap<>();
        private static final Map<UUID, List<HoveringArrow>> hoveringArrows = new HashMap<>();
        private static class HoveringSpellProjectile {
            final SpellProjectile projectile;
            final LivingEntity attacker;
            final LivingEntity target;

            int ticksSinceLaunch = 0;
            private static final int MAX_LIFESPAN = 200;
            private static final double HOMING_FORCE = 0.25;
            private static final double SPEED = 1.5;

            HoveringSpellProjectile(SpellProjectile projectile, LivingEntity attacker, LivingEntity target) {
                this.projectile = projectile;
                this.attacker = attacker;
                this.target = target;
            }

            boolean tick(ServerWorld world) {
                if (!projectile.isAlive() || !target.isAlive() || target.isRemoved()) {
                    projectile.discard();
                    return true;
                }

                ticksSinceLaunch++;
                if (ticksSinceLaunch > MAX_LIFESPAN) {
                    projectile.discard();
                    return true;
                }

                Vec3d toTarget = target.getPos().add(0, target.getHeight() * 0.5, 0).subtract(projectile.getPos());
                Vec3d homing = toTarget.normalize().multiply(HOMING_FORCE);
                projectile.setVelocity(
                        projectile.getVelocity().add(homing).normalize().multiply(SPEED)
                );

                world.spawnParticles(ParticleTypes.END_ROD, projectile.getX(), projectile.getY(), projectile.getZ(), 1, 0, 0, 0, 0.001);

                return false;
            }
        }

        public static void triggerMultistrike(LivingEntity attacker, LivingEntity target, float damage, ItemStack weaponUsed) {
            if (!isValidMultistrikeTarget(attacker, target)) return;
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

        public static void spawnHoveringProjectiles(LivingEntity attacker, LivingEntity target, float baseDamage, Entity sourceProjectile, ItemStack weaponUsed) {
            if (!(attacker.getWorld() instanceof ServerWorld world)) return;

            EntityAttributeInstance countAttr = attacker.getAttributeInstance(KevsLibrary.MULTISTRIKE_COUNT);
            int count = countAttr != null ? (int) countAttr.getValue() : 1;

            EntityAttributeInstance dmgAttr = attacker.getAttributeInstance(KevsLibrary.MULTISTRIKE_DAMAGE);
            float multiplier = dmgAttr != null ? (float) ((dmgAttr.getValue() - 100.0) / 100.0 + 1.0) : 0.5f;

            float finalDamage = baseDamage * multiplier * 0.6f;

            Vec3d forward = attacker.getRotationVec(1.0f).normalize();
            Vec3d up = new Vec3d(0, 1, 0);
            Vec3d right = forward.crossProduct(up).normalize();

            for (int i = 0; i < count; i++) {
                Vec3d spawnPos;
                boolean fromRight;

                if (count >= 3) {
                    double angle = (2 * Math.PI / count) * i;
                    Vec3d side = right.multiply(Math.sin(angle)).normalize();
                    spawnPos = attacker.getPos().add(side.multiply(1.0)).add(0, attacker.getHeight() * 0.6, 0);
                    fromRight = side.dotProduct(right) > 0;
                } else {
                    Vec3d side = (i % 2 == 0 ? right : right.multiply(-1));
                    spawnPos = attacker.getPos()
                            .add(side.multiply(1.0))
                            .add(0, attacker.getHeight() * 0.6, 0);
                    fromRight = i % 2 == 0;
                }

                if (sourceProjectile instanceof SpellProjectile spellProj) {
                    float msMultiplier = multiplier;

                    var baseCtx = spellProj.getImpactContext();
                    var scaledCtx = baseCtx.distance(baseCtx.distance() * msMultiplier);

                    SpellProjectile newSpell = new SpellProjectile(
                            world,
                            attacker,
                            spawnPos.x, spawnPos.y, spawnPos.z,
                            spellProj.getBehaviour(),
                            spellProj.getSpellEntry(),
                            scaledCtx,
                            spellProj.mutablePerks().copy()
                    );

                    Vec3d toTarget = target.getPos().add(0, target.getHeight() * 0.5, 0).subtract(attacker.getPos());
                    Vec3d forwardDir = toTarget.normalize();
                    Vec3d sideVec = forwardDir.crossProduct(new Vec3d(0, 1, 0)).normalize();
                    Vec3d offsetCurve = sideVec.multiply(fromRight ? 0.6 : -0.6);
                    Vec3d launchDir = forwardDir.add(offsetCurve).normalize().multiply(1.5);

                    newSpell.setVelocity(launchDir);
                    newSpell.setYaw((float) (Math.toDegrees(Math.atan2(launchDir.z, launchDir.x)) - 90));
                    newSpell.setPitch((float) (-Math.toDegrees(Math.atan2(launchDir.y, launchDir.horizontalLength()))));
                    newSpell.setHeadYaw(newSpell.getYaw());
                    newSpell.setBodyYaw(newSpell.getYaw());

                    newSpell.setSilent(true);
                    newSpell.setGlowing(true);
                    newSpell.addCommandTag("multistrike_spell");

                    world.spawnEntity(newSpell);
                    hoveringSpellProjectiles
                            .computeIfAbsent(attacker.getUuid(), k -> new ArrayList<>())
                            .add(new HoveringSpellProjectile(newSpell, attacker, target));
                    continue;
                }

                if (sourceProjectile instanceof PersistentProjectileEntity projectile) {
                    EntityType<?> type = projectile.getType();
                    Entity newArrowEntity = type.create(world);
                    if (!(newArrowEntity instanceof PersistentProjectileEntity arrow)) continue;

                    arrow.setOwner(attacker);

                    float dmgToSet = (projectile instanceof TridentEntity)
                            ? baseDamage * multiplier
                            : finalDamage;

                    arrow.setCritical(true);
                    arrow.setDamage(dmgToSet);
                    arrow.setSilent(true);
                    arrow.setGlowing(true);
                    arrow.setNoGravity(true);
                    arrow.pickupType = PersistentProjectileEntity.PickupPermission.DISALLOWED;

                    arrow.setPosition(spawnPos);
                    arrow.setVelocity(Vec3d.ZERO);
                    world.spawnEntity(arrow);
                    arrow.pickupType = PersistentProjectileEntity.PickupPermission.DISALLOWED;
                    arrow.setCustomNameVisible(false);
                    arrow.setCustomName(Text.of("multistrike_arrow"));
                    arrow.addCommandTag("multistrike_arrow");

                    double angleOffset = ((2 * Math.PI) / count) * i;
                    int delay = 30 + (i * 10);
                    hoveringArrows
                            .computeIfAbsent(attacker.getUuid(), k -> new ArrayList<>())
                            .add(new HoveringArrow(arrow, attacker, target, angleOffset, delay, fromRight));
                } else {

                    MultistrikeHandler.triggerMultistrike(attacker, target, baseDamage, weaponUsed);
                    break;
                }
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
            Iterator<Map.Entry<UUID, List<HoveringSpellProjectile>>> spellIt = hoveringSpellProjectiles.entrySet().iterator();
            while (spellIt.hasNext()) {
                Map.Entry<UUID, List<HoveringSpellProjectile>> entry = spellIt.next();
                List<HoveringSpellProjectile> projectiles = entry.getValue();
                projectiles.removeIf(p -> p.tick(world));
                if (projectiles.isEmpty()) {
                    spellIt.remove();
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
                }


            }
            world.iterateEntities().forEach(entity -> {
                if (entity instanceof PersistentProjectileEntity ppe &&
                        ppe.getCommandTags().contains("multistrike_arrow") &&
                        ppe.age > 200 &&
                        (ppe.getOwner() == null || ppe.isRemoved())) {
                    ppe.discard();
                }
            });

        }


        private static class HoveringArrow {
            final PersistentProjectileEntity arrow;
            final LivingEntity attacker;
            final LivingEntity target;

            final boolean fromRightSide;

            boolean launched = false;

            int ticksSinceSpawn = 0;
            private static final int MAX_LIFESPAN = 400;
            private static final double INITIAL_SPEED = 1.5;
            private static final double HOMING_FORCE = 0.3;
            private static final double TARGET_RADIUS = 0.6;

            private int ticksSinceLaunch = 0;
            HoveringArrow(PersistentProjectileEntity arrow, LivingEntity attacker, LivingEntity target, double angleOffset, int delayBeforeLaunch, boolean fromRightSide) {
                this.arrow = arrow;
                this.attacker = attacker;
                this.target = target;
                this.fromRightSide = fromRightSide;
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
                    launchArrow(world);
                    launched = true;
                }

                if (launched) {
                    ticksSinceLaunch++;

                    if (ticksSinceLaunch >= 10) {
                        Vec3d toTarget = target.getPos()
                                .add(0, target.getHeight() * 0.8, 0)
                                .subtract(arrow.getPos());

                        Vec3d homing = toTarget.normalize().multiply(HOMING_FORCE);

                        arrow.setVelocity(
                                arrow.getVelocity().add(homing).normalize().multiply(INITIAL_SPEED)
                        );
                    }


                    world.spawnParticles(ParticleTypes.END_ROD, arrow.getX(), arrow.getY(), arrow.getZ(), 1, 0, 0, 0, 0.001);

                    if (arrow.collidesWith(target)) {
                        if (!isValidMultistrikeTarget(attacker, target)) {
                            arrow.discard();
                            return true;
                        }
                        float damage = (float) arrow.getDamage();
                        DamageSource source = attacker.getDamageSources().create(KevsDamageTypes.MULTISTRIKE_RANGED, attacker);
                        int prevRegen = target.timeUntilRegen;
                        int prevHurt = target.hurtTime;
                        target.timeUntilRegen = 0;
                        target.hurtTime = 0;
                        boolean hit = target.damage(source, damage);
                        target.timeUntilRegen = prevRegen;
                        target.hurtTime = prevHurt;

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

                            world.spawnParticles(
                                    ParticleTypes.SONIC_BOOM,
                                    target.getX(), target.getY() + 1, target.getZ(),
                                    5, 0.3, 0.3, 0.3, 0.01
                            );
                        }

                        arrow.discard();
                        return true;
                    }

                }


                return false;
            }

            void launchArrow(ServerWorld world) {
                arrow.setNoGravity(false);

                Vec3d toTarget = target.getPos().add(0, target.getHeight() * 0.5, 0).subtract(attacker.getPos());
                Vec3d forward = toTarget.normalize();
                Vec3d side = forward.crossProduct(new Vec3d(0, 1, 0)).normalize();
                Vec3d offsetCurve = side.multiply(fromRightSide ? 0.6 : -0.6);

                Vec3d launchDirection = forward.add(offsetCurve).normalize().multiply(INITIAL_SPEED);
                arrow.setVelocity(launchDirection);

                arrow.getWorld().playSound(null, arrow.getBlockPos(), SoundEvents.ENTITY_ARROW_SHOOT, SoundCategory.PLAYERS, 0.4f, 0.4f);
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
                if (!isValidMultistrikeTarget(source, target)) return true;
                float avgDamage = totalDamage / triggerCount;
                EntityAttributeInstance dmgAttr = source.getAttributeInstance(KevsLibrary.MULTISTRIKE_DAMAGE);
                float multiplier = dmgAttr != null ? (float) (dmgAttr.getValue() / 100.0) : 1.0f;
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
                if (strikeIndex == 0) {
                    world.playSound(null, target.getBlockPos(), SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1f, 1f);
                }
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

            private static boolean isValidMultistrikeTarget(LivingEntity attacker, LivingEntity target) {
                return target.isAlive()
                        && !target.equals(attacker)
                        && !target.isTeammate(attacker)
                        && (!(target instanceof TameableEntity tameable) || !tameable.isTamed());
            }


        }

