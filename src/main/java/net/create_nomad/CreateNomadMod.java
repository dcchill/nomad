package net.create_nomad;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import net.create_nomad.init.CreateNomadModBlockEntities;
import net.create_nomad.init.CreateNomadModBlocks;
import net.create_nomad.init.CreateNomadModCuriosCompat;
import net.create_nomad.init.CreateNomadModCuriosRenderers;
import net.create_nomad.init.CreateNomadModItems;
import net.create_nomad.init.CreateNomadModMenus;
import net.create_nomad.init.CreateNomadModSounds;
import net.create_nomad.init.CreateNomadModTabs;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

@Mod(CreateNomadMod.MODID)
public class CreateNomadMod {
	public static final Logger LOGGER = LogManager.getLogger(CreateNomadMod.class);
	public static final String MODID = "create_nomad";
	private static final String NETWORK_PROTOCOL = "1";
	private static final SimpleChannel NETWORK = NetworkRegistry.newSimpleChannel(new ResourceLocation(MODID, "main"), () -> NETWORK_PROTOCOL, NETWORK_PROTOCOL::equals, NETWORK_PROTOCOL::equals);
	private static final AtomicInteger MESSAGE_ID = new AtomicInteger();
	private static final Collection<WorkEntry> WORK_QUEUE = new ConcurrentLinkedQueue<>();
	private static final TagKey<Item> CURIOS_TAG = TagKey.create(net.minecraft.core.registries.Registries.ITEM, new ResourceLocation("curios", "back"));

	public CreateNomadMod() {
		this(FMLJavaModLoadingContext.get().getModEventBus());
	}

	public CreateNomadMod(IEventBus modEventBus) {
		MinecraftForge.EVENT_BUS.register(this);
		if (ModList.get().isLoaded("curios")) {
			modEventBus.addListener(CreateNomadModCuriosCompat::registerCapabilities);
			modEventBus.addListener(CreateNomadModCuriosRenderers::registerRenderers);
		}
		CreateNomadModSounds.REGISTRY.register(modEventBus);
		CreateNomadModBlocks.REGISTRY.register(modEventBus);
		CreateNomadModBlockEntities.REGISTRY.register(modEventBus);
		CreateNomadModItems.REGISTRY.register(modEventBus);
		CreateNomadModTabs.REGISTRY.register(modEventBus);
		CreateNomadModMenus.REGISTRY.register(modEventBus);
	}

	public static <T> void addNetworkMessage(Class<T> type, BiConsumer<T, net.minecraft.network.FriendlyByteBuf> encoder,
			Function<net.minecraft.network.FriendlyByteBuf, T> decoder,
			BiConsumer<T, Supplier<net.minecraftforge.network.NetworkEvent.Context>> handler) {
		NETWORK.registerMessage(MESSAGE_ID.getAndIncrement(), type, encoder, decoder, handler);
	}

	public static void sendToServer(Object message) {
		NETWORK.sendToServer(message);
	}

	public static void sendToPlayer(net.minecraft.server.level.ServerPlayer player, Object message) {
		NETWORK.send(PacketDistributor.PLAYER.with(() -> player), message);
	}

	private record WorkEntry(Runnable action, int ticksRemaining) {
	}

	public static void queueServerWork(int tick, Runnable action) {
		WORK_QUEUE.add(new WorkEntry(action, tick));
	}

	@SubscribeEvent
	public void tick(TickEvent.ServerTickEvent event) {
		if (event.phase != TickEvent.Phase.END) {
			return;
		}
		List<WorkEntry> actions = new ArrayList<>();
		for (WorkEntry work : WORK_QUEUE) {
			WorkEntry updated = new WorkEntry(work.action(), work.ticksRemaining() - 1);
			WORK_QUEUE.remove(work);
			if (updated.ticksRemaining() <= 0) {
				actions.add(updated);
			} else {
				WORK_QUEUE.add(updated);
			}
		}
		actions.forEach(entry -> entry.action().run());
	}

	public static class CuriosApiHelper {
		public static IItemHandler getCuriosInventory(Player player) {
			if (ModList.get().isLoaded("curios")) {
				return top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player).map(handler -> (IItemHandler) handler).orElse(null);
			}
			return null;
		}

		public static boolean isCurioItem(ItemStack itemstack) {
			return itemstack.is(CURIOS_TAG);
		}
	}
}
