package gjum.minecraft.civ.snitchmod.common;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import gjum.minecraft.civ.snitchmod.common.model.Snitch;
import gjum.minecraft.civ.snitchmod.common.model.SnitchFieldPreview;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
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

		// yellow by default
		float r = 1;
		float g = 1;
		float b = 0;
		if (snitch.hasCullTs() && snitch.getCullTs() < now) {
			g = 0; // red
		} else if (snitch.hasDormantTs() && snitch.getDormantTs() < now) {
			g = .5f; // orange
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
		}

		List<Component> linesToRender = new ArrayList<>(3);
		boolean playerLookingAtSnitch = playerIsLookingAtSnitch(mc.player, snitch);
		if (playerInRange || playerLookingAtSnitch) {
			String name = snitch.getName();
			if (name != null && !name.isEmpty()) {
				linesToRender.add(Component.literal(name));
			}

			String group = snitch.getGroup();
			if (group != null) {
				linesToRender.add(Component.literal(String.format("[%s]", group)));
			}

			final String livelinessText;
			if (snitch.wasBroken()) {
				livelinessText = "broken " + timestampRelativeText(snitch.getBrokenTs());
			} else if (snitch.isGone()) {
				livelinessText = "gone " + timestampRelativeText(snitch.getGoneTs());
			} else if (snitch.hasDormantTs() && snitch.getDormantTs() > now) {
				livelinessText = "deactivates " + timestampRelativeText(snitch.getDormantTs());
			} else if (snitch.hasCullTs() && snitch.getCullTs() < now) {
				livelinessText = "culled " + timestampRelativeText(snitch.getCullTs());
			} else if (snitch.hasCullTs() && snitch.getCullTs() > now) {
				livelinessText = "culls " + timestampRelativeText(snitch.getCullTs());
			} else if (snitch.hasDormantTs() && snitch.getDormantTs() < now) {
				livelinessText = "deactivated " + timestampRelativeText(snitch.getDormantTs());
			} else livelinessText = null;

			if (livelinessText != null) {
				linesToRender.add(Component.literal(livelinessText));
			}

			if (playerLookingAtSnitch) {
				linesToRender.add(
					Component.literal(
						String.format(
							"%d %d %d",
							snitch.getPos().getX(),
							snitch.getPos().getY(),
							snitch.getPos().getZ()
						)
					)
				);

				if (snitch.getType() != null) {
					linesToRender.add(
						Component.literal(
							String.format(
								"%s",
								StringUtils.capitalize(snitch.getType().replaceAll("_", ""))
							)
						)
					);
				}
			}
		}

		Vec3 center = snitch.pos.getCenter();
		int offset = -1;
		for (Component line : linesToRender) {
			renderTextFacingCamera(line, center, offset, 1f);
			offset += 1;
		}
	}

	public static boolean playerIsLookingAtSnitch(LocalPlayer player, Snitch snitch) {
		Vec3 playerLookAngle = player.getLookAngle();
		Vec3 playerPos = player.getEyePosition();
		AABB snitchBox = new AABB(snitch.pos);

		double invertedLookAngleX = 1/playerLookAngle.x;
		double tx1 = (snitchBox.minX - playerPos.x) * invertedLookAngleX;
		double tx2 = (snitchBox.maxX - playerPos.x) * invertedLookAngleX;

		double tmin = Math.min(tx1, tx2);
		double tmax = Math.max(tx1, tx2);

		double invertedLookAngleY = 1/playerLookAngle.y;
		double ty1 = (snitchBox.minY - playerPos.y) * invertedLookAngleY;
		double ty2 = (snitchBox.maxY - playerPos.y) * invertedLookAngleY;

		tmin = Math.max(tmin, Math.min(ty1, ty2));
		tmax = Math.min(tmax, Math.max(ty1, ty2));

		double invertedLookAngleZ = 1/playerLookAngle.z;
		double tz1 = (snitchBox.minZ - playerPos.z) * invertedLookAngleZ;
		double tz2 = (snitchBox.maxZ - playerPos.z) * invertedLookAngleZ;

		tmin = Math.max(tmin, Math.min(tz1, tz2));
		tmax = Math.min(tmax, Math.max(tz1, tz2));

		return tmax >= 0 && tmax >= tmin;
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
	private static void renderTextFacingCamera(Component text, Vec3 pos, float offset, float scale) {
		var poseStack = new PoseStack();
		poseStack.translate(pos.x, pos.y, pos.z);
		poseStack.mulPose(mc.gameRenderer.getMainCamera().rotation());
		scale *= 0.025f;
		poseStack.scale(-scale, -scale, 1); // third component determines background distance

		float w = mc.font.width(text);
		float x = -w / 2f;
		float y = -(.5f - offset) * (mc.font.lineHeight + 1); // +2 for background padding, -1 for default line spacing
		int color = 0xFF_FFFFFF;
		boolean shadow = false;
		var matrix = poseStack.last().pose();
		var buffer = mc.renderBuffers().bufferSource();
		float bgOpacity = Minecraft.getInstance().options.getBackgroundOpacity(0.25f);
		int bgColor = (int) (bgOpacity * 255.0f) << 24;
		int flags = 0;
		// XXX somehow, the letters farthest from the crosshair render behind the background
		mc.font.drawInBatch(text, x, y, color, shadow, matrix, buffer, Font.DisplayMode.SEE_THROUGH, bgColor, flags);

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
