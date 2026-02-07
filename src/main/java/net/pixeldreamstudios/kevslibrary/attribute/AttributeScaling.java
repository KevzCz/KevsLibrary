package net.pixeldreamstudios.kevslibrary.attribute;

import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.registry.entry.RegistryEntry;

import java.util.ArrayList;
import java.util.List;

public class AttributeScaling {
    private final List<ScalingEntry> scalingAttributes;
    private final double baseRatio;

    private AttributeScaling(Builder builder) {
        this.scalingAttributes = builder.scalingAttributes;
        this.baseRatio = builder.baseRatio;
    }

    public double calculateScaling(AttributeContext context) {
        double total = 0.0;
        for (ScalingEntry entry : scalingAttributes) {
            double value = context.getAttributeValue(entry.attribute);
            total += (value - 100) * entry.ratio;
        }
        return total * baseRatio;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final List<ScalingEntry> scalingAttributes = new ArrayList<>();
        private double baseRatio = 1.0;

        public Builder addScaling(RegistryEntry<EntityAttribute> attribute, double ratio) {
            scalingAttributes.add(new ScalingEntry(attribute, ratio));
            return this;
        }

        public Builder baseRatio(double ratio) {
            this.baseRatio = ratio;
            return this;
        }

        public AttributeScaling build() {
            return new AttributeScaling(this);
        }
    }

    private static class ScalingEntry {
        final RegistryEntry<EntityAttribute> attribute;
        final double ratio;

        ScalingEntry(RegistryEntry<EntityAttribute> attribute, double ratio) {
            this.attribute = attribute;
            this.ratio = ratio;
        }
    }
}