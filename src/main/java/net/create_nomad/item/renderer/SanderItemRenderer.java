package net.create_nomad.item.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.equipment.sandPaper.SandPaperItemComponent;
import com.simibubi.create.content.equipment.sandPaper.SandPaperItemRenderer;
import com.simibubi.create.foundation.item.render.CustomRenderedItemModel;
import com.simibubi.create.foundation.item.render.PartialItemModelRenderer;

import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class SanderItemRenderer extends SandPaperItemRenderer {
	private static final ModelResourceLocation ACCELERATOR_MODEL = ModelResourceLocation.inventory(ResourceLocation.fromNamespaceAndPath(net.create_nomad.CreateNomadMod.MODID, "sander_accelerator"));
	private static final float ACCELERATOR_ROTATION_SPEED = -12.0f;

	@Override
	protected void render(ItemStack stack, CustomRenderedItemModel model, PartialItemModelRenderer renderer, ItemDisplayContext transformType,
			PoseStack poseStack, MultiBufferSource buffer, int light, int overlay) {
		Minecraft minecraft = Minecraft.getInstance();
		ItemRenderer itemRenderer = minecraft.getItemRenderer();
		LocalPlayer player = minecraft.player;
		float partialTicks = AnimationTickHolder.getPartialTicks();
		boolean leftHand = transformType == ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
		boolean firstPerson = leftHand || transformType == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;
		boolean jeiPreview = stack.has(AllDataComponents.SAND_PAPER_JEI);

		poseStack.pushPose();

		if (stack.has(AllDataComponents.SAND_PAPER_POLISHING)) {
			poseStack.pushPose();
			if (transformType == ItemDisplayContext.GUI) {
				poseStack.translate(0.0F, 0.2F, 1.0F);
				poseStack.scale(0.75F, 0.75F, 0.75F);
			} else {
				int handDirection = leftHand ? -1 : 1;
				poseStack.mulPose(Axis.YP.rotationDegrees(handDirection * 40.0F));
			}

			int useDuration = player != null ? stack.getUseDuration(player) : 32;
			float useTicks = (jeiPreview ? -AnimationTickHolder.getTicks() % useDuration : player != null ? player.getUseItemRemainingTicks() : useDuration)
				- partialTicks + 1.0F;
			if (useTicks / useDuration < 0.8F) {
				float offset = -Mth.abs(Mth.cos(useTicks / 4.0F * Mth.PI) * 0.1F);
				if (transformType == ItemDisplayContext.GUI) {
					poseStack.translate(offset, offset, 0.0F);
				} else {
					poseStack.translate(0.0F, offset, 0.0F);
				}
			}

			SandPaperItemComponent polishingComponent = stack.get(AllDataComponents.SAND_PAPER_POLISHING);
			ItemStack polishedStack = polishingComponent != null ? polishingComponent.item() : ItemStack.EMPTY;
			if (!polishedStack.isEmpty() && minecraft.level != null) {
				itemRenderer.renderStatic(polishedStack, ItemDisplayContext.GUI, light, overlay, poseStack, buffer, minecraft.level, 0);
			}
			poseStack.popPose();
		}

		if (firstPerson && player != null) {
			int remainingTicks = player.getUseItemRemainingTicks();
			if (remainingTicks > 0) {
				int handDirection = leftHand ? -1 : 1;
				poseStack.translate(handDirection * 0.5F, 0.0F, -0.25F);
				poseStack.mulPose(Axis.ZP.rotationDegrees(handDirection * 40.0F));
				poseStack.mulPose(Axis.XP.rotationDegrees(handDirection * 10.0F));
				poseStack.mulPose(Axis.YP.rotationDegrees(handDirection * 90.0F));
			}
		}

		BakedModel originalModel = model.getOriginalModel();
		renderer.render(originalModel, light);

		BakedModel acceleratorModel = minecraft.getModelManager().getModel(ACCELERATOR_MODEL);
		float rotation = (AnimationTickHolder.getTicks() + partialTicks) * ACCELERATOR_ROTATION_SPEED;
		poseStack.pushPose();
		poseStack.translate(0.5F, 0.5F, 0.5F);
		poseStack.mulPose(Axis.XP.rotationDegrees(rotation));
		poseStack.translate(-0.5F, -0.5F, -0.5F);
		renderer.render(acceleratorModel, light);
		poseStack.popPose();

		poseStack.popPose();
	}
}
