package net.create_nomad.item;

import java.util.function.Consumer;

import com.simibubi.create.content.equipment.sandPaper.SandPaperItem;
import com.simibubi.create.foundation.item.render.SimpleCustomRenderer;

import net.create_nomad.item.renderer.SanderItemRenderer;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

public class SanderItem extends SandPaperItem {
	public SanderItem() {
		super(new Item.Properties().stacksTo(1));
	}

	@Override
	public void initializeClient(Consumer<IClientItemExtensions> consumer) {
		consumer.accept(SimpleCustomRenderer.create(this, new SanderItemRenderer()));
	}
}
