package gjum.minecraft.civ.snitchmod.common;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import gjum.minecraft.civ.snitchmod.common.model.Snitch;
import gjum.minecraft.civ.snitchmod.common.model.SnitchFieldPreview;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.opengl.GL11;

import java.util.*;

import static gjum.minecraft.civ.snitchmod.common.SnitchMod.getMod;

public class Renderer {
	private final static Minecraft mc = Minecraft.getInstance();

	public static void renderOverlays(PoseStack poseStackArg) {
		if (mc.player == null) return;
		if (mc.level == null) return;

		if (mc.options.hideGui) return; // F1 mode
		// if (mc.options.renderDebug) return; // F3 mode

		Vec3 camPos = mc.gameRenderer.getMainCamera().getPosition();
		PoseStack modelViewStack = RenderSystem.getModelViewStack();
		modelViewStack.pushPose();
		modelViewStack.mulPoseMatrix(poseStackArg.last().pose());
		modelViewStack.translate(-camPos.x, -camPos.y, -camPos.z);
		RenderSystem.applyModelViewMatrix();

		if (getMod().rangeOverlayVisible) {
			int fieldDist = 260;
			getMod().streamNearbySnitches(mc.player.position(), fieldDist)
					.limit(100)
					.forEach(Renderer::renderSnitch);
		}

		if (getMod().placementHelperVisible) {
			int placeHelperDist = 50;
			getMod().streamNearbySnitches(mc.player.position(), placeHelperDist)
					.limit(10)
					.forEach(Renderer::renderPlacementHelper);
		}

		if (getMod().snitchFieldToPreview != null) {
			renderSnitchFieldPreview(getMod().snitchFieldToPreview);
		}

		RenderSystem.enableDepthTest();
		RenderSystem.depthMask(true);
		RenderSystem.enableCull();
		RenderSystem.lineWidth(1.0F);
		RenderSystem.clearColor(1, 1, 1, 1);

		modelViewStack.popPose();
		RenderSystem.applyModelViewMatrix();
	}

	private static void renderSnitchFieldPreview(SnitchFieldPreview preview) {
		float boxAlpha = 0.2f;
		float lineAlpha = 1;
		float lineWidth = 2;
		int blockHlDist = 64;

		final AABB range = preview.field().getRangeAABB();

		// inflate/deflate so the box face isn't obscured by adjacent blocks
		final boolean playerInRange = range.contains(mc.player.position());
		AABB rangeBox = playerInRange ? range.inflate(-.01) : range.inflate(.01);

		// green by default
		float r = 0.3f;
		float g = 0.8f;
		float b = 0.3f;

		RenderSystem.enableDepthTest();
		RenderSystem.enableBlend();

		RenderSystem.disableCull();
		renderFilledBox(rangeBox, r, g, b, boxAlpha);

		renderBoxOutline(rangeBox, r, g, b, lineAlpha, lineWidth);

		if (preview.field().pos.distSqr(mc.player.blockPosition()) < blockHlDist * blockHlDist) {
			RenderSystem.disableDepthTest();

			// inflate so it isn't obstructed by the snitch block
			final AABB blockBox = new AABB(preview.field().pos).inflate(.01);
			renderBoxOutline(blockBox, r, g, b, lineAlpha, lineWidth);
		}
	}

	private static void renderSnitch(Snitch snitch) {
		if (snitch.wasBroken()) return;
		// but still show culled/gone snitches

		float boxAlpha = 0.2f;
		float lineAlpha = 1;
		float lineWidth = 2;
		int blockHlDist = 64;

		long now = System.currentTimeMillis();

		final AABB range = snitch.getRangeAABB();

		// inflate/deflate so the box face isn't obscured by adjacent blocks
		final boolean playerInRange = range.contains(mc.player.position());
		AABB rangeBox = playerInRange ? range.inflate(-.01) : range.inflate(.01);
		AABB outlineBox = playerInRange ? range.inflate(-.05) : range.inflate(.05);

		final int white = 0xFF_EEEEEE;
		// #EE4056
		final int red = 0xFF_EE4056;
		// #EE8140
		final int orange = 0xFF_EE8140;

		// Yellow - #EED840
		float r = 0.93f;
		float g = 0.85f;
		float b = 0.25f;
		if (snitch.hasCullTs() && snitch.getCullTs() < now) {
			// Red - #EE4056
			r = .93f;
			g = .25f;
			b = .34f;
		} else if (snitch.hasCullTs() || (snitch.hasDormantTs() && snitch.getDormantTs() < now)) {
			// Orange - #EE8140
			r = .9f;
			g = .5f;
			b = .25f;
		}

		RenderSystem.enableDepthTest();
		RenderSystem.enableBlend();

		RenderSystem.disableCull();
		renderFilledBox(rangeBox, r, g, b, boxAlpha);

		renderBoxOutline(outlineBox, r, g, b, lineAlpha, lineWidth);

		if (snitch.pos.distSqr(mc.player.blockPosition()) < blockHlDist * blockHlDist) {
			RenderSystem.disableDepthTest();

			// inflate so it isn't obstructed by the snitch block
			final AABB blockBox = new AABB(snitch.pos).inflate(.01);
			renderBoxOutline(blockBox, r, g, b, lineAlpha, lineWidth);
			renderFilledBox(blockBox, r, g, b, boxAlpha);
		}

		record ColoredComponent(Component text, int colorAlphaHex) { }

		List<ColoredComponent> linesToRender = new ArrayList<>(3);
		boolean playerLookingAtSnitch = Utils.playerIsLookingAtSnitch(mc.player, snitch);
		if (playerInRange || playerLookingAtSnitch) {
			String name = snitch.getName();
			if (name != null && !name.isEmpty()) {
				linesToRender.add(new ColoredComponent(Component.literal(name), white));
			}

			String group = snitch.getGroup();
			if (group != null) {
				linesToRender.add(new ColoredComponent(Component.literal(String.format("[%s]", group)), white));
			}

			final String livelinessText;
			int livelinessTextColor = white;
			if (snitch.wasBroken()) {
				livelinessText = "broken " + timestampRelativeText(snitch.getBrokenTs());
				livelinessTextColor = red;
			} else if (snitch.isGone()) {
				livelinessText = "gone " + timestampRelativeText(snitch.getGoneTs());
				livelinessTextColor = red;
			} else if (snitch.hasDormantTs() && snitch.getDormantTs() > now) {
				livelinessText = "deactivates " + timestampRelativeText(snitch.getDormantTs());
				livelinessTextColor = white;
			} else if (snitch.hasCullTs() && snitch.getCullTs() < now) {
				livelinessText = "culled " + timestampRelativeText(snitch.getCullTs());
				livelinessTextColor = red;
			} else if (snitch.hasCullTs() && snitch.getCullTs() > now) {
				livelinessText = "culls " + timestampRelativeText(snitch.getCullTs());
				livelinessTextColor = orange;
			} else if (snitch.hasDormantTs() && snitch.getDormantTs() < now) {
				livelinessText = "deactivated " + timestampRelativeText(snitch.getDormantTs());
				livelinessTextColor = orange;
			} else {
				livelinessText = null;
			}
			if (livelinessText != null) {
				linesToRender.add(new ColoredComponent(Component.literal(livelinessText), livelinessTextColor));
			}

			if (playerLookingAtSnitch) {
				linesToRender.add(
					new ColoredComponent(
						Component.literal(
							String.format(
								"%d %d %d",
								snitch.getPos().getX(),
								snitch.getPos().getY(),
								snitch.getPos().getZ()
							)
						),
						white
					)
				);

				if (snitch.getType() != null) {
					linesToRender.add(
						new ColoredComponent(
							Component.literal(StringUtils.capitalize(snitch.getType().replaceAll("_", ""))),
							white
						)
					);
				}

				if (snitch.getLastSeenTs() != 0) {
					linesToRender.add(
						new ColoredComponent(
							Component.literal(
								String.format("last seen %s", timestampRelativeText(snitch.getLastSeenTs()))
							),
							white
						)
					);
				}
			}
		}

		Vec3 center = snitch.pos.getCenter();
		int offset = -1;
		for (ColoredComponent line : linesToRender) {
			renderTextFacingCamera(line.text, center, offset, 1f, line.colorAlphaHex);
			offset += 1;
		}
	}

	private static void renderPlacementHelper(Snitch snitch) {
		if (snitch.isGone()) return;
		long now = System.currentTimeMillis();
		if (snitch.hasCullTs() && snitch.getCullTs() < now) return;
		if (snitch.hasDormantTs() && snitch.getDormantTs() < now) return;

		// light blue
		float r = 0;
		float g = 0.7f;
		float b = 1;
		float alpha = 0.2f;

		final boolean playerInRange = snitch.getRangeAABB().contains(mc.player.position());
		if (playerInRange) return; // only render helper for snitches the player isn't inside of
		final AABB helperBox = new AABB(snitch.pos).inflate(22.3);

		RenderSystem.enableDepthTest();
		RenderSystem.enableBlend();

		RenderSystem.disableCull();
		renderFilledBox(helperBox, r, g, b, alpha);
	}

	private static void renderBoxOutline(AABB box, float r, float g, float b, float a, float lineWidth) {
		RenderSystem.lineWidth(lineWidth);

		Tesselator tesselator = Tesselator.getInstance();
		BufferBuilder bufferBuilder = tesselator.getBuilder();
		bufferBuilder.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);

		GL11.glEnable(GL11.GL_LINE_SMOOTH);

		bufferBuilder.vertex(box.minX, box.minY, box.minZ).color(r, g, b, a).endVertex();
		bufferBuilder.vertex(box.maxX, box.minY, box.minZ).color(r, g, b, a).endVertex();
		bufferBuilder.vertex(box.minX, box.minY, box.minZ).color(r, g, b, a).endVertex();
		bufferBuilder.vertex(box.minX, box.maxY, box.minZ).color(r, g, b, a).endVertex();
		bufferBuilder.vertex(box.minX, box.minY, box.minZ).color(r, g, b, a).endVertex();
		bufferBuilder.vertex(box.minX, box.minY, box.maxZ).color(r, g, b, a).endVertex();
		bufferBuilder.vertex(box.maxX, box.minY, box.minZ).color(r, g, b, a).endVertex();
		bufferBuilder.vertex(box.maxX, box.maxY, box.minZ).color(r, g, b, a).endVertex();
		bufferBuilder.vertex(box.maxX, box.maxY, box.minZ).color(r, g, b, a).endVertex();
		bufferBuilder.vertex(box.minX, box.maxY, box.minZ).color(r, g, b, a).endVertex();
		bufferBuilder.vertex(box.minX, box.maxY, box.minZ).color(r, g, b, a).endVertex();
		bufferBuilder.vertex(box.minX, box.maxY, box.maxZ).color(r, g, b, a).endVertex();
		bufferBuilder.vertex(box.minX, box.maxY, box.maxZ).color(r, g, b, a).endVertex();
		bufferBuilder.vertex(box.minX, box.minY, box.maxZ).color(r, g, b, a).endVertex();
		bufferBuilder.vertex(box.minX, box.minY, box.maxZ).color(r, g, b, a).endVertex();
		bufferBuilder.vertex(box.maxX, box.minY, box.maxZ).color(r, g, b, a).endVertex();
		bufferBuilder.vertex(box.maxX, box.minY, box.maxZ).color(r, g, b, a).endVertex();
		bufferBuilder.vertex(box.maxX, box.minY, box.minZ).color(r, g, b, a).endVertex();
		bufferBuilder.vertex(box.minX, box.maxY, box.maxZ).color(r, g, b, a).endVertex();
		bufferBuilder.vertex(box.maxX, box.maxY, box.maxZ).color(r, g, b, a).endVertex();
		bufferBuilder.vertex(box.maxX, box.minY, box.maxZ).color(r, g, b, a).endVertex();
		bufferBuilder.vertex(box.maxX, box.maxY, box.maxZ).color(r, g, b, a).endVertex();
		bufferBuilder.vertex(box.maxX, box.maxY, box.minZ).color(r, g, b, a).endVertex();
		bufferBuilder.vertex(box.maxX, box.maxY, box.maxZ).color(r, g, b, a).endVertex();

		tesselator.end();
	}

	private static void renderFilledBox(AABB box, float r, float g, float b, float a) {
		Tesselator tesselator = Tesselator.getInstance();
		BufferBuilder bufferBuilder = tesselator.getBuilder();
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		bufferBuilder.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);

		bufferBuilder.vertex(box.minX, box.minY, box.minZ).color(r, g, b, a).endVertex();
		bufferBuilder.vertex(box.minX, box.minY, box.minZ).color(r, g, b, a).endVertex();
		bufferBuilder.vertex(box.minX, box.minY, box.minZ).color(r, g, b, a).endVertex();
		bufferBuilder.vertex(box.minX, box.minY, box.maxZ).color(r, g, b, a).endVertex();
		bufferBuilder.vertex(box.minX, box.maxY, box.minZ).color(r, g, b, a).endVertex();
		bufferBuilder.vertex(box.minX, box.maxY, box.maxZ).color(r, g, b, a).endVertex();
		bufferBuilder.vertex(box.minX, box.maxY, box.maxZ).color(r, g, b, a).endVertex();
		bufferBuilder.vertex(box.minX, box.minY, box.maxZ).color(r, g, b, a).endVertex();
		bufferBuilder.vertex(box.maxX, box.maxY, box.maxZ).color(r, g, b, a).endVertex();
		bufferBuilder.vertex(box.maxX, box.minY, box.maxZ).color(r, g, b, a).endVertex();
		bufferBuilder.vertex(box.maxX, box.minY, box.maxZ).color(r, g, b, a).endVertex();
		bufferBuilder.vertex(box.maxX, box.minY, box.minZ).color(r, g, b, a).endVertex();
		bufferBuilder.vertex(box.maxX, box.maxY, box.maxZ).color(r, g, b, a).endVertex();
		bufferBuilder.vertex(box.maxX, box.maxY, box.minZ).color(r, g, b, a).endVertex();
		bufferBuilder.vertex(box.maxX, box.maxY, box.minZ).color(r, g, b, a).endVertex();
		bufferBuilder.vertex(box.maxX, box.minY, box.minZ).color(r, g, b, a).endVertex();
		bufferBuilder.vertex(box.minX, box.maxY, box.minZ).color(r, g, b, a).endVertex();
		bufferBuilder.vertex(box.minX, box.minY, box.minZ).color(r, g, b, a).endVertex();
		bufferBuilder.vertex(box.minX, box.minY, box.minZ).color(r, g, b, a).endVertex();
		bufferBuilder.vertex(box.maxX, box.minY, box.minZ).color(r, g, b, a).endVertex();
		bufferBuilder.vertex(box.minX, box.minY, box.maxZ).color(r, g, b, a).endVertex();
		bufferBuilder.vertex(box.maxX, box.minY, box.maxZ).color(r, g, b, a).endVertex();
		bufferBuilder.vertex(box.maxX, box.minY, box.maxZ).color(r, g, b, a).endVertex();
		bufferBuilder.vertex(box.minX, box.maxY, box.minZ).color(r, g, b, a).endVertex();
		bufferBuilder.vertex(box.minX, box.maxY, box.minZ).color(r, g, b, a).endVertex();
		bufferBuilder.vertex(box.minX, box.maxY, box.maxZ).color(r, g, b, a).endVertex();
		bufferBuilder.vertex(box.maxX, box.maxY, box.minZ).color(r, g, b, a).endVertex();
		bufferBuilder.vertex(box.maxX, box.maxY, box.maxZ).color(r, g, b, a).endVertex();
		bufferBuilder.vertex(box.maxX, box.maxY, box.maxZ).color(r, g, b, a).endVertex();
		bufferBuilder.vertex(box.maxX, box.maxY, box.maxZ).color(r, g, b, a).endVertex();

		tesselator.end();
	}

	/**
	 * middle center of text is at `pos` before moving it down the screen by `offset`
	 */
	private static void renderTextFacingCamera(Component text, Vec3 pos, float offset, float scale, int colorAlphaHex) {
		var poseStack = new PoseStack();
		poseStack.translate(pos.x, pos.y, pos.z);
		poseStack.mulPose(mc.gameRenderer.getMainCamera().rotation());
		scale *= 0.005f * (mc.player.position().distanceTo(pos)/2.4);
		scale = Utils.clamp(scale, 0.015f, 0.15f);
		poseStack.scale(-scale, -scale, 1); // third component determines background distance

		float w = mc.font.width(text);
		float x = -w / 2f;
		float y = -(.5f - offset) * (mc.font.lineHeight + 1); // +2 for background padding, -1 for default line spacing
		boolean shadow = false;
		var matrix = poseStack.last().pose();
		var buffer = mc.renderBuffers().bufferSource();
		float bgOpacity = Minecraft.getInstance().options.getBackgroundOpacity(0.25f);
		int bgColor = (int) (bgOpacity * 255.0f) << 24;
		int flags = 0;
		// XXX somehow, the letters farthest from the crosshair render behind the background
		mc.font.drawInBatch(text, x, y, colorAlphaHex, shadow, matrix, buffer, Font.DisplayMode.SEE_THROUGH, bgColor, flags);

		poseStack.popPose();
		buffer.endBatch();
	}

	private static @NotNull String timestampRelativeText(long ts) {
		long now = System.currentTimeMillis();
		if (ts < now) return durationToText(now - ts) + " ago";
		else return "in " + durationToText(ts - now);
	}

	/**
	 * Treats negative same as positive.
	 * Returns only one unit (either ms/s/min/h/days).
	 */
	private static @NotNull String durationToText(long ms) {
		if (ms < 0) ms = -ms;
		if (ms < 1000) return ms + "ms";
		long sec = ms / 1000;
		if (sec < 100) return sec + "s";
		long min = sec / 60;
		if (min < 100) return min + "min";
		long hours = min / 60;
		if (hours < 2 * 24) return hours + "h";
		long days = hours / 24;
		return days + " days";
	}
}
