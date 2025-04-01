package net.pixeldreamstudios.kevslibrary.client;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.particle.EndRodParticle;
import net.minecraft.client.render.entity.ArrowEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.util.Identifier;
import net.minecraft.client.render.entity.FlyingItemEntityRenderer;

import net.pixeldreamstudios.kevslibrary.KevsLibrary;

public class KevsLibraryClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(
                KevsLibrary.MULTISTRIKE_ARROW,
                (EntityRendererFactory.Context context) -> new ArrowEntityRenderer(context)
        );
        EntityRendererRegistry.register(
                KevsLibrary.ICICLE_PROJECTILE,
                context -> new FlyingItemEntityRenderer<>(context)
        );


    }
}
