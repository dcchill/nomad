package net.create_nomad.client.model;

import net.minecraft.world.entity.Entity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.EntityModel;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack;

// Made with Blockbench 5.0.7
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports
public class Modelharpoon<T extends Entity> extends EntityModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in
	// the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("create_nomad", "modelharpoon"), "main");
	public final ModelPart harpoon;

	public Modelharpoon(ModelPart root) {
		this.harpoon = root.getChild("harpoon");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();
		PartDefinition harpoon = partdefinition.addOrReplaceChild("harpoon", CubeListBuilder.create().texOffs(0, 32).addBox(-1.99F, -0.99F, -6.7662F, 2.0F, 2.0F, 28.0F, new CubeDeformation(0.01F)), PartPose.offset(0.0F, 24.0F, -4.6219F));
		PartDefinition cube_r1 = harpoon.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(64, 0).addBox(-3.0F, -2.0F, -2.0F, 2.0F, 2.0F, 6.0F, new CubeDeformation(-0.01F)),
				PartPose.offsetAndRotation(1.0F, 0.0624F, -4.1631F, 0.3927F, 0.0F, 0.0F));
		PartDefinition cube_r2 = harpoon.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(64, 0).addBox(-3.0F, -2.0F, -2.0F, 2.0F, 2.0F, 6.0F, new CubeDeformation(-0.01F)),
				PartPose.offsetAndRotation(1.0F, 1.7854F, -4.9285F, -0.3927F, 0.0F, 0.0F));
		return LayerDefinition.create(meshdefinition, 128, 128);
	}

	@Override
	public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int rgb) {
		harpoon.render(poseStack, vertexConsumer, packedLight, packedOverlay, rgb);
	}
}