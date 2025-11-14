package net.pixeldreamstudios.kevslibrary.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.FlyingItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.pixeldreamstudios.kevslibrary.KevsDamageTypes;
import net.pixeldreamstudios.kevslibrary.KevsLibrary;

import java.util.List;

public class ArcaneShardEntity extends PersistentProjectileEntity implements FlyingItemEntity {

    private boolean homing = false;
    private static final double HOMING_SPEED = 1.6;
    private static final double HOMING_LERP = 0.4;

    public ArcaneShardEntity(EntityType<? extends PersistentProjectileEntity> type, World world) {
        super(type, world);
        this.setNoGravity(false);
        this.setCritical(false);
        this.pickupType = PickupPermission.DISALLOWED;
    }

    public static ArcaneShardEntity create(World world, LivingEntity owner, Vec3d direction, float baseRuptureDamage) {
        ArcaneShardEntity shard = new ArcaneShardEntity(KevsLibrary.ARCANE_SHARD, world);
        shard.setOwner(owner);
        shard.setDamage(baseRuptureDamage * 0.5f);
        shard.setVelocity(direction.normalize().multiply(0.5));
        return shard;
    }

    @Override
    public void tick() {
        super.tick();

        if (this.getWorld() instanceof ServerWorld world) {
            world.spawnParticles(ParticleTypes.END_ROD, this.getX(), this.getY(), this.getZ(), 1, 0, 0, 0, 0.01);
        }


        if (!homing && this.getVelocity().y < 0) {
            homing = true;
        }

        if (homing && !getWorld().isClient && getOwner() instanceof LivingEntity owner) {
            List<LivingEntity> targets = getWorld().getEntitiesByClass(
                    LivingEntity.class,
                    getBoundingBox().expand(16.0),
                    e -> e.isAlive() && !(e instanceof PlayerEntity) && !e.isTeammate(owner) && !e.equals(owner)
            );

            if (!targets.isEmpty()) {
                LivingEntity target = targets.get(random.nextInt(targets.size()));
                Vec3d toTarget = target.getPos()
                        .add(0, target.getHeight() * 0.6, 0)
                        .subtract(this.getPos())
                        .normalize();

                this.setVelocity(this.getVelocity().lerp(toTarget.multiply(HOMING_SPEED), HOMING_LERP));
            }
        }


        if (horizontalCollision || verticalCollision) {
            this.setNoClip(true);
        }
    }

    @Override
    protected void onEntityHit(EntityHitResult result) {
        if (!(getOwner() instanceof LivingEntity attacker)) return;
        if (!(result.getEntity() instanceof LivingEntity target)) return;
        if (target instanceof PlayerEntity) return;

        target.timeUntilRegen = 0;
        target.hurtTime = 0;
        target.damage(this.getDamageSources().create(KevsDamageTypes.ARCANE_SHARD, attacker), (float) this.getDamage());

        if (this.getWorld() instanceof ServerWorld world) {
            world.spawnParticles(ParticleTypes.CRIT, target.getX(), target.getY() + 1, target.getZ(), 5, 0.2, 0.2, 0.2, 0.01);
            world.playSound(null, target.getBlockPos(), SoundEvents.ENTITY_ENDER_EYE_DEATH, SoundCategory.PLAYERS, 0.6f, 1.2f);
        }

        discard();
    }

    @Override
    protected void onBlockHit(BlockHitResult hit) {

    }

    @Override
    protected ItemStack getDefaultItemStack() {
        return new ItemStack(Items.AMETHYST_SHARD);
    }

    @Override
    public ItemStack getStack() {
        return getDefaultItemStack();
    }

    @Override
    protected double getGravity() {
        return 0.05;
    }

    @Override
    public void onPlayerCollision(PlayerEntity player) {

    }

    public boolean canPickup() {
        return false;
    }
}
