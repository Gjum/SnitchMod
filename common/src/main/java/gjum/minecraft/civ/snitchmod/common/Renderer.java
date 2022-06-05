package gjum.minecraft.civ.snitchmod.common;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import static gjum.minecraft.civ.snitchmod.common.SnitchMod.getMod;

public class Renderer {
	private final static Minecraft mc = Minecraft.getInstance();

	public static void renderOverlays(PoseStack poseStackArg, float partialTicks) {
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
		var poseStack = new PoseStack();

		long now = System.currentTimeMillis();

		if (getMod().rangeOverlayVisible) {
			float boxAlpha = 0.2f;
			float lineAlpha = 1;
			float lineWidth = 2;
			int blockHlDist = 64;
			int fieldDist = 260;

			getMod().streamNearbySnitches(mc.player.blockPosition(), fieldDist).limit(100).forEach(snitch -> {
				final AABB range = snitch.getAABB();
				// make sure box isn't obstructed by blocks
				final boolean playerInRange = range.contains(mc.player.position());
				final AABB rangeBox = playerInRange ? range.inflate(-.01) : range.inflate(.01);
				float r = 1;
				float g = 1;
				float b = 0;
				if (snitch.getDormantTs() != 0 && snitch.getDormantTs() < now) {
					g = .5f;
				}
				if (snitch.getCullTs() != 0 && snitch.getCullTs() < now) {
					g = 0;
				}

				RenderSystem.enableDepthTest();
				DebugRenderer.renderFilledBox(rangeBox, r, g, b, boxAlpha);

				renderBoxOutline(rangeBox, r, g, b, lineAlpha, lineWidth);

				if (snitch.distSqr(mc.player.blockPosition()) < blockHlDist * blockHlDist) {
					RenderSystem.disableDepthTest();
					final AABB blockBox = new AABB(snitch).inflate(.01);
					renderBoxOutline(blockBox, r, g, b, lineAlpha, lineWidth);
//					renderTextFacingCamera(poseStack, text, snitch, -.5f, 1); // XXX
				}
			});
		}

		if (getMod().placementHelperVisible) {
			// light blue
			float r = 0;
			float g = 0.7f;
			float b = 1;
			float alpha = 0.2f;
			int placeHelperDist = 30;
			getMod().streamNearbySnitches(mc.player.blockPosition(), placeHelperDist).limit(10).forEach(snitch -> {
				final boolean playerInRange = snitch.getAABB().contains(mc.player.position());
				if (playerInRange) return; // only render helper for snitches the player isn't inside of
				final AABB helperBox = new AABB(snitch.getBlockPos()).inflate(22.5);
				DebugRenderer.renderFilledBox(helperBox, r, g, b, alpha);
			});
		}

		RenderSystem.enableTexture();
		RenderSystem.enableDepthTest();
		RenderSystem.depthMask(true);
		RenderSystem.enableCull();
	}

	static void renderBoxOutline(AABB box, float r, float g, float b, float a, float lineWidth) {
		RenderSystem.disableTexture();
		RenderSystem.disableDepthTest();
		RenderSystem.depthMask(false);

		RenderSystem.lineWidth(lineWidth);
		Tesselator tesselator = Tesselator.getInstance();
		BufferBuilder bufferBuilder = tesselator.getBuilder();
		bufferBuilder.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR);

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
		RenderSystem.lineWidth(1.0F);
	}
}
