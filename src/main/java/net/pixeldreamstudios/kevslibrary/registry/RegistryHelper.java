package net.pixeldreamstudios.kevslibrary.registry;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.registry.entry.RegistryEntry;
import net.pixeldreamstudios.kevslibrary.KevsLibrary;

public class RegistryHelper {
    public static RegistryEntry<EntityAttribute> registerAttribute(String name, EntityAttribute attribute) {
        return Registry.registerReference(
                Registries.ATTRIBUTE,
                Identifier.of(KevsLibrary.MOD_ID, name),
                attribute
        );
    }
}
