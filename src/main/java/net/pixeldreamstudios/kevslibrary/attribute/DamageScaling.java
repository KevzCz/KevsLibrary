package net.pixeldreamstudios.kevslibrary.attribute;

import net.pixeldreamstudios.kevslibrary.KevsLibrary;

    public class DamageScaling {

    public static float applyGlobalDamageScaling(AttributeContext context, float baseDamage) {
        double damageValue = context.getAttributeValue(KevsLibrary.DAMAGE);
        double multiplier = (damageValue - 100.0) / 100.0 + 1.0;
        return baseDamage * (float) multiplier;
    }

    public static float applyScaling(AttributeContext context, AttributeScaling scaling, float baseDamage) {
        if (scaling == null) return baseDamage;
        double scalingBonus = scaling.calculateScaling(context);
        return baseDamage * (1.0f + (float) scalingBonus);
    }

    public static float applyBoth(AttributeContext context, AttributeScaling scaling, float baseDamage) {
        float withGlobal = applyGlobalDamageScaling(context, baseDamage);
        return applyScaling(context, scaling, withGlobal);
    }
}