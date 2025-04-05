package net.pixeldreamstudios.kevslibrary.config;

import com.terraformersmc.modmenu.api.ModMenuApi;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class KevsLibraryModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return (Screen parent) -> {
            ConfigBuilder builder = ConfigBuilder.create()
                    .setParentScreen(parent)
                    .setTitle(Text.literal("Kev's Library Config"));

            ConfigEntryBuilder entryBuilder = builder.entryBuilder();
            ConfigCategory general = builder.getOrCreateCategory(Text.of("General"));

            general.addEntry(entryBuilder
                    .startIntField(Text.literal("X Offset"), KevsLibraryConfig.INSTANCE.xOffset)
                    .setDefaultValue(-61)
                    .setTooltip(Text.of("Horizontal position of the attribute panel icon"))
                    .setSaveConsumer(value -> KevsLibraryConfig.INSTANCE.xOffset = value)
                    .build());

            general.addEntry(entryBuilder
                    .startIntField(Text.literal("Y Offset"), KevsLibraryConfig.INSTANCE.yOffset)
                    .setDefaultValue(66)
                    .setTooltip(Text.of("Vertical position of the attribute panel icon"))
                    .setSaveConsumer(value -> KevsLibraryConfig.INSTANCE.yOffset = value)
                    .build());

            builder.setSavingRunnable(() -> {
                KevsLibraryConfig.save();
                KevsLibraryConfig.apply(); // <-- Apply after saving
            });

            return builder.build();
        };
    }
}
