package gjum.minecraft.civ.snitchmod.common;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import gjum.minecraft.civ.snitchmod.common.model.*;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.Comparator;
import java.util.stream.Stream;

import static gjum.minecraft.civ.snitchmod.common.JalistStackParser.getJalistEntryFromStack;
import static gjum.minecraft.civ.snitchmod.common.SnitchAlertParser.getSnitchAlertFromChat;

public abstract class SnitchMod {
	private final static Minecraft mc = Minecraft.getInstance();

	private static final KeyMapping openGuiKey = new KeyMapping(
			"key.snitchmod.openGui",
			InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_L,
			"category.snitchmod"
	);

	private static final KeyMapping toggleOverlayKey = new KeyMapping(
			"key.snitchmod.toggleOverlay",
			InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_O,
			"category.snitchmod"
	);

	private static final KeyMapping togglePlacementKey = new KeyMapping(
			"key.snitchmod.togglePlacement",
			InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_P,
			"category.snitchmod"
	);

	private static SnitchMod INSTANCE;

	public boolean rangeOverlayVisible = false;
	public boolean placementHelperVisible = false;

	@Nullable
	private SnitchesStore store;

	public static SnitchMod getMod() {
		return INSTANCE;
	}

	public SnitchMod() {
		if (INSTANCE != null) throw new IllegalStateException("Constructor called twice");
		INSTANCE = this;
	}

	public void init() {
		registerKeyBinding(openGuiKey);
		registerKeyBinding(toggleOverlayKey);
		registerKeyBinding(togglePlacementKey);
	}

	public abstract void registerKeyBinding(KeyMapping mapping);

	public void handleTick() {
		while (openGuiKey.consumeClick()) {
			// TODO open gui
		}
		while (toggleOverlayKey.consumeClick()) {
			rangeOverlayVisible = !rangeOverlayVisible;
			mc.gui.getChat().addMessage(new TextComponent(
					"Range overlay " + (rangeOverlayVisible ? "visible" : "off")));
		}
		while (togglePlacementKey.consumeClick()) {
			placementHelperVisible = !placementHelperVisible;
			mc.gui.getChat().addMessage(new TextComponent(
					"Placement helper " + (placementHelperVisible ? "visible" : "off")));
		}
		// TODO if block pos changed -> if pos inside snitch range not in before -> send jainfo -> mark refreshed
	}

	public void handleConnectedToServer() {
		if (store != null) store.close();
		store = null;
		final ServerData currentServer = mc.getCurrentServer();
		if (currentServer == null) return;
		String server = currentServer.ip;
		store = new SnitchesStore(server);
	}

	public void handleDisconnectedFromServer() {
		if (store != null) store.close();
		store = null;
	}

	/**
	 * Returns true when the packet should be dropped
	 */
	public boolean handleChat(Component message) {
		SnitchAlert snitchAlert = getSnitchAlertFromChat(message, getCurrentServer());
		if (snitchAlert != null) handleSnitchAlert(snitchAlert);
		// TODO snitch created message -> remember for placement helper
		// TODO any broken snitch messages (own, hostile) -> delete snitch
		// TODO if chat is jainfo and can refresh group -> mark refreshed
		return false;
	}

	public void handleSetSlot(ItemStack stack) {
		JalistEntry jalistEntry = getJalistEntryFromStack(
				stack,
				getCurrentServer(),
				getSnitchCullDurationForServer(getCurrentServer()));
		if (jalistEntry != null) handleJalistEntry(jalistEntry);
	}

	public void handleRenderBlockOverlay(PoseStack matrices, float partialTicks) {
		Renderer.renderOverlays(matrices, partialTicks);
	}

	public void handleJalistEntry(JalistEntry jalistEntry) {
		if (store == null) return;
		store.updateSnitchFromJalist(jalistEntry);
	}

	public void handleSnitchAlert(SnitchAlert snitchAlert) {
		if (store == null) return;
		store.updateSnitchFromAlert(snitchAlert);
	}

	public String getCurrentServer() {
		if (store == null) return null;
		return store.server;
	}

	public Stream<Snitch> streamNearbySnitches(BlockPos playerPos, int distance) {
		if (store == null) return Stream.empty();
		AABB aabb = new AABB(playerPos).inflate(distance);
		return store.getAllSnitches().stream()
				.filter(s -> aabb.contains(s.getX(), s.getY(), s.getZ()))
				.sorted(Comparator.comparing(playerPos::distSqr));
	}

	/**
	 * milliseconds
	 */
	public long getSnitchCullDurationForServer(String server) {
		return 4L * 7L * 24L * 60L * 60L * 1000L; // 4 weeks (CivClassic config) TODO other servers CullDuration
	}
}
