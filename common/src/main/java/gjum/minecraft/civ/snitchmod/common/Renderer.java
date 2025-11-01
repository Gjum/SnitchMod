package gjum.minecraft.civ.snitchmod.common;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import gjum.minecraft.civ.snitchmod.common.Utils.Color;
import gjum.minecraft.civ.snitchmod.common.model.Snitch;
import gjum.minecraft.civ.snitchmod.common.model.SnitchFieldPreview;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
//import net.minecraft.client.renderer.CoreShaders;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
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
    private static PoseStack eventPoseStack = null;

    public static class RenderBufferGuard implements AutoCloseable {
        public MultiBufferSource.BufferSource bufferSource;
        private static ByteBufferBuilder byteBufferBuilder = new ByteBufferBuilder(256 * 1024);
        private static MultiBufferSource.BufferSource sBufferSource = null;
        private static RenderBufferGuard current = null;
        private RenderBufferGuard parent;
        private int referenceCount = 1;
        private boolean enableDepthTest;
        private boolean enableDepthMask;
        private boolean enableCullFace;
        private boolean initialDepthTestValue;
        private boolean initialDepthMask;
        private boolean initialCullFace;

        private RenderBufferGuard(boolean enableDepthTest, boolean enableDepthMask, boolean enableCullFace) {
            parent = current;
            current = this;

            this.enableDepthTest = enableDepthTest;
            this.enableDepthMask = enableDepthMask;
            this.enableCullFace = enableCullFace;
            initialDepthTestValue = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
            initialDepthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
            initialCullFace = GL11.glIsEnabled(GL11.GL_CULL_FACE);

            if (parent != null) {
                // There's already an active buffer; flush anything that's pending
                sBufferSource.endBatch();
                byteBufferBuilder.clear();
            }
            else {
                // There's no active buffer
                sBufferSource = MultiBufferSource.immediate(byteBufferBuilder);
            }
            bufferSource = sBufferSource;

            if (enableDepthTest) {
                GL11.glEnable(GL11.GL_DEPTH_TEST);
            }
            else {
                GL11.glDisable(GL11.GL_DEPTH_TEST);
            }
            GL11.glDepthMask(enableDepthMask);
            if (enableCullFace) {
                GL11.glEnable(GL11.GL_CULL_FACE);
            }
            else {
                GL11.glDisable(GL11.GL_CULL_FACE);
            }
        }

        public static RenderBufferGuard open(boolean enableDepthTest, boolean enableDepthMask, boolean enableCullFace) {
            if (current != null
                && current.enableDepthTest == enableDepthTest
                && current.enableDepthMask == enableDepthMask
                && current.enableCullFace == enableCullFace) {
                current.referenceCount += 1;
                return current;
            }

            return new RenderBufferGuard(enableDepthTest, enableDepthMask, enableCullFace);
        }

        public static RenderBufferGuard open() {
            if (current != null) {
                current.referenceCount += 1;
                return current;
            }

            return new RenderBufferGuard(
                GL11.glIsEnabled(GL11.GL_DEPTH_TEST),
                GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK),
                GL11.glIsEnabled(GL11.GL_CULL_FACE)
            );
        }

        @Override
        public void close() {
            if (referenceCount > 1) {
                // This will be flushed by an outer scope's close().
                referenceCount -= 1;
                return;
            }

            // All references to this guard have fallen out of scope
            current = parent;

            try {
                bufferSource.endBatch();
            }
            finally {
                if (initialDepthTestValue) {
                    GL11.glEnable(GL11.GL_DEPTH_TEST);
                }
                else {
                    GL11.glDisable(GL11.GL_DEPTH_TEST);
                }
                GL11.glDepthMask(initialDepthMask);
                if (initialCullFace) {
                    GL11.glEnable(GL11.GL_CULL_FACE);
                }
                else {
                    GL11.glDisable(GL11.GL_CULL_FACE);
                }

                if (parent == null) {
                    sBufferSource = null;
                }
            }
        }
    }

	public static void renderOverlays(PoseStack poseStack) {
        eventPoseStack = poseStack;
		if (mc.player == null) return;
		if (mc.level == null) return;

		if (mc.options.hideGui) return; // F1 mode
		// if (mc.options.renderDebug) return; // F3 mode

		Vec3 camPos = mc.gameRenderer.getMainCamera().getPosition();
		Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();
		modelViewStack.pushMatrix();
		modelViewStack.mul(eventPoseStack.last().pose());
		modelViewStack.translate((float) -camPos.x, (float) -camPos.y, (float) -camPos.z);

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

		RenderSystem.lineWidth(1.0F);

		modelViewStack.popMatrix();
        MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        buffers.endBatch();
        eventPoseStack = null;
	}

	private static void renderSnitchFieldPreview(SnitchFieldPreview preview) {
        final Color color = PINK;
        final float boxAlpha = 0.2f;
        final float lineAlpha = 1;
        final float lineWidth = 2;
        final int blockHlDist = 64;
		final AABB range = preview.field().getRangeAABB();
		// inflate/deflate so the box face isn't obscured by adjacent blocks
		final boolean playerInRange = range.contains(mc.player.position());
		AABB rangeBox = playerInRange ? range.inflate(-.01) : range.inflate(.01);

        try (RenderBufferGuard ignored = RenderBufferGuard.open(true, true, false)) {
            renderFilledBox(rangeBox, color, boxAlpha);
            renderBoxOutline(rangeBox, color, lineAlpha, lineWidth);
            renderBoxGuides(rangeBox, color, lineAlpha, lineWidth);
        }

		if (preview.field().pos.distSqr(mc.player.blockPosition()) < blockHlDist * blockHlDist) {
            try (RenderBufferGuard ignored = RenderBufferGuard.open(false, false, false)) {
                // inflate so it isn't obstructed by the snitch block
                final AABB blockBox = new AABB(preview.field().pos).inflate(.01);
                renderBoxOutline(blockBox, color, lineAlpha, lineWidth);
            }
		}
	}

	private static void renderSnitch(Snitch snitch) {
		final AABB range = snitch.getRangeAABB();
		// inflate/deflate so the box face isn't obscured by adjacent blocks
		final boolean playerInRange = range.contains(mc.player.position());
		AABB rangeBox = playerInRange ? range.inflate(-.01) : range.inflate(.01);
		AABB outlineBox = playerInRange ? range.inflate(-.01) : range.inflate(.01);
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

        final float lineWidth = 2;

        try (RenderBufferGuard ignored = RenderBufferGuard.open(true, true, false)) {
            renderFilledBox(rangeBox, snitchLiveliness.color, boxAlpha);
            if (!snitch.isGone()) {
                // Should include renderFilledBox too but that creates a bug, no idea why. When bugged, the snitch box lines
                // get rendered pure black when standing close (<~50 blocks?) instead of whatever we configured.

                renderBoxOutline(outlineBox, snitchLiveliness.color, lineAlpha, lineWidth);
            }
        }

		/*
		 * Render the snitch box.
		 */
		final int blockHlDist = 64;
		if (snitch.pos.distSqr(mc.player.blockPosition()) < blockHlDist * blockHlDist) {
			// inflate so it isn't obstructed by the snitch block
            try (RenderBufferGuard ignored = RenderBufferGuard.open(false, false, false)) {
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

        try (RenderBufferGuard ignored = RenderBufferGuard.open(true, true, false)) {
            renderFilledBox(helperBox, BLUE, 0.2f);
        }
	}

    private static void renderBoxOutline(AABB box, Color color, float alpha, float lineWidth) {
        //mc.gui.getChat().addMessage(Component.literal("[SnitchMod] renderBoxOutline: " + box));
        try (RenderBufferGuard guard = RenderBufferGuard.open()) {
            RenderSystem.lineWidth(lineWidth);
            VertexConsumer vertexConsumer = guard.bufferSource.getBuffer(RenderType.debugLineStrip(lineWidth));
            ShapeRenderer.renderLineBox(
                    eventPoseStack, vertexConsumer,
                    box.minX, box.minY, box.minZ,
                    box.maxX, box.maxY, box.maxZ,
                    color.r, color.g, color.b, alpha
            );
        }
    }

    private static void renderFilledBox(AABB box, Color color, float alpha) {
        //mc.gui.getChat().addMessage(Component.literal("[SnitchMod] renderFilledBox: " + box));
        try (RenderBufferGuard guard = RenderBufferGuard.open()) {
            VertexConsumer vertexConsumer = guard.bufferSource.getBuffer(RenderType.debugQuads());
            //ShapeRenderer.renderShape(eventPoseStack, vertexConsumer, Shapes.create(box), box.minX, box.minY, box.minZ, color.hex);
            float minX = (float) box.minX;
            float minY = (float) box.minY;
            float minZ = (float) box.minZ;
            float maxX = (float) box.maxX;
            float maxY = (float) box.maxY;
            float maxZ = (float) box.maxZ;
            for (Direction direction : Direction.values()) {
                ShapeRenderer.renderFace(
                        eventPoseStack, vertexConsumer, direction,
                        minX, minY, minZ,
                        maxX, maxY, maxZ,
                        color.r, color.g, color.b, alpha
                );
            }
        }
    }

    private static void renderBoxGuides(AABB box, Color color, float a, float lineWidth) {
        boolean initialLineSmooth = GL11.glIsEnabled(GL11.GL_LINE_SMOOTH);
        RenderSystem.lineWidth(lineWidth);

        float r = color.r;
        float g = color.g;
        float b = color.b;
        Vec3 center = box.getCenter();
        float radius = (float) (box.maxX - center.x);
        try (RenderBufferGuard guard = RenderBufferGuard.open()) {
            PoseStack poseStack = new PoseStack();
            PoseStack.Pose pose = poseStack.last();
            GL11.glEnable(GL11.GL_LINE_SMOOTH);
            VertexConsumer vertexConsumer = guard.bufferSource.getBuffer(RenderType.debugLineStrip(lineWidth));
            vertexConsumer.addVertex(pose, (float) center.x + 1, (float) center.y, (float) center.z).setColor(r, g, b, a).setNormal(1, 0, 0);
            vertexConsumer.addVertex(pose, (float) center.x + radius, (float) center.y, (float) center.z).setColor(r, g, b, a).setNormal(1, 0, 0);
            vertexConsumer.addVertex(pose, (float) center.x - 1, (float) center.y, (float) center.z).setColor(r, g, b, a).setNormal(-1, 0, 0);
            vertexConsumer.addVertex(pose, (float) center.x - radius, (float) center.y, (float) center.z).setColor(r, g, b, a).setNormal(-1, 0, 0);
            vertexConsumer.addVertex(pose, (float) center.x, (float) center.y + 1, (float) center.z).setColor(r, g, b, a).setNormal(0, 1, 0);
            vertexConsumer.addVertex(pose, (float) center.x, (float) center.y + radius, (float) center.z).setColor(r, g, b, a).setNormal(1, 0, 0);
            vertexConsumer.addVertex(pose, (float) center.x, (float) center.y - 1, (float) center.z).setColor(r, g, b, a).setNormal(0, -1, 0);
            vertexConsumer.addVertex(pose, (float) center.x, (float) center.y - radius, (float) center.z).setColor(r, g, b, a).setNormal(-1, 0, 0);
            vertexConsumer.addVertex(pose, (float) center.x, (float) center.y, (float) center.z + 1).setColor(r, g, b, a).setNormal(0, 0, 1);
            vertexConsumer.addVertex(pose, (float) center.x, (float) center.y, (float) center.z + radius).setColor(r, g, b, a).setNormal(0, 0, 1);
            vertexConsumer.addVertex(pose, (float) center.x, (float) center.y, (float) center.z - 1).setColor(r, g, b, a).setNormal(0, 0, -1);
            vertexConsumer.addVertex(pose, (float) center.x, (float) center.y, (float) center.z - radius).setColor(r, g, b, a).setNormal(0, 0, -1);
        }
        finally {
            if (!initialLineSmooth) {
                GL11.glDisable(GL11.GL_LINE_SMOOTH);
            }
        }
    }

	/**
	 * middle center of text is at `pos` before moving it down the screen by `offset`
	 */
	private static void renderTextFacingCamera(Component text, Vec3 pos, float offset, float scale, int colorAlphaHex) {
        float w = mc.font.width(text);
        float x = -w / 2f;
        float y = -(.5f - offset) * (mc.font.lineHeight + 2); // +2 for background padding, -1 for default line spacing
        boolean shadow = false;
        scale *= 0.005f * (mc.player.position().distanceTo(pos) / 2.4);
        scale = Math.clamp(scale, 0.015f, 0.15f);
        Matrix4f matrix = new Matrix4f(eventPoseStack.last().pose());
        matrix.scale(scale, -scale, 1); // third component determines background distance
        float bgOpacity = Minecraft.getInstance().options.getBackgroundOpacity(0.25f);
        int bgColor = (int) (bgOpacity * 255.0f) << 24;
        int flags = 0;
        // XXX somehow, the letters farthest from the crosshair render behind the background
        try (RenderBufferGuard guard = RenderBufferGuard.open(false, false, false)) {
            mc.font.drawInBatch(text, x, y, colorAlphaHex, shadow, matrix, guard.bufferSource, Font.DisplayMode.SEE_THROUGH, bgColor, flags);
        }

		/*var poseStack = new PoseStack();
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
		buffer.endBatch();*/
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
