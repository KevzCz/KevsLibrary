package net.pixeldreamstudios.kevslibrary;

import net.minecraft.entity.damage.DamageType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

public class KevsDamageTypes {
    public static final RegistryKey<DamageType> MULTISTRIKE =
            RegistryKey.of(RegistryKeys.DAMAGE_TYPE, Identifier.of(KevsLibrary.MOD_ID, "multistrike"));
    public static final RegistryKey<DamageType> MULTISTRIKE_RANGED =
            RegistryKey.of(RegistryKeys.DAMAGE_TYPE, Identifier.of(KevsLibrary.MOD_ID, "multistrike_ranged"));
    public static final RegistryKey<DamageType> ICICLE =
            RegistryKey.of(RegistryKeys.DAMAGE_TYPE, Identifier.of(KevsLibrary.MOD_ID, "icicle"));
}
