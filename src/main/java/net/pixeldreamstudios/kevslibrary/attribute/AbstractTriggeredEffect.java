package net.pixeldreamstudios.kevslibrary.attribute;

public abstract class AbstractTriggeredEffect implements TriggeredEffect {
    protected final ChanceAttribute chanceAttribute;
    protected final AttributeScaling scaling;

    protected AbstractTriggeredEffect(ChanceAttribute chanceAttribute, AttributeScaling scaling) {
        this.chanceAttribute = chanceAttribute;
        this.scaling = scaling;
    }

    @Override
    public ChanceAttribute getChanceAttribute() {
        return chanceAttribute;
    }

    @Override
    public AttributeScaling getScaling() {
        return scaling;
    }

    @Override
    public void triggerOverload(EffectContext context) {
        trigger(context);
    }
}