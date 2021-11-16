package gjum.minecraft.civ.snitchmod.common;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import gjum.minecraft.civ.snitchmod.common.model.*;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.stream.Collectors;

import static gjum.minecraft.civ.snitchmod.common.JalistStackParser.getSnitchFromStack;
import static gjum.minecraft.civ.snitchmod.common.SnitchAlertParser.getSnitchAlertFromChat;

public abstract class SnitchMod {
	private static SnitchMod INSTANCE;

	private static final KeyMapping toggleKey = new KeyMapping(
			"Toggle snitch range overlay",
			InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_O,
			"Snitch"
	);

	public boolean rangeOverlayVisible = true;

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
		registerKeyBinding(toggleKey);
	}

	public abstract void registerKeyBinding(KeyMapping mapping);

	public void handleTick() {
		while (toggleKey.consumeClick()) {
			rangeOverlayVisible = !rangeOverlayVisible;
		}
		// TODO if block pos changed -> if pos inside snitch range not in before -> send jainfo -> mark refreshed
	}

	public void handleConnectedToServer() {
		if (store != null) store.close();
		store = null;
		final ServerData currentServer = Minecraft.getInstance().getCurrentServer();
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
		JalistEntry jalistEntry = getSnitchFromStack(
				stack,
				getCurrentServer(),
				getSnitchCullDurationForServer(getCurrentServer()));
		if (jalistEntry != null) handleJalistEntry(jalistEntry);
	}

	public void handleRenderBlockOverlay(PoseStack matrices, float partialTicks) {
		if (!rangeOverlayVisible) return;
		RangesOverlayRenderer.renderRangesOverlay(matrices, partialTicks);
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

	public Collection<Snitch> getNearbySnitches(BlockPos playerPos) {
		if (store == null) return Collections.emptyList();
		final int renderDistance = 260; // TODO
		return store.getAllSnitches().stream()
				.filter(s -> playerPos.distSqr(s) < renderDistance)
				.sorted(Comparator.comparing(playerPos::distSqr))
				.limit(100)
				.collect(Collectors.toList());
	}

	/**
	 * milliseconds
	 */
	public long getSnitchCullDurationForServer(String server) {
		return 4L * 7L * 24L * 60L * 60L * 1000L; // 4 weeks (CivClassic config) TODO other servers CullDuration
	}
}
