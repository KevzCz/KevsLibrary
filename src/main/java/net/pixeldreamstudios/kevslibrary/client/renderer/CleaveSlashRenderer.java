package net.pixeldreamstudios.kevslibrary.client.renderer;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.pixeldreamstudios.kevslibrary.entity.CleaveSlashEntity;
import org.joml.Matrix4f;
import org.joml.Vector3f;

@Environment(EnvType.CLIENT)
public class CleaveSlashRenderer extends EntityRenderer<CleaveSlashEntity> {
    private static final Identifier TEXTURE = Identifier.of("kevslibrary", "textures/entity/slash.png");

    public CleaveSlashRenderer(EntityRendererFactory.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(CleaveSlashEntity entity, float yaw, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light) {
        RenderLayer layer = RenderLayer.getEntityTranslucent(TEXTURE);
        VertexConsumer vertex = vertexConsumers.getBuffer(layer);

        float width = 5.0f;
        float height = 1.5f;
        float thickness = 0.1f;
        float yOffset = height / 2.0f;

        int overlay = 0;
        int r = 255, g = 255, b = 255, a = 255;

        float z = thickness;

        float[] xRotations = {0f, 45f, -45f};

        for (float pitch : xRotations) {
            matrices.push();

            matrices.translate(0.0, yOffset, 0.0);
            matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(-entity.getYaw()));
            matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_X.rotationDegrees(90 + pitch));
            matrices.scale(width, height, 1.0f);

            Matrix4f mat = matrices.peek().getPositionMatrix();

            vertex.vertex(mat, -0.5f, -0.5f, +z).color(r, g, b, a).texture(0, 1).overlay(overlay).light(light).normal(0, 0, -1);
            vertex.vertex(mat,  0.5f, -0.5f, +z).color(r, g, b, a).texture(1, 1).overlay(overlay).light(light).normal(0, 0, -1);
            vertex.vertex(mat,  0.5f,  0.5f, +z).color(r, g, b, a).texture(1, 0).overlay(overlay).light(light).normal(0, 0, -1);
            vertex.vertex(mat, -0.5f,  0.5f, +z).color(r, g, b, a).texture(0, 0).overlay(overlay).light(light).normal(0, 0, -1);

            vertex.vertex(mat,  0.5f, -0.5f, -z).color(r, g, b, a).texture(0, 1).overlay(overlay).light(light).normal(0, 0, 1);
            vertex.vertex(mat, -0.5f, -0.5f, -z).color(r, g, b, a).texture(1, 1).overlay(overlay).light(light).normal(0, 0, 1);
            vertex.vertex(mat, -0.5f,  0.5f, -z).color(r, g, b, a).texture(1, 0).overlay(overlay).light(light).normal(0, 0, 1);
            vertex.vertex(mat,  0.5f,  0.5f, -z).color(r, g, b, a).texture(0, 0).overlay(overlay).light(light).normal(0, 0, 1);

            matrices.pop();
        }
    }







    @Override
    public Identifier getTexture(CleaveSlashEntity entity) {
        return TEXTURE;
    }
}

