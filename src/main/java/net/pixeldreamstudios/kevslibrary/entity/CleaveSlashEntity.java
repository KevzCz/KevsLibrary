package net.pixeldreamstudios.kevslibrary.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;

public class CleaveSlashEntity extends Entity {
    private int ticks = 0;
    private int lifespan = 8;
    private LivingEntity owner;

    public CleaveSlashEntity(EntityType<? extends CleaveSlashEntity> type, World world) {
        super(type, world);
        this.setNoGravity(true);
    }

    public void setOwner(LivingEntity owner) {
        this.owner = owner;
    }

    public LivingEntity getOwner() {
        return this.owner;
    }

    public void setLifespan(int lifespan) {
        this.lifespan = lifespan;
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {}

    @Override
    public void tick() {
        super.tick();
        this.move(MovementType.SELF, this.getVelocity());
        if (++ticks > lifespan) discard();
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        this.lifespan = nbt.getInt("Lifespan");
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putInt("Lifespan", this.lifespan);
    }
}
