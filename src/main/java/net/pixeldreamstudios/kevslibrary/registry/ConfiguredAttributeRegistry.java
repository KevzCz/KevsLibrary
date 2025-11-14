package net.pixeldreamstudios.kevslibrary.registry;

import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.pixeldreamstudios.kevslibrary.KevsLibrary;
import net.pixeldreamstudios.kevslibrary.config.KevsLibraryConfig;

import java.util.*;

public class ConfiguredAttributeRegistry {
    private static final Map<String, RegistryEntry<EntityAttribute>> REGISTERED_ATTRIBUTES = new HashMap<>();

    public static RegistryEntry<EntityAttribute> registerAttribute(String name, EntityAttribute attribute) {
        KevsLibraryConfig config = KevsLibraryConfig.getInstance();


        if (!shouldRegisterAttribute(name, config)) {
            KevsLibrary.LOGGER.info("Skipping attribute registration: {} (disabled in config)", name);
            return null;
        }


        RegistryEntry<EntityAttribute> entry = Registry.registerReference(
                Registries.ATTRIBUTE,
                Identifier.of(KevsLibrary.MOD_ID, name),
                attribute
        );

        REGISTERED_ATTRIBUTES.put(name, entry);
        KevsLibrary.LOGGER.debug("Registered attribute: {}", name);
        return entry;
    }

    private static boolean shouldRegisterAttribute(String attributeName, KevsLibraryConfig config) {

        if (!config.attributes.getOrDefault(attributeName, false)) {
            return false;
        }


        String parentSystem = findParentSystem(attributeName);
        if (parentSystem == null) {
            return true;
        }

        if (!config.systems.getOrDefault(parentSystem, false)) {
            return false;
        }


        List<String> requiredAttributes = config.getSystemAttributes(parentSystem);
        if (requiredAttributes != null) {
            for (String siblingAttr : requiredAttributes) {
                if (!config.attributes.getOrDefault(siblingAttr, false)) {
                    return false;
                }
            }
        }

        return true;
    }

    private static String findParentSystem(String attributeName) {
        KevsLibraryConfig config = KevsLibraryConfig.getInstance();

        for (String systemName : config.systems.keySet()) {
            List<String> systemAttrs = config.getSystemAttributes(systemName);
            if (systemAttrs != null && systemAttrs.contains(attributeName)) {
                return systemName;
            }
        }

        return null;
    }

    public static boolean isAttributeRegistered(String name) {
        return REGISTERED_ATTRIBUTES.containsKey(name);
    }

    public static RegistryEntry<EntityAttribute> getAttribute(String name) {
        return REGISTERED_ATTRIBUTES.get(name);
    }
}