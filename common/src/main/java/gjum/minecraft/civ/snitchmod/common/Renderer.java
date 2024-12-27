package gjum.minecraft.civ.snitchmod.common;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import gjum.minecraft.civ.snitchmod.common.Utils.Color;
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
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

import static gjum.minecraft.civ.snitchmod.common.SnitchMod.getMod;

public class Renderer {
	private final static Minecraft mc = Minecraft.getInstance();

	private final static Color BLACK = new Color(0x333333);
	private final static Color WHITE = new Color(0xEEEEEE);
	private final static Color RED = new Color(0xEE4056);
	private final static Color GREEN = new Color(0x28CC52);
	private final static Color BLUE = new Color(0x00B2FF);
	private final static Color YELLOW = new Color(0xEED840);
	private final static Color ORANGE = new Color(0xEE8140);
	private final static Color PINK = new Color(0xFC66CC);

	public static void renderOverlays(Matrix4f matrixArg) {
		if (mc.player == null) return;
		if (mc.level == null) return;

		if (mc.options.hideGui) return; // F1 mode
		// if (mc.options.renderDebug) return; // F3 mode

		Vec3 camPos = mc.gameRenderer.getMainCamera().getPosition();
		Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();
		modelViewStack.pushMatrix();
		modelViewStack.mul(matrixArg);
		modelViewStack.translate((float) -camPos.x, (float) -camPos.y, (float) -camPos.z);
		RenderSystem.applyModelViewMatrix();

		if (getMod().rangeOverlayVisible) {
			int fieldDist = 260;
			getMod().streamNearbySnitches(mc.player.position(), fieldDist)
				// but still show culled/gone snitches
				.filter(s -> !s.wasBroken())
				.limit(100)
				.forEach(Renderer::renderSnitch);
		}

		if (getMod().placementHelperVisible) {
			int placeHelperDist = 50;
			getMod().streamNearbySnitches(mc.player.position(), placeHelperDist)
				.filter(Snitch::isAlive)
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

		modelViewStack.popMatrix();
		RenderSystem.applyModelViewMatrix();
	}

	private static void renderSnitchFieldPreview(SnitchFieldPreview preview) {
		final AABB range = preview.field().getRangeAABB();
		// inflate/deflate so the box face isn't obscured by adjacent blocks
		final boolean playerInRange = range.contains(mc.player.position());
		AABB rangeBox = playerInRange ? range.inflate(-.01) : range.inflate(.01);

		RenderSystem.enableDepthTest();
		RenderSystem.enableBlend();
		RenderSystem.disableCull();

		final Color color = PINK;
		final float boxAlpha = 0.2f;
		renderFilledBox(rangeBox, color, boxAlpha);

		final float lineAlpha = 1;
		final float lineWidth = 2;
		renderBoxOutline(rangeBox, color, lineAlpha, lineWidth);
		renderBoxGuides(rangeBox, color, lineAlpha, lineWidth);

		final int blockHlDist = 64;
		if (preview.field().pos.distSqr(mc.player.blockPosition()) < blockHlDist * blockHlDist) {
			RenderSystem.disableDepthTest();

			// inflate so it isn't obstructed by the snitch block
			final AABB blockBox = new AABB(preview.field().pos).inflate(.01);
			renderBoxOutline(blockBox, color, lineAlpha, lineWidth);
		}
	}

	private static void renderSnitch(Snitch snitch) {
		final AABB range = snitch.getRangeAABB();
		// inflate/deflate so the box face isn't obscured by adjacent blocks
		final boolean playerInRange = range.contains(mc.player.position());
		AABB rangeBox = playerInRange ? range.inflate(-.01) : range.inflate(.01);
		AABB outlineBox = playerInRange ? range.inflate(-.05) : range.inflate(.05);
		if (playerInRange) {
			snitch.maybeRefreshed = true;
		}

		enum SnitchLiveliness {
			BROKEN(RED),
			GONE(RED),
			LONG_TIME_NOT_SEEN(RED),
			CULLED(RED),
			WILL_CULL(ORANGE),
			WILL_CULL_MAYBE_REFRESHED(GREEN),
			DORMANT_NOW(ORANGE),
			DORMANT_NOW_MAYBE_REFRESHED(GREEN),
			DORMANT_SOON(ORANGE),
			DORMANT_SOON_MAYBE_REFRESHED(GREEN),
			DORMANT_SOONISH(YELLOW),
			DORMANT_SOONISH_MAYBE_REFRESHED(GREEN),
			DORMANT_EVENTUALLY(GREEN);

			private final Color color;

			SnitchLiveliness(Color color) {
				this.color = color;
			}
		}

		long now = System.currentTimeMillis();
		long snitchTimer = snitch.getType() != null ? snitch.getType().timer : Snitch.Type.NOTEBLOCK.timer;
		SnitchLiveliness snitchLiveliness = SnitchLiveliness.DORMANT_EVENTUALLY;
		if (snitch.wasBroken()) {
			snitchLiveliness = SnitchLiveliness.BROKEN;
		} else if (snitch.isGone()) {
			snitchLiveliness = SnitchLiveliness.GONE;
		} else if (snitch.getLastSeenTs() != 0 && now - snitch.getLastSeenTs() > snitchTimer) {
			snitchLiveliness = SnitchLiveliness.LONG_TIME_NOT_SEEN;
		} else if (snitch.hasCullTs() && snitch.getCullTs() < now) {
			snitchLiveliness = SnitchLiveliness.CULLED;
		} else if (snitch.hasCullTs() && snitch.getCullTs() > now) {
			snitchLiveliness = snitch.maybeRefreshed
				? SnitchLiveliness.WILL_CULL_MAYBE_REFRESHED
				: SnitchLiveliness.WILL_CULL;
		} else if (snitch.hasDormantTs() && snitch.getDormantTs() < now) {
			snitchLiveliness = snitch.maybeRefreshed
				? SnitchLiveliness.DORMANT_NOW_MAYBE_REFRESHED
				: SnitchLiveliness.DORMANT_NOW;
		} else if (snitch.hasDormantTs() && snitch.getDormantTs() > now) {
			final long goodThreshold = 1000L * 60L * 60L * 24L * 7L;
			final long badThreshold = 1000L * 60L * 60L * 24L * 3L;
			final long delta = snitch.getDormantTs() - now;
			if (delta >= goodThreshold) {
				// no-op: default
			} else if (delta >= badThreshold) {
				snitchLiveliness = snitch.maybeRefreshed
					? SnitchLiveliness.DORMANT_SOONISH_MAYBE_REFRESHED
					: SnitchLiveliness.DORMANT_SOONISH;
			} else {
				snitchLiveliness = snitch.maybeRefreshed
					? SnitchLiveliness.DORMANT_SOON_MAYBE_REFRESHED
					: SnitchLiveliness.DORMANT_SOON;
			}
		}

		float boxAlpha = 0.2f;
		float lineAlpha = 1;
		if (snitch.isGone()) {
			// Workaround for a bug in renderFilledBox described below.
			boxAlpha = 0f;
			lineAlpha = 0.5f;
		}

		/*
		 * Render the snitch range box.
		 */
		RenderSystem.enableDepthTest();
		RenderSystem.enableBlend();
		RenderSystem.disableCull();

		renderFilledBox(rangeBox, snitchLiveliness.color, boxAlpha);

		final float lineWidth = 2;
		if (!snitch.isGone()) {
			// Should include renderFilledBox too but that creates a bug, no idea why. When bugged, the snitch box lines
			// get rendered pure black when standing close (<~50 blocks?) instead of whatever we configured.

			renderBoxOutline(outlineBox, snitchLiveliness.color, lineAlpha, lineWidth);
		}

		/*
		 * Render the snitch box.
		 */
		final int blockHlDist = 64;
		if (snitch.pos.distSqr(mc.player.blockPosition()) < blockHlDist * blockHlDist) {
			RenderSystem.disableDepthTest();

			// inflate so it isn't obstructed by the snitch block
			final AABB blockBox = new AABB(snitch.pos).inflate(.01);
			Color boxOutlineColor = snitchLiveliness.color;
			if (
				getMod().snitchFieldToPreview != null
				&& getMod().snitchFieldToPreview.source().equals(snitch)
			) {
				boxOutlineColor = PINK;
			}
			renderBoxOutline(blockBox, boxOutlineColor, lineAlpha, lineWidth);

			Color boxFillColor = snitch.isGone() ? BLACK : snitchLiveliness.color;
			renderFilledBox(blockBox, boxFillColor, boxAlpha);
		}

		/*
		 * Render the snitch box text.
		 */
		record ColoredComponent(Component text, Color color) {}

		List<ColoredComponent> linesToRender = new ArrayList<>(3);
		boolean playerLookingAtSnitch = Utils.playerIsLookingAtSnitch(mc.player, snitch);
		// Not using mc.player.getEyePosition() nor mc.gameRenderer.getMainCamera().getPosition() because
		// they return a position that is too high. We simply want the block position of our head.
		Vec3 eyePosition = new Vec3(mc.player.position().x, mc.player.position().y + 1, mc.player.position().z);
		if (
			(
				(!snitch.isGone() && playerInRange)
				|| playerLookingAtSnitch
			)
			// Text of close by snitches at our eye level obscures our vision.
			&& (
				eyePosition.y != snitch.pos.getY()
				|| eyePosition.distanceTo(snitch.pos.getCenter()) > 3
			)
		) {
			String name = snitch.getName();
			if (name != null && !name.isEmpty()) {
				linesToRender.add(new ColoredComponent(Component.literal(name), WHITE));
			}

			String group = snitch.getGroup();
			if (group != null) {
				linesToRender.add(new ColoredComponent(Component.literal(String.format("[%s]", group)), WHITE));
			}

			String lastSeenText = null;
			if (snitch.getLastSeenTs() != 0) {
				lastSeenText = String.format("last seen %s", timestampRelativeText(snitch.getLastSeenTs()));
			}

			String maxSnitchTimerText = "";
			if (playerLookingAtSnitch) {
				maxSnitchTimerText = String.format(" / %s", durationToText(snitchTimer));
			}

			String livelinessText = null;
			switch (snitchLiveliness) {
				case BROKEN:
					livelinessText = "broken " + timestampRelativeText(snitch.getBrokenTs());
					break;
				case GONE:
					livelinessText = "gone " + timestampRelativeText(snitch.getGoneTs());
					break;
				case LONG_TIME_NOT_SEEN:
					livelinessText = lastSeenText;
					break;
				case CULLED:
					livelinessText = "culled " + timestampRelativeText(snitch.getCullTs());
					break;
				case WILL_CULL, WILL_CULL_MAYBE_REFRESHED:
					livelinessText = String.format(
						"culls %s%s",
						timestampRelativeText(snitch.getCullTs()),
						maxSnitchTimerText
					);
					break;
				case DORMANT_NOW, DORMANT_NOW_MAYBE_REFRESHED:
					livelinessText = "deactivated " + timestampRelativeText(snitch.getDormantTs());
					break;
				case DORMANT_SOON, DORMANT_SOON_MAYBE_REFRESHED, DORMANT_SOONISH, DORMANT_SOONISH_MAYBE_REFRESHED,
						DORMANT_EVENTUALLY:
					if (snitch.hasDormantTs()) {
						livelinessText = String.format(
							"deactivates %s%s",
							timestampRelativeText(snitch.getDormantTs()),
							maxSnitchTimerText
						);
					}
					break;
			}
			if (livelinessText != null) {
				if (snitch.maybeRefreshed) {
					switch (snitchLiveliness) {
						case WILL_CULL_MAYBE_REFRESHED, DORMANT_NOW_MAYBE_REFRESHED, DORMANT_SOON_MAYBE_REFRESHED,
								DORMANT_SOONISH_MAYBE_REFRESHED, DORMANT_EVENTUALLY:
							livelinessText = livelinessText + " (refreshed?)";
							break;
						default:
					}
				}
				linesToRender.add(new ColoredComponent(Component.literal(livelinessText), snitchLiveliness.color));
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
								WHITE
						)
				);

				if (snitch.getType() != null) {
					linesToRender.add(
						new ColoredComponent(
							Component.literal(StringUtils.capitalize(snitch.getType().toString().replaceAll("_", ""))),
							WHITE
						)
					);
				}

				if (lastSeenText != null && snitchLiveliness != SnitchLiveliness.LONG_TIME_NOT_SEEN) {
					linesToRender.add(new ColoredComponent(Component.literal(lastSeenText), WHITE));
				}
			}
		}

		Vec3 center = snitch.pos.getCenter();
		int offset = -1;
		for (ColoredComponent line : linesToRender) {
			renderTextFacingCamera(line.text, center, offset, 1f, line.color.hex);
			offset += 1;
		}
	}

	private static void renderPlacementHelper(Snitch snitch) {
		final boolean playerInRange = snitch.getRangeAABB().contains(mc.player.position());
		if (playerInRange) return; // only render helper for snitches the player isn't inside of
		final AABB helperBox = new AABB(snitch.pos).inflate(22.3);

		RenderSystem.enableDepthTest();
		RenderSystem.enableBlend();
		RenderSystem.disableCull();

		Color color = BLUE;
		float alpha = 0.2f;
		renderFilledBox(helperBox, color, alpha);
	}

	private static void renderBoxOutline(AABB box, Color color, float a, float lineWidth) {
		RenderSystem.lineWidth(lineWidth);

		Tesselator tesselator = Tesselator.getInstance();
		BufferBuilder bufferBuilder = tesselator.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);

		GL11.glEnable(GL11.GL_LINE_SMOOTH);

		float r = color.r;
		float g = color.g;
		float b = color.b;
		float minX = (float) box.minX;
		float minY = (float) box.minY;
		float minZ = (float) box.minZ;
		float maxX = (float) box.maxX;
		float maxY = (float) box.maxY;
		float maxZ = (float) box.maxZ;

		bufferBuilder.addVertex(minX, minY, minZ).setColor(r, g, b, a);
		bufferBuilder.addVertex(maxX, minY, minZ).setColor(r, g, b, a);
		bufferBuilder.addVertex(minX, minY, minZ).setColor(r, g, b, a);
		bufferBuilder.addVertex(minX, maxY, minZ).setColor(r, g, b, a);
		bufferBuilder.addVertex(minX, minY, minZ).setColor(r, g, b, a);
		bufferBuilder.addVertex(minX, minY, maxZ).setColor(r, g, b, a);
		bufferBuilder.addVertex(maxX, minY, minZ).setColor(r, g, b, a);
		bufferBuilder.addVertex(maxX, maxY, minZ).setColor(r, g, b, a);
		bufferBuilder.addVertex(maxX, maxY, minZ).setColor(r, g, b, a);
		bufferBuilder.addVertex(minX, maxY, minZ).setColor(r, g, b, a);
		bufferBuilder.addVertex(minX, maxY, minZ).setColor(r, g, b, a);
		bufferBuilder.addVertex(minX, maxY, maxZ).setColor(r, g, b, a);
		bufferBuilder.addVertex(minX, maxY, maxZ).setColor(r, g, b, a);
		bufferBuilder.addVertex(minX, minY, maxZ).setColor(r, g, b, a);
		bufferBuilder.addVertex(minX, minY, maxZ).setColor(r, g, b, a);
		bufferBuilder.addVertex(maxX, minY, maxZ).setColor(r, g, b, a);
		bufferBuilder.addVertex(maxX, minY, maxZ).setColor(r, g, b, a);
		bufferBuilder.addVertex(maxX, minY, minZ).setColor(r, g, b, a);
		bufferBuilder.addVertex(minX, maxY, maxZ).setColor(r, g, b, a);
		bufferBuilder.addVertex(maxX, maxY, maxZ).setColor(r, g, b, a);
		bufferBuilder.addVertex(maxX, minY, maxZ).setColor(r, g, b, a);
		bufferBuilder.addVertex(maxX, maxY, maxZ).setColor(r, g, b, a);
		bufferBuilder.addVertex(maxX, maxY, minZ).setColor(r, g, b, a);
		bufferBuilder.addVertex(maxX, maxY, maxZ).setColor(r, g, b, a);

		BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());
	}

	private static void renderFilledBox(AABB box, Color color, float a) {
		Tesselator tesselator = Tesselator.getInstance();
		BufferBuilder bufferBuilder = tesselator.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
		RenderSystem.setShader(GameRenderer::getPositionColorShader);

		float r = color.r;
		float g = color.g;
		float b = color.b;
		float minX = (float) box.minX;
		float minY = (float) box.minY;
		float minZ = (float) box.minZ;
		float maxX = (float) box.maxX;
		float maxY = (float) box.maxY;
		float maxZ = (float) box.maxZ;

		bufferBuilder.addVertex(minX, minY, minZ).setColor(r, g, b, a);
		bufferBuilder.addVertex(minX, minY, minZ).setColor(r, g, b, a);
		bufferBuilder.addVertex(minX, minY, minZ).setColor(r, g, b, a);
		bufferBuilder.addVertex(minX, minY, maxZ).setColor(r, g, b, a);
		bufferBuilder.addVertex(minX, maxY, minZ).setColor(r, g, b, a);
		bufferBuilder.addVertex(minX, maxY, maxZ).setColor(r, g, b, a);
		bufferBuilder.addVertex(minX, maxY, maxZ).setColor(r, g, b, a);
		bufferBuilder.addVertex(minX, minY, maxZ).setColor(r, g, b, a);
		bufferBuilder.addVertex(maxX, maxY, maxZ).setColor(r, g, b, a);
		bufferBuilder.addVertex(maxX, minY, maxZ).setColor(r, g, b, a);
		bufferBuilder.addVertex(maxX, minY, maxZ).setColor(r, g, b, a);
		bufferBuilder.addVertex(maxX, minY, minZ).setColor(r, g, b, a);
		bufferBuilder.addVertex(maxX, maxY, maxZ).setColor(r, g, b, a);
		bufferBuilder.addVertex(maxX, maxY, minZ).setColor(r, g, b, a);
		bufferBuilder.addVertex(maxX, maxY, minZ).setColor(r, g, b, a);
		bufferBuilder.addVertex(maxX, minY, minZ).setColor(r, g, b, a);
		bufferBuilder.addVertex(minX, maxY, minZ).setColor(r, g, b, a);
		bufferBuilder.addVertex(minX, minY, minZ).setColor(r, g, b, a);
		bufferBuilder.addVertex(minX, minY, minZ).setColor(r, g, b, a);
		bufferBuilder.addVertex(maxX, minY, minZ).setColor(r, g, b, a);
		bufferBuilder.addVertex(minX, minY, maxZ).setColor(r, g, b, a);
		bufferBuilder.addVertex(maxX, minY, maxZ).setColor(r, g, b, a);
		bufferBuilder.addVertex(maxX, minY, maxZ).setColor(r, g, b, a);
		bufferBuilder.addVertex(minX, maxY, minZ).setColor(r, g, b, a);
		bufferBuilder.addVertex(minX, maxY, minZ).setColor(r, g, b, a);
		bufferBuilder.addVertex(minX, maxY, maxZ).setColor(r, g, b, a);
		bufferBuilder.addVertex(maxX, maxY, minZ).setColor(r, g, b, a);
		bufferBuilder.addVertex(maxX, maxY, maxZ).setColor(r, g, b, a);
		bufferBuilder.addVertex(maxX, maxY, maxZ).setColor(r, g, b, a);
		bufferBuilder.addVertex(maxX, maxY, maxZ).setColor(r, g, b, a);

		BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());
	}

	private static void renderBoxGuides(AABB box, Color color, float a, float lineWidth) {
		RenderSystem.lineWidth(lineWidth);

		Tesselator tesselator = Tesselator.getInstance();
		BufferBuilder bufferBuilder = tesselator.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
		RenderSystem.setShader(GameRenderer::getPositionColorShader);

		GL11.glEnable(GL11.GL_LINE_SMOOTH);

		float r = color.r;
		float g = color.g;
		float b = color.b;

		Vec3 center = box.getCenter();
		double radius = box.maxX - center.x;

		addVertex(bufferBuilder, center.x + 1, center.y, center.z).setColor(r, g, b, a);
		addVertex(bufferBuilder, center.x + radius, center.y, center.z).setColor(r, g, b, a);
		addVertex(bufferBuilder, center.x - 1, center.y, center.z).setColor(r, g, b, a);
		addVertex(bufferBuilder, center.x - radius, center.y, center.z).setColor(r, g, b, a);

		addVertex(bufferBuilder, center.x, center.y + 1, center.z).setColor(r, g, b, a);
		addVertex(bufferBuilder, center.x, center.y + radius, center.z).setColor(r, g, b, a);
		addVertex(bufferBuilder, center.x, center.y - 1, center.z).setColor(r, g, b, a);
		addVertex(bufferBuilder, center.x, center.y - radius, center.z).setColor(r, g, b, a);

		addVertex(bufferBuilder, center.x, center.y, center.z + 1).setColor(r, g, b, a);
		addVertex(bufferBuilder, center.x, center.y, center.z + radius).setColor(r, g, b, a);
		addVertex(bufferBuilder, center.x, center.y, center.z - 1).setColor(r, g, b, a);
		addVertex(bufferBuilder, center.x, center.y, center.z - radius).setColor(r, g, b, a);

		BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());
	}

	// does the double->float cast for us
	private static VertexConsumer addVertex(VertexConsumer bufferBuilder, double x, double y, double z) {
		return bufferBuilder.addVertex((float) x, (float) y, (float) z);
	}

	/**
	 * middle center of text is at `pos` before moving it down the screen by `offset`
	 */
	private static void renderTextFacingCamera(Component text, Vec3 pos, float offset, float scale, int colorAlphaHex) {
		var poseStack = new PoseStack();
		poseStack.translate(pos.x, pos.y, pos.z);
		poseStack.mulPose(mc.gameRenderer.getMainCamera().rotation());
		scale *= 0.005f * (mc.player.position().distanceTo(pos) / 2.4);
		scale = Math.clamp(scale, 0.015f, 0.15f);
		poseStack.scale(scale, -scale, 1); // third component determines background distance

		float w = mc.font.width(text);
		float x = -w / 2f;
		float y = -(.5f - offset) * (mc.font.lineHeight + 2); // +2 for background padding, -1 for default line spacing
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
		return "in " + durationToText(ts - now);
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
