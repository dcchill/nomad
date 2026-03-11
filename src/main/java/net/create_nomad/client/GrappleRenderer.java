package net.create_nomad.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

@EventBusSubscriber(modid = "gearbound", value = Dist.CLIENT)
public class GrappleRenderer {

    private static final ResourceLocation CHAIN_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("gearbound", "textures/entities/grapple_chain.png");

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {

        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES)
            return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;

        if (player == null || mc.level == null)
            return;

        if (!player.getPersistentData().getBoolean("gearbound_grappling"))
            return;

        double ax = player.getPersistentData().getDouble("gearbound_anchor_x");
        double ay = player.getPersistentData().getDouble("gearbound_anchor_y");
        double az = player.getPersistentData().getDouble("gearbound_anchor_z");

        Vec3 anchor = new Vec3(ax, ay, az);

        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);

        Vec3 cameraPos = mc.gameRenderer.getMainCamera().getPosition();

        Vec3 eye = player.getEyePosition(partialTick);
        Vec3 look = player.getViewVector(partialTick);
        Vec3 right = player.getViewVector(partialTick)
        .cross(player.getUpVector(partialTick))
        .normalize();

        Vec3 handPos = eye
                .add(look.scale(0.3))
                .add(right.scale(0.13))
                .add(0, -0.10, 0);

        Vec3 start = handPos.subtract(cameraPos);
        Vec3 endFull = anchor.subtract(cameraPos);

        Vec3 fullDirection = endFull.subtract(start);
        double fullLength = fullDirection.length();

        if (fullLength < 0.01)
            return;

        fullDirection = fullDirection.normalize();

        // 🔥 ANIMATED PROGRESS
        double progress = player.getPersistentData().getDouble("gearbound_grapple_progress");
        double length = fullLength * Math.min(progress, 1.0);


        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();

        MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();
        VertexConsumer builder =
                buffer.getBuffer(RenderType.entityTranslucentCull(CHAIN_TEXTURE));

        var pose = poseStack.last();

        float width = 0.09f;

        float segmentLength = 0.25f;
        int segments = Math.max(1, (int)(length / segmentLength));

        int light = LightTexture.FULL_BRIGHT;
        int overlay = 0;

        float nx = (float) fullDirection.x;
        float ny = (float) fullDirection.y;
        float nz = (float) fullDirection.z;

        Vec3 basePerp = fullDirection.cross(new Vec3(0, 1, 0));
        if (basePerp.lengthSqr() < 0.0001) {
            basePerp = fullDirection.cross(new Vec3(1, 0, 0));
        }
        basePerp = basePerp.normalize();

        Vec3 perp1 = basePerp.scale(width);
        Vec3 perp2 = fullDirection.cross(basePerp).normalize().scale(width);

        for (int i = 0; i < segments; i++) {

            Vec3 segStart = start.add(fullDirection.scale(i * segmentLength));
            double segEndDistance = Math.min((i + 1) * segmentLength, length);
            Vec3 segEnd = start.add(fullDirection.scale(segEndDistance));

            float v0 = (float) i;
            float v1 = (float) (i + 1);

            renderQuad(builder, pose, segStart, segEnd, perp1, light, overlay, nx, ny, nz, v0, v1);
            renderQuad(builder, pose, segStart, segEnd, perp2, light, overlay, nx, ny, nz, v0, v1);
        }

        poseStack.popPose();
    }

    private static void renderQuad(
            VertexConsumer builder,
            PoseStack.Pose pose,
            Vec3 start,
            Vec3 end,
            Vec3 perpendicular,
            int light, int overlay,
            float nx, float ny, float nz,
            float v0, float v1
    ) {

        Vec3 p1 = start.add(perpendicular);
        Vec3 p2 = start.subtract(perpendicular);
        Vec3 p3 = end.subtract(perpendicular);
        Vec3 p4 = end.add(perpendicular);

        builder.addVertex(pose.pose(), (float)p1.x, (float)p1.y, (float)p1.z)
                .setColor(255,255,255,255)
                 .setUv(0f, v0)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(nx, ny, nz);

        builder.addVertex(pose.pose(), (float)p2.x, (float)p2.y, (float)p2.z)
                .setColor(255,255,255,255)
                 .setUv(1f, v0)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(nx, ny, nz);

        builder.addVertex(pose.pose(), (float)p3.x, (float)p3.y, (float)p3.z)
                .setColor(255,255,255,255)
                 .setUv(1f, v1)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(nx, ny, nz);

        builder.addVertex(pose.pose(), (float)p4.x, (float)p4.y, (float)p4.z)
                .setColor(255,255,255,255)
                 .setUv(0f, v1)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(nx, ny, nz);
    }
}
