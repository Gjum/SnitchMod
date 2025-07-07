package gjum.minecraft.civ.snitchmod.common;

import com.mojang.blaze3d.platform.InputConstants;
import gjum.minecraft.civ.snitchmod.common.model.Direction;
import gjum.minecraft.civ.snitchmod.common.model.JalistEntry;
import gjum.minecraft.civ.snitchmod.common.model.Snitch;
import gjum.minecraft.civ.snitchmod.common.model.SnitchAlert;
import gjum.minecraft.civ.snitchmod.common.model.SnitchCreatedChatParser;
import gjum.minecraft.civ.snitchmod.common.model.SnitchFieldPreview;
import gjum.minecraft.civ.snitchmod.common.model.SnitchRename;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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

	protected static final KeyMapping toggleSnitchGoneStatusKey = new KeyMapping(
		"key.snitchmod.toggleSnitchGoneStatusKey",
		InputConstants.Type.KEYSYM,
		GLFW.GLFW_KEY_DELETE,
		"category.snitchmod"
	);

	protected static final KeyMapping toggleSnitchFieldRenderKey = new KeyMapping(
		"key.snitchmod.toggleSnitchFieldRenderKey",
		InputConstants.Type.KEYSYM,
		GLFW.GLFW_KEY_T,
		"category.snitchmod"
	);

	private static SnitchMod INSTANCE;

	public boolean rangeOverlayVisible = false;
	public boolean placementHelperVisible = false;
	public Optional<SnitchFieldPreview> snitchFieldToPreview = Optional.empty();
	public Optional<Snitch> lastBrokenSnitch = Optional.empty();
	private Optional<SnitchesStore> store = Optional.empty();

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

	public Optional<SnitchesStore> getStore() {
		String server = getCurrentServer();
		if (store.isPresent() && !store.get().server.equals(server)) {
			store.get().close();
			store = Optional.empty();
		}
		if (store.isEmpty() && server != null) {
			store = Optional.of(new SnitchesStore(server));
		}
		return store;
	}

	public void handleConnectedToServer() {
		getStore();
	}

	public void handleDisconnectedFromServer() {
        store.ifPresent(SnitchesStore::close);
		store = Optional.empty();
	}

	public void handleTick() {
		while (openGuiKey.consumeClick()) {
			// TODO open gui, and rename keybind
			store.ifPresent(SnitchesStore::close);
			store = Optional.empty();
			getStore();
			logToChat(Component.literal("Reloaded the database"));
		}

		while (toggleSnitchGoneStatusKey.consumeClick()) {
			if (!getMod().rangeOverlayVisible) {
				break;
			}

			Optional<Snitch> optSnitch = getMod().streamNearbySnitches(mc.player.position(), 260)
				.filter(s -> !s.wasBroken())
				.limit(100)
				.filter(s -> Utils.playerIsLookingAtSnitch(mc.player, s))
				.findFirst();
			if (optSnitch.isEmpty()) {
				logToChat(Component.literal("Error: you must be looking at a snitch to change its status"));
				break;
			}

			Snitch snitch = optSnitch.get();
			String formattedSnitch = String.format(
				"\"%s\" on [%s] at %d %d %d",
				snitch.getName() != null ? snitch.getName() : "",
				snitch.getGroup(),
				snitch.getPos().getX(),
				snitch.getPos().getY(),
				snitch.getPos().getZ()
			);
			if (snitch.isGone()) {
				store.get().updateSnitchNoLongerGone(snitch.pos);
				logToChat(Component.literal(String.format("Marked snitch %s as alive", formattedSnitch)));
			} else {
				store.get().updateSnitchGone(snitch.pos);
				logToChat(Component.literal(String.format("Marked snitch %s as gone", formattedSnitch)));
			}
		}

		while (toggleOverlayKey.consumeClick()) {
			rangeOverlayVisible = !rangeOverlayVisible;
			logToChat(
				Component.literal("Range overlay " + (rangeOverlayVisible ? "visible" : "hidden"))
			);
		}

		while (togglePlacementKey.consumeClick()) {
			placementHelperVisible = !placementHelperVisible;

			if (placementHelperVisible) {
				snitchFieldToPreview = Optional.empty();
			}

			logToChat(
				Component.literal("Placement helper " + (placementHelperVisible ? "visible" : "hidden"))
			);
		}

		while (previewSnitchFieldKey.consumeClick()) {
			Optional<Snitch> optNearestSnitch = streamNearbySnitches(mc.player.position(), 2*23)
				.filter(Snitch::isAlive)
				.findFirst();
			if (optNearestSnitch.isEmpty()) {
				snitchFieldToPreview = Optional.empty();
				logToChat(Component.literal("No nearby alive snitches to base a field preview on"));
				break;
			}
			Snitch nearestSnitch = optNearestSnitch.get();

			if (placementHelperVisible) {
				placementHelperVisible = false;
			}

			SnitchFieldPreview newSnitchFieldToPreview = new SnitchFieldPreview(
				nearestSnitch, Direction.ofPlayer(mc.player));
			if (
				snitchFieldToPreview.isPresent()
				&& newSnitchFieldToPreview.equals(snitchFieldToPreview.get())
			) {
				logToChat(Component.literal("Turning off the snitch field preview"));
				snitchFieldToPreview = null;
				break;
			}

			snitchFieldToPreview = Optional.of(newSnitchFieldToPreview);
			logToChat(Component.literal("Showing a snitch field preview in the direction you're looking"));
		}
		// TODO if block pos changed -> if pos inside snitch range not in before -> send jainfo -> mark refreshed
	}

	/**
	 * Returns true when the packet should be dropped
	 */
	public boolean handleChat(Component message) {
		getStore();
		if (store.isEmpty()) return false;

		SnitchAlert snitchAlert = SnitchAlert.fromChat(message, store.get().server, getCurrentWorld());
		if (snitchAlert != null) {
			store.get().updateSnitchFromAlert(snitchAlert);
			return false;
		}

		SnitchRename snitchRename = SnitchRename.fromChat(message, store.get().server, getCurrentWorld(), getClientUuid());
		if (snitchRename != null) {
			store.get().updateSnitchFromRename(snitchRename);
			return false;
		}

		Snitch snitchCreated = SnitchCreatedChatParser.fromChat(message, store.get().server, getCurrentWorld(), getClientUuid());
		if (snitchCreated != null) {
			Snitch alreadyExistingSnitch = store.get().getSnitch(snitchCreated.pos);
			store.get().updateSnitchFromCreation(snitchCreated);
			if (
				alreadyExistingSnitch != null
				&& alreadyExistingSnitch.getName() != null
				&& !alreadyExistingSnitch.getName().equals("")
			) {
				mc.player.connection.sendCommand(
					String.format(
						"janameat %d %d %d %s",
						alreadyExistingSnitch.pos.getX(),
						alreadyExistingSnitch.pos.getY(),
						alreadyExistingSnitch.pos.getZ(),
						alreadyExistingSnitch.getName()
					)
				);
				logToChat(Component.literal(String.format("Named the replaced snitch \"%s\"", alreadyExistingSnitch.getName())));
			}

			if (
				snitchFieldToPreview.isPresent()
				&& snitchFieldToPreview.get().field().pos.equals(snitchCreated.pos)
			) {
				if (placementHelperVisible) {
					placementHelperVisible = false;
				}

				snitchFieldToPreview = Optional.of(new SnitchFieldPreview(
					snitchCreated, snitchFieldToPreview.get().direction()));

				logToChat(Component.literal("Showing an inferred snitch field preview"));
			}

			return false;
		}

		// TODO if chat is jainfo and can refresh group -> mark refreshed

		return false;
	}

	public void handleWindowItems(List<ItemStack> stacks) {
		getStore();
		if (store.isEmpty()) return;
		List<JalistEntry> jalistEntries = new ArrayList<JalistEntry>(stacks.size());
		for (int i = 0; i < stacks.size(); i++) {
			ItemStack stack = stacks.get(i);
			try {
				JalistEntry jalistEntry = JalistEntry.fromStack(stack, store.get().server);
				if (jalistEntry != null) {
					jalistEntries.add(jalistEntry);
				}
			} catch (Throwable e) {
				System.err.println("Failed parsing jalist stack " + i + " " + stack);
				e.printStackTrace();
				logToChat(Component.literal("Failed reading snitch " + i + " on JAList page"));
			}
		}
		store.get().updateSnitchesFromJalist(jalistEntries);
		if (jalistEntries.size() > 0) {
			logToChat(Component.literal("Found " + jalistEntries.size() + " snitches on JAList page"));
		}
	}

	public void handleRenderBlockOverlay(Matrix4f matrix) {
		Renderer.renderOverlays(matrix);
	}

	public Stream<Snitch> streamNearbySnitches(Vec3 playerPos, int distance) {
		getStore();
		if (store.isEmpty()) return Stream.empty();
		return store.get().getAllSnitches().stream()
			.filter(s -> s.getPos().getCenter().distanceTo(playerPos) < distance)
			.sorted(Comparator.comparing(s -> s.getPos().getCenter().distanceTo(playerPos)));
	}

	private void logToChat(Component msg) {
		mc.gui.getChat().addMessage(Component.literal("[SnitchMod] ").append(msg));
	}
}
