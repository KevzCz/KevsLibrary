package net.kevslibrary;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KevsLibrary implements ModInitializer {
    public static String MOD_ID = "kevslibrary";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Hello from " + MOD_ID + "!");
        LOGGER.info("Loading Resourcepack for " + MOD_ID);
        FabricLoader.getInstance().getModContainer(MOD_ID).ifPresent((modContainer -> ResourceManagerHelper.registerBuiltinResourcePack(
                new ResourceLocation(MOD_ID, "ModifierNames"), modContainer, ResourcePackActivationType.ALWAYS_ENABLED
        )));
        if(FabricLoader.getInstance().isModLoaded("kevstierifymodifiers")) {
            FabricLoader.getInstance().getModContainer(MOD_ID).ifPresent((modContainer -> ResourceManagerHelper.registerBuiltinResourcePack(
                    new ResourceLocation(MOD_ID, "TierifyM"), modContainer, ResourcePackActivationType.ALWAYS_ENABLED
            )));
        }
        if(FabricLoader.getInstance().isModLoaded("kevstieredzmodifiers")) {
            FabricLoader.getInstance().getModContainer(MOD_ID).ifPresent((modContainer -> ResourceManagerHelper.registerBuiltinResourcePack(
                    new ResourceLocation(MOD_ID, "TieredM"), modContainer, ResourcePackActivationType.ALWAYS_ENABLED
            )));
        }

    }

}