package net.pixeldreamstudios.kevslibrary.handler;

import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.LivingEntity;
import net.pixeldreamstudios.kevslibrary.KevsLibrary;

public class ArmorPenetrationHandler {

    public static float applyArmorPenetration(LivingEntity attacker, LivingEntity target, float damage) {
        float armor = target.getArmor();

        double flatPen = 0.0;
        double percentPen = 0.0;

        EntityAttributeInstance flatAttr = attacker.getAttributeInstance(KevsLibrary.ARMOR_PENETRATION_FLAT);
        EntityAttributeInstance percentAttr = attacker.getAttributeInstance(KevsLibrary.ARMOR_PENETRATION);

        if (flatAttr != null) flatPen = flatAttr.getValue();
        if (percentAttr != null) percentPen = percentAttr.getValue();

        float effectiveArmor = Math.max(armor - (float) flatPen, 0);
        effectiveArmor *= (1.0f - (float) percentPen);

        // Fully ignore armor if effectiveArmor is basically 0
        if (effectiveArmor <= 0.1f) {
            return damage;
        }

        // Vanilla reduction formula: up to 80% reduction
        float reduction = Math.min(effectiveArmor * 0.04f, 0.8f);
        return damage * (1.0f - reduction);
    }
}

