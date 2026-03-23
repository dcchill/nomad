package net.create_nomad.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BackpackInventorySavedData extends SavedData {
	private static final String DATA_NAME = "gearbound_backpacks";
	private static final String ROOT_KEY = "Backpacks";

	private final Map<UUID, CompoundTag> backpacks = new HashMap<>();

	public static BackpackInventorySavedData get(ServerLevel level) {
		return level.getServer().overworld().getDataStorage().computeIfAbsent(BackpackInventorySavedData::load, BackpackInventorySavedData::new, DATA_NAME);
	}

	private static BackpackInventorySavedData load(CompoundTag tag) {
		BackpackInventorySavedData data = new BackpackInventorySavedData();
		CompoundTag root = tag.getCompound(ROOT_KEY);
		for (String key : root.getAllKeys()) {
			try {
				UUID id = UUID.fromString(key);
				data.backpacks.put(id, root.getCompound(key).copy());
			} catch (IllegalArgumentException ignored) {
			}
		}
		return data;
	}

	@Override
	public CompoundTag save(CompoundTag tag) {
		CompoundTag root = new CompoundTag();
		for (Map.Entry<UUID, CompoundTag> entry : backpacks.entrySet()) {
			root.put(entry.getKey().toString(), entry.getValue().copy());
		}
		tag.put(ROOT_KEY, root);
		return tag;
	}

	public CompoundTag getInventory(UUID id) {
		return backpacks.getOrDefault(id, new CompoundTag()).copy();
	}

	public void setInventory(UUID id, CompoundTag inventoryTag) {
		backpacks.put(id, inventoryTag.copy());
		setDirty();
	}
}
