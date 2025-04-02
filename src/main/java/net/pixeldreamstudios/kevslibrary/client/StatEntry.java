package net.pixeldreamstudios.kevslibrary.client;

import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;

public record StatEntry(
        Text name,
        double base,
        double current,
        boolean percent,
        RegistryEntry<EntityAttribute> attribute // <-- Add this
) {
    public boolean isChanged() {
        return Math.abs(current - base) > 0.001;
    }
}
