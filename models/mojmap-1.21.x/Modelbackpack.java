// Made with Blockbench 5.0.7
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports

public class Modelbackpack<T extends Entity> extends EntityModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in
	// the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
			new ResourceLocation("modid", "backpack"), "main");
	private final ModelPart backpack;

	public Modelbackpack(ModelPart root) {
		this.backpack = root.getChild("backpack");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition backpack = partdefinition.addOrReplaceChild("backpack", CubeListBuilder.create(),
				PartPose.offset(0.0F, 0.0F, 6.0F));

		PartDefinition cube_r1 = backpack.addOrReplaceChild("cube_r1",
				CubeListBuilder.create().texOffs(26, 24)
						.addBox(-3.0F, -6.5F, 2.5F, 6.0F, 2.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(26, 19)
						.addBox(-4.0F, 3.5F, -3.0F, 8.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)).texOffs(0, 12)
						.addBox(-6.0F, 3.5F, -1.0F, 12.0F, 3.0F, 4.0F, new CubeDeformation(0.0F)).texOffs(0, 19)
						.addBox(-4.0F, -4.5F, -1.0F, 8.0F, 6.0F, 5.0F, new CubeDeformation(0.0F)).texOffs(0, 0)
						.addBox(-5.0F, 1.5F, -2.0F, 10.0F, 6.0F, 6.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, 4.5F, 6.0F, 0.0F, 3.1416F, 0.0F));

		return LayerDefinition.create(meshdefinition, 64, 64);
	}

	@Override
	public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
			float headPitch) {

	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay,
			float red, float green, float blue, float alpha) {
		backpack.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}
}