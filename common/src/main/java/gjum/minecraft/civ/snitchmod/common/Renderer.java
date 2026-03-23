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
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;
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

		Vec3 camPos = mc.gameRenderer.getMainCamera().position();
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
		// Convert the requested line width into a world-space thickness.
		// Tweak the multiplier if you want thicker/thinner outlines visually.
		double t = Math.max(0.01, lineWidth * 0.02);

		double minX = box.minX;
		double minY = box.minY;
		double minZ = box.minZ;
		double maxX = box.maxX;
		double maxY = box.maxY;
		double maxZ = box.maxZ;

		// Clamp thickness so it cannot exceed half the box size on any axis.
		double maxThicknessX = Math.max(0.0, (maxX - minX) / 2.0);
		double maxThicknessY = Math.max(0.0, (maxY - minY) / 2.0);
		double maxThicknessZ = Math.max(0.0, (maxZ - minZ) / 2.0);
		t = Math.min(t, Math.min(maxThicknessX, Math.min(maxThicknessY, maxThicknessZ)));

		if (t <= 0.0) {
			return;
		}

		// Bottom ring
		renderFilledBox(new AABB(minX, minY, minZ, maxX, minY + t, minZ + t), color, alpha); // north
		renderFilledBox(new AABB(minX, minY, maxZ - t, maxX, minY + t, maxZ), color, alpha); // south
		renderFilledBox(new AABB(minX, minY, minZ + t, minX + t, minY + t, maxZ - t), color, alpha); // west
		renderFilledBox(new AABB(maxX - t, minY, minZ + t, maxX, minY + t, maxZ - t), color, alpha); // east

		// Top ring
		renderFilledBox(new AABB(minX, maxY - t, minZ, maxX, maxY, minZ + t), color, alpha); // north
		renderFilledBox(new AABB(minX, maxY - t, maxZ - t, maxX, maxY, maxZ), color, alpha); // south
		renderFilledBox(new AABB(minX, maxY - t, minZ + t, minX + t, maxY, maxZ - t), color, alpha); // west
		renderFilledBox(new AABB(maxX - t, maxY - t, minZ + t, maxX, maxY, maxZ - t), color, alpha); // east

		// Vertical edges
		renderFilledBox(new AABB(minX, minY + t, minZ, minX + t, maxY - t, minZ + t), color, alpha); // NW
		renderFilledBox(new AABB(maxX - t, minY + t, minZ, maxX, maxY - t, minZ + t), color, alpha); // NE
		renderFilledBox(new AABB(minX, minY + t, maxZ - t, minX + t, maxY - t, maxZ), color, alpha); // SW
		renderFilledBox(new AABB(maxX - t, minY + t, maxZ - t, maxX, maxY - t, maxZ), color, alpha); // SE
	}

    private static void renderFilledBox(AABB box, Color color, float alpha) {
        //mc.gui.getChat().addMessage(Component.literal("[SnitchMod] renderFilledBox: " + box));
        try (RenderBufferGuard guard = RenderBufferGuard.open()) {
			VertexConsumer vertexConsumer = guard.bufferSource.getBuffer(RenderTypes.debugQuads());
            //ShapeRenderer.renderShape(eventPoseStack, vertexConsumer, Shapes.create(box), box.minX, box.minY, box.minZ, color.hex);
            float minX = (float) box.minX;
            float minY = (float) box.minY;
            float minZ = (float) box.minZ;
            float maxX = (float) box.maxX;
            float maxY = (float) box.maxY;
            float maxZ = (float) box.maxZ;
			int alphaInt = Math.round(alpha * 255.0f) & 0xFF;
			int colorInt = (alphaInt << 24) | (color.hex & 0x00FFFFFF);
			PoseStack.Pose pose = eventPoseStack.last();

			// Bottom (-Y)
			vertexConsumer.addVertex(pose, minX, minY, minZ).setColor(colorInt);
			vertexConsumer.addVertex(pose, maxX, minY, minZ).setColor(colorInt);
			vertexConsumer.addVertex(pose, maxX, minY, maxZ).setColor(colorInt);
			vertexConsumer.addVertex(pose, minX, minY, maxZ).setColor(colorInt);

			// Top (+Y)
			vertexConsumer.addVertex(pose, minX, maxY, minZ).setColor(colorInt);
			vertexConsumer.addVertex(pose, minX, maxY, maxZ).setColor(colorInt);
			vertexConsumer.addVertex(pose, maxX, maxY, maxZ).setColor(colorInt);
			vertexConsumer.addVertex(pose, maxX, maxY, minZ).setColor(colorInt);

			// North (-Z)
			vertexConsumer.addVertex(pose, minX, minY, minZ).setColor(colorInt);
			vertexConsumer.addVertex(pose, minX, maxY, minZ).setColor(colorInt);
			vertexConsumer.addVertex(pose, maxX, maxY, minZ).setColor(colorInt);
			vertexConsumer.addVertex(pose, maxX, minY, minZ).setColor(colorInt);

			// South (+Z)
			vertexConsumer.addVertex(pose, minX, minY, maxZ).setColor(colorInt);
			vertexConsumer.addVertex(pose, maxX, minY, maxZ).setColor(colorInt);
			vertexConsumer.addVertex(pose, maxX, maxY, maxZ).setColor(colorInt);
			vertexConsumer.addVertex(pose, minX, maxY, maxZ).setColor(colorInt);

			// West (-X)
			vertexConsumer.addVertex(pose, minX, minY, minZ).setColor(colorInt);
			vertexConsumer.addVertex(pose, minX, minY, maxZ).setColor(colorInt);
			vertexConsumer.addVertex(pose, minX, maxY, maxZ).setColor(colorInt);
			vertexConsumer.addVertex(pose, minX, maxY, minZ).setColor(colorInt);

			// East (+X)
			vertexConsumer.addVertex(pose, maxX, minY, minZ).setColor(colorInt);
			vertexConsumer.addVertex(pose, maxX, maxY, minZ).setColor(colorInt);
			vertexConsumer.addVertex(pose, maxX, maxY, maxZ).setColor(colorInt);
			vertexConsumer.addVertex(pose, maxX, minY, maxZ).setColor(colorInt);
        }
    }

	private static void renderBoxGuides(AABB box, Color color, float alpha, float lineWidth) {
		Vec3 center = box.getCenter();
		double radius = box.maxX - center.x;

		// Convert requested line width into a world-space thickness.
		double t = Math.max(0.01, lineWidth * 0.02);
		double halfT = t / 2.0;

		double cx = center.x;
		double cy = center.y;
		double cz = center.z;

		// +X guide
		renderFilledBox(
				new AABB(cx + 1.0, cy - halfT, cz - halfT,
						cx + radius, cy + halfT, cz + halfT),
				color, alpha
		);

		// -X guide
		renderFilledBox(
				new AABB(cx - radius, cy - halfT, cz - halfT,
						cx - 1.0, cy + halfT, cz + halfT),
				color, alpha
		);

		// +Y guide
		renderFilledBox(
				new AABB(cx - halfT, cy + 1.0, cz - halfT,
						cx + halfT, cy + radius, cz + halfT),
				color, alpha
		);

		// -Y guide
		renderFilledBox(
				new AABB(cx - halfT, cy - radius, cz - halfT,
						cx + halfT, cy - 1.0, cz + halfT),
				color, alpha
		);

		// +Z guide
		renderFilledBox(
				new AABB(cx - halfT, cy - halfT, cz + 1.0,
						cx + halfT, cy + halfT, cz + radius),
				color, alpha
		);

		// -Z guide
		renderFilledBox(
				new AABB(cx - halfT, cy - halfT, cz - radius,
						cx + halfT, cy + halfT, cz - 1.0),
				color, alpha
		);
	}

	/**
	 * middle center of text is at `pos` before moving it down the screen by `offset`
	 */
	private static void renderTextFacingCamera(Component text, Vec3 pos, float offset, float scale, int colorAlphaHex) {
        // Create a new pose stack for proper 3D positioning
        PoseStack poseStack = new PoseStack();

        // Translate to the world position
        poseStack.translate(pos.x, pos.y, pos.z);

        // Make text face the camera
        poseStack.mulPose(mc.gameRenderer.getMainCamera().rotation());

        // Calculate scale based on distance
        scale *= 0.005f * (mc.player.position().distanceTo(pos) / 2.4);
        scale = Math.clamp(scale, 0.015f, 0.15f);

        // Apply scaling (negative Y to flip text right-side up)
        poseStack.scale(scale, -scale, scale);

        // Calculate text positioning
        float w = mc.font.width(text);
        float x = -w / 2f;
        float y = -(.5f - offset) * (mc.font.lineHeight + 2); // +2 for background padding, -1 for default line spacing
        boolean shadow = false;

        // Get the final transformation matrix
        Matrix4f matrix = poseStack.last().pose();

        // Background settings - make it more transparent to see text better
        float bgOpacity = Minecraft.getInstance().options.getBackgroundOpacity(0.25f);
        int bgColor = (int) (bgOpacity * 255.0f) << 24;

        // Ensure text has full alpha if not already set
        if ((colorAlphaHex & 0xFF000000) == 0) {
            colorAlphaHex |= 0xFF000000; // Add full alpha if missing
        }

        // Use immediate mode rendering with proper depth handling
        try (RenderBufferGuard guard = RenderBufferGuard.open(false, true, false)) {
            mc.font.drawInBatch(text, x, y, colorAlphaHex, shadow, matrix, guard.bufferSource, Font.DisplayMode.NORMAL, bgColor, 15728880);
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
