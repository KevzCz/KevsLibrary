package net.pixeldreamstudios.kevslibrary.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.world.World;
import net.pixeldreamstudios.kevslibrary.KevsDamageTypes;
import net.pixeldreamstudios.kevslibrary.handler.OnHitEffectHandler;

public class MultistrikeArrowEntity extends ArrowEntity {

    private boolean multistrikeActive = false;

    public MultistrikeArrowEntity(EntityType<? extends ArrowEntity> type, World world) {
        super(type, world);
    }

    public MultistrikeArrowEntity(World world, LivingEntity owner) {
        super(world, owner, new ItemStack(Items.ARROW), new ItemStack(Items.BOW));
    }

    public boolean isMultistrikeActive() {
        return multistrikeActive;
    }

    public void setMultistrikeActive(boolean active) {
        this.multistrikeActive = active;
    }

    @Override
    public ItemStack getDefaultItemStack() {
        return new ItemStack(Items.ARROW);
    }

    @Override
    protected void onEntityHit(EntityHitResult result) {
        if (!(result.getEntity() instanceof LivingEntity target)) return;
        if (!(this.getOwner() instanceof LivingEntity attacker)) return;

        DamageSource source = this.getDamageSources().create(KevsDamageTypes.MULTISTRIKE_RANGED, this, attacker);
        float amount = (float) this.getDamage();

        boolean damaged = target.damage(source, amount);

        if (damaged && this.getWorld() instanceof ServerWorld serverWorld) {
            OnHitEffectHandler.withMultistrikeContext(() -> {
                OnHitEffectHandler.triggerAll(attacker, target, amount);
            });

            serverWorld.spawnParticles(
                    ParticleTypes.CRIT,
                    target.getX(), target.getY() + 1.0, target.getZ(),
                    10, 0.3, 0.5, 0.3, 0.01
            );
        }

        this.discard();
    }
}
