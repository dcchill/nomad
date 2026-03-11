package net.create_nomad.compat.ponder;

import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.catnip.math.Pointing;
import net.minecraft.core.Direction;

public class GearboundPonderScenes {
	public static void backpackAutoHotbar(SceneBuilder scene, SceneBuildingUtil util) {
		scene.title("backpack_auto_hotbar", "Backpack Auto Hotbar Filling");
		scene.configureBasePlate(0, 0, 5);
		scene.showBasePlate();

		scene.world().showSection(util.select().everywhere(), Direction.UP);
		scene.idle(20);

		scene.addKeyframe();
		scene.overlay().showText(80)
			.text("When an item stack in your hotbar runs low, the backpack refills it automatically.")
			.pointAt(util.vector().centerOf(util.grid().at(2, 1, 2)))
			.placeNearTarget();
		scene.idle(90);

		scene.addKeyframe();
		scene.overlay().showControls(util.vector().centerOf(util.grid().at(2, 1, 2)), Pointing.RIGHT, 40)
			.rightClick();
		scene.idle(10);

		scene.overlay().showText(80)
			.text("You can bind the backpack to a hotbar slot to choose which stack it should maintain.")
			.pointAt(util.vector().centerOf(util.grid().at(2, 1, 2)))
			.placeNearTarget();
		scene.idle(90);

		scene.addKeyframe();
		scene.overlay().showText(80)
			.text("Keep matching stacks inside the backpack and it will keep your hotbar stocked while you play.")
			.pointAt(util.vector().centerOf(util.grid().at(2, 1, 2)))
			.placeNearTarget();
		scene.idle(100);
	}

	public static void backpackColorVariants(SceneBuilder scene, SceneBuildingUtil util) {
		scene.title("backpack_auto_hotbar_pg2", "Dyeing Backpack Variants");
		scene.configureBasePlate(0, 0, 5);
		scene.showBasePlate();

		scene.world().showSection(util.select().everywhere(), Direction.UP);
		scene.idle(20);

		scene.addKeyframe();
		scene.overlay().showText(100)
			.text("Backpacks can be dyed into many colors to match your style or loadout.")
			.pointAt(util.vector().centerOf(util.grid().at(2, 1, 2)))
			.placeNearTarget();
		scene.idle(110);

		scene.addKeyframe();
		scene.overlay().showText(80)
			.text("Use any dye with a backpack in crafting to recolor it.")
			.pointAt(util.vector().centerOf(util.grid().at(2, 1, 2)))
			.placeNearTarget();
		scene.idle(90);
	}
}
