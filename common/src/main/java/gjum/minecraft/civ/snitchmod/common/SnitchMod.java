package gjum.minecraft.civ.snitchmod.common;

import com.mamiyaotaru.voxelmap.VoxelMap;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import gjum.minecraft.civ.snitchmod.common.model.*;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.stream.Stream;

public abstract class SnitchMod {
	private final static Minecraft mc = Minecraft.getInstance();

	protected static final KeyMapping openGuiKey = new KeyMapping(
			"key.snitchmod.openGui",
			InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_L,
			"category.snitchmod"
	);

	protected static final KeyMapping toggleOverlayKey = new KeyMapping(
			"key.snitchmod.toggleOverlay",
			InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_O,
			"category.snitchmod"
	);

	protected static final KeyMapping togglePlacementKey = new KeyMapping(
			"key.snitchmod.togglePlacement",
			InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_P,
			"category.snitchmod"
	);

	protected static final KeyMapping previewSnitchFieldKey = new KeyMapping(
			"key.snitchmod.togglePreviewSnitchFieldKey",
			InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_N,
			"category.snitchmod"
	);

	private static SnitchMod INSTANCE;

	public boolean rangeOverlayVisible = false;
	public boolean placementHelperVisible = false;
	@Nullable
	public SnitchFieldPreview snitchFieldToPreview = null;
	@Nullable
	public Snitch lastBrokenSnitch = null;

	@Nullable
	private SnitchesStore store;

	public static SnitchMod getMod() {
		return INSTANCE;
	}

	public SnitchMod() {
		if (INSTANCE != null) throw new IllegalStateException("Constructor called twice");
		INSTANCE = this;
	}

	public @Nullable String getCurrentServer() {
		final ServerData currentServer = mc.getCurrentServer();
		if (currentServer == null) return null;
		return currentServer.ip;
	}

	public String getCurrentWorld() {
		if (mc.level == null) return null;
		String dimension = mc.level.dimension().location().getPath();
		// civ server world names as they occur in snitches are different from dimension names
		return switch (dimension) {
			case "overworld" -> "world";
			case "the_nether" -> "world_nether";
			case "the_end" -> "world_the_end";
			default -> dimension;
		};
	}

	public UUID getClientUuid() {
		if (mc.player == null) return null;
		return mc.player.getUUID();
	}

	public @Nullable SnitchesStore getStore() {
		String server = getCurrentServer();
		if (store != null && !store.server.equals(server)) {
			store.close();
			store = null;
		}
		if (store == null && server != null) {
			store = new SnitchesStore(server);
		}
		return store;
	}

	public void handleConnectedToServer() {
		getStore();
	}

	public void handleDisconnectedFromServer() {
		if (store != null) store.close();
		store = null;
	}

	public void handleTick() {
		while (openGuiKey.consumeClick()) {
			// TODO open gui, and rename keybind
			store.close();
			store = null;
			getStore();
			logToChat(Component.literal("Reloaded the database"));
		}
		while (toggleOverlayKey.consumeClick()) {
			rangeOverlayVisible = !rangeOverlayVisible;
			logToChat(Component.literal(
					"Range overlay " + (rangeOverlayVisible ? "visible" : "hidden")));
		}
		while (togglePlacementKey.consumeClick()) {
			placementHelperVisible = !placementHelperVisible;

			if (placementHelperVisible) {
				snitchFieldToPreview = null;
			}

			logToChat(Component.literal(
					"Placement helper " + (placementHelperVisible ? "visible" : "hidden")));
		}
		while (previewSnitchFieldKey.consumeClick()) {
			Optional<Snitch> optNearestSnitch = streamNearbySnitches(mc.player.position(), 2*23)
				.filter(Snitch::isAlive)
				.findFirst();
			if (optNearestSnitch.isEmpty()) {
				snitchFieldToPreview = null;
				logToChat(Component.literal("No nearby snitches to base a field preview on"));
				break;
			}
			Snitch nearestSnitch = optNearestSnitch.get();

			if (placementHelperVisible) {
				placementHelperVisible = false;
			}

			SnitchFieldPreview newSnitchFieldToPreview = new SnitchFieldPreview(
				nearestSnitch, Direction.ofPlayer(mc.player));
			if (
				snitchFieldToPreview != null
				&& newSnitchFieldToPreview.equals(snitchFieldToPreview)
			) {
				logToChat(Component.literal("Turning off the snitch field preview"));
				snitchFieldToPreview = null;
				break;
			}

			snitchFieldToPreview = newSnitchFieldToPreview;
			logToChat(Component.literal("Showing a snitch field preview"));
		}
		// TODO if block pos changed -> if pos inside snitch range not in before -> send jainfo -> mark refreshed
	}

	/**
	 * Returns true when the packet should be dropped
	 */
	public boolean handleChat(Component message) {
		getStore();
		if (store == null) return false;

		SnitchAlert snitchAlert = SnitchAlert.fromChat(message, store.server, getCurrentWorld());
		if (snitchAlert != null) {
			store.updateSnitchFromAlert(snitchAlert);
			return false;
		}

		SnitchRename snitchRename = SnitchRename.fromChat(message, store.server, getCurrentWorld(), getClientUuid());
		if (snitchRename != null) {
			store.updateSnitchFromRename(snitchRename);
			return false;
		}

		Snitch snitchCreated = SnitchCreatedChatParser.fromChat(message, store.server, getCurrentWorld(), getClientUuid());
		if (snitchCreated != null) {
			store.updateSnitchFromCreation(snitchCreated);

			if (
				snitchFieldToPreview != null
				&& snitchFieldToPreview.field().pos.equals(snitchCreated.pos)
			) {
				if (placementHelperVisible) {
					placementHelperVisible = false;
				}

				snitchFieldToPreview = new SnitchFieldPreview(
					snitchCreated, snitchFieldToPreview.direction());

				logToChat(Component.literal("Showing an inferred snitch field preview"));
			}

			if (
				lastBrokenSnitch != null
				&& lastBrokenSnitch.getName() != null
				&& !lastBrokenSnitch.getName().equals("")
				&& lastBrokenSnitch.pos.equals(snitchCreated.pos)
			) {
				mc.getConnection().sendCommand(
					String.format(
						"/janameat %d %d %d %s",
						lastBrokenSnitch.pos.getX(),
						lastBrokenSnitch.pos.getY(),
						lastBrokenSnitch.pos.getZ(),
						lastBrokenSnitch.getName()));
				logToChat(Component.literal("Named the replaced snitch"));
			}

			return false;
		}

		long now = System.currentTimeMillis();
		if (lastBrokenBlockTs > now - 1000 && lastBrokenBlockPos != null) {
			SnitchBroken snitchBroken = SnitchBroken.fromChat(message, lastBrokenBlockPos, store.server, getCurrentWorld());
			if (snitchBroken != null) {
				store.updateSnitchBroken(snitchBroken);

				Snitch snitch = store.getSnitch(getCurrentWorld(), lastBrokenBlockPos);
				if (snitch != null) {
					lastBrokenSnitch = snitch;
				}

				return false;
			}
		}

		// TODO if chat is jainfo and can refresh group -> mark refreshed

		return false;
	}

	private BlockPos lastBrokenBlockPos;
	private long lastBrokenBlockTs;

	public void handleBlockUpdate(BlockPos pos, BlockState blockState) {
		if (blockState == Blocks.AIR.defaultBlockState()) {
			lastBrokenBlockPos = pos;
			lastBrokenBlockTs = System.currentTimeMillis();
		} else if (store != null
				// Prevent interaction between this and placing new snitches.
				&& !blockState.is(Blocks.JUKEBOX)
				&& !blockState.is(Blocks.NOTE_BLOCK)
		) {
			WorldPos worldPos = new WorldPos(store.server, getCurrentWorld(), pos);

			store.updateSnitchGone(worldPos);

			Snitch snitch = store.getSnitch(worldPos);
			if (snitch != null) {
				lastBrokenSnitch = snitch;
			}
		}
	}

	public void handleWindowItems(List<ItemStack> stacks) {
		getStore();
		if (store == null) return;
		List<JalistEntry> jalistEntries = new ArrayList<JalistEntry>(stacks.size());
		for (int i = 0; i < stacks.size(); i++) {
			ItemStack stack = stacks.get(i);
			try {
				JalistEntry jalistEntry = JalistEntry.fromStack(stack, store.server);
				if (jalistEntry != null) {
					jalistEntries.add(jalistEntry);
				}
			} catch (Throwable e) {
				System.err.println("Failed parsing jalist stack " + i + " " + stack);
				e.printStackTrace();
				logToChat(Component.literal(
						"Failed reading snitch " + i + " on JAList page"));
			}
		}
		store.updateSnitchesFromJalist(jalistEntries);
		if (jalistEntries.size() > 0) {
			logToChat(Component.literal(
					"Found " + jalistEntries.size() + " snitches on JAList page"));
		}
	}

	public void handleRenderBlockOverlay(PoseStack matrices) {
		Renderer.renderOverlays(matrices);
	}

	public Stream<Snitch> streamNearbySnitches(Vec3 playerPos, int distance) {
		getStore();
		if (store == null) return Stream.empty();
		return store.getAllSnitches().stream()
				.filter(s -> s.getPos().getCenter().distanceTo(playerPos) < distance)
				.sorted(Comparator.comparing(s -> s.getPos().getCenter().distanceTo(playerPos)));
	}

	private void logToChat(Component msg) {
		mc.gui.getChat().addMessage(Component.literal("[SnitchMod] ").append(msg));
	}
}
