package net.create_nomad.client.renderer;

import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import net.create_nomad.client.model.Modelbackpack;

import java.util.Collections;
import java.util.Map;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

public class BrassBackpackCuriosRenderer implements ICurioRenderer {
	private final ResourceLocation texture;
	private final HumanoidModel<LivingEntity> humanoidModel;

	public BrassBackpackCuriosRenderer(ResourceLocation texture) {
		this.texture = texture;
		Modelbackpack model = new Modelbackpack(Minecraft.getInstance().getEntityModels().bakeLayer(Modelbackpack.LAYER_LOCATION));
		this.humanoidModel = new HumanoidModel<LivingEntity>(new ModelPart(Collections.emptyList(),
				Map.of("hat", new ModelPart(Collections.emptyList(), Collections.emptyMap()), "head", new ModelPart(Collections.emptyList(), Collections.emptyMap()), "body", model.backpack, "left_arm",
						new ModelPart(Collections.emptyList(), Collections.emptyMap()), "right_arm", new ModelPart(Collections.emptyList(), Collections.emptyMap()), "left_leg", new ModelPart(Collections.emptyList(), Collections.emptyMap()),
						"right_leg", new ModelPart(Collections.emptyList(), Collections.emptyMap()))));
	}

	@Override
	public <T extends LivingEntity, M extends EntityModel<T>> void render(ItemStack stack, SlotContext slotContext, PoseStack matrixStack, RenderLayerParent<T, M> renderLayerParent, MultiBufferSource renderTypeBuffer, int light,
			float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
		LivingEntity entity = slotContext.entity();
		ICurioRenderer.followHeadRotations(entity, this.humanoidModel.head);
		ICurioRenderer.followBodyRotations(entity, this.humanoidModel);
		this.humanoidModel.prepareMobModel(entity, limbSwing, limbSwingAmount, partialTicks);
		VertexConsumer vertexconsumer = ItemRenderer.getArmorFoilBuffer(renderTypeBuffer, RenderType.entityTranslucent(texture), stack.hasFoil());
		this.humanoidModel.renderToBuffer(matrixStack, vertexconsumer, light, OverlayTexture.NO_OVERLAY);
	}
}
