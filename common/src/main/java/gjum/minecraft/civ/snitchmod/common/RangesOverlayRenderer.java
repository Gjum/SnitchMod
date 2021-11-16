package gjum.minecraft.civ.snitchmod.common;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import gjum.minecraft.civ.snitchmod.common.model.Snitch;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.opengl.GL11;

import static gjum.minecraft.civ.snitchmod.common.SnitchMod.getMod;

public class RangesOverlayRenderer {
	private final static Minecraft mc = Minecraft.getInstance();

	public static void renderRangesOverlay(PoseStack matrices, float partialTicks) {
		if (mc.player == null) return;

		final Level level = Minecraft.getInstance().level;
		if (level == null) return;

		RenderSystem.pushMatrix();

		final Camera camera = mc.gameRenderer.getMainCamera();
		final Vec3 camPos = camera.getPosition();

		RenderSystem.multMatrix(matrices.last().pose());
		RenderSystem.translated(-camPos.x(), -camPos.y(), -camPos.z());

		float r = 1;
		float g = 1;
		float b = 0;
		float boxAlpha = 0.3f;
		float lineAlpha = 1;
		float lineWidth = 2;

		for (Snitch snitch : getMod().getNearbySnitches(mc.player.blockPosition())) {
			final AABB range = snitch.getAABB();
			// make sure box isn't obstructed by blocks
			final boolean playerInRange = range.contains(mc.player.position());
			final AABB box = playerInRange ? range.inflate(-.01) : range.inflate(.01);

			DebugRenderer.renderFilledBox(box, r, g, b, boxAlpha);

			renderBoxOutline(box, r, g, b, lineAlpha, lineWidth);
		}

		RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
		RenderSystem.popMatrix();
	}

	static void renderBoxOutline(AABB box, float r, float g, float b, float a, float lineWidth) {
		RenderSystem.lineWidth(lineWidth);
		Tesselator tesselator = Tesselator.getInstance();
		BufferBuilder bufferBuilder = tesselator.getBuilder();
		bufferBuilder.begin(GL11.GL_LINES, DefaultVertexFormat.POSITION_COLOR);

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
