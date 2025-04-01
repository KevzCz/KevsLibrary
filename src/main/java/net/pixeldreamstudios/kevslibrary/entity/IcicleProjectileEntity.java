package net.pixeldreamstudios.kevslibrary.entity;

import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.FlyingItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
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
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.pixeldreamstudios.kevslibrary.KevsDamageTypes;
import net.pixeldreamstudios.kevslibrary.KevsLibrary;

public class IcicleProjectileEntity extends PersistentProjectileEntity implements FlyingItemEntity {
    private float icicleDamage = 2.0f;

    public IcicleProjectileEntity(EntityType<? extends PersistentProjectileEntity> entityType, World world) {
        super(entityType, world);
        this.pickupType = PickupPermission.DISALLOWED;
    }

    public static IcicleProjectileEntity create(World world, LivingEntity owner, float damage) {
        IcicleProjectileEntity icicle = new IcicleProjectileEntity(KevsLibrary.ICICLE_PROJECTILE, world);
        icicle.setOwner(owner);
        icicle.setDamage(damage);
        icicle.setSilent(true);
        icicle.setCritical(false);
        return icicle;
    }

    public void setIcicleDamage(float damage) {
        this.icicleDamage = damage;
        this.setDamage(damage);
    }

    @Override
    protected void onEntityHit(EntityHitResult result) {
        if (!(getOwner() instanceof LivingEntity attacker)) return;
        if (!(result.getEntity() instanceof LivingEntity target)) return;

        DamageSource source = this.getDamageSources().create(KevsDamageTypes.ICICLE, attacker);
        boolean hit = target.damage(source, icicleDamage);

        if (hit && getWorld() instanceof ServerWorld world) {
            world.spawnParticles(ParticleTypes.ITEM_SNOWBALL, target.getX(), target.getY() + 1.0, target.getZ(),
                    10, 0.2, 0.3, 0.2, 0.01);
            world.playSound(null, target.getBlockPos(), SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 0.6f, 1.8f);
        }

        discard();
    }

    @Override
    protected void onBlockHit(BlockHitResult hit) {
        super.onBlockHit(hit);
        if (getWorld() instanceof ServerWorld world) {
            BlockPos pos = hit.getBlockPos();
            BlockState block = world.getBlockState(pos);

            // 💥 Puff on block impact
            world.spawnParticles(ParticleTypes.CLOUD, getX(), getY(), getZ(), 6, 0.1, 0.1, 0.1, 0.01);
            world.playSound(null, pos, SoundEvents.BLOCK_SNOW_BREAK, SoundCategory.PLAYERS, 0.6f, 1.2f);
        }

        discard(); // 💀 Remove on contact with block
    }

    @Override
    protected ItemStack getDefaultItemStack() {
        return new ItemStack(Items.SNOWBALL);
    }

    @Override
    public ItemStack getStack() {
        return new ItemStack(Items.SNOWBALL);
    }

    public boolean canPickup() {
        return false;
    }

    @Override
    public void onPlayerCollision(PlayerEntity player) {
        // Do nothing — prevents pickup
    }

    @Override
    public void tick() {
        super.tick();
        if (this.getWorld() instanceof ServerWorld world) {
            world.spawnParticles(ParticleTypes.END_ROD, this.getX(), this.getY(), this.getZ(), 1, 0, 0, 0, 0.01);
        }
    }
}
