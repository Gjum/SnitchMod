package gjum.minecraft.civ.snitchmod.fabric;

import gjum.minecraft.civ.snitchmod.common.JalistAutoPaginator;
import gjum.minecraft.civ.snitchmod.common.SnitchMod;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.lwjgl.glfw.GLFW;

public class FabricSnitchMod extends SnitchMod implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		KeyMappingHelper.registerKeyMapping(openGuiKey);
		KeyMappingHelper.registerKeyMapping(toggleOverlayKey);
		KeyMappingHelper.registerKeyMapping(togglePlacementKey);
		KeyMappingHelper.registerKeyMapping(previewSnitchFieldKey);
		KeyMappingHelper.registerKeyMapping(toggleSnitchGoneStatusKey);
		KeyMappingHelper.registerKeyMapping(jalistAutoKey);

		ClientTickEvents.START_CLIENT_TICK.register(client -> {
			try {
				handleTick();
				
				// Check for J key press while in JAList GUI
				var mc = Minecraft.getInstance();
				if (mc.screen instanceof AbstractContainerScreen<?> containerScreen) {
					String title = containerScreen.getTitle().getString();
					if ((title.toLowerCase().contains("snitches") || title.contains("JukeAlert")) 
						&& GLFW.glfwGetKey(mc.getWindow().handle(), GLFW.GLFW_KEY_J) == GLFW.GLFW_PRESS) {
						
						// Prevent spam clicking by checking if auto-paginator is not already active
						if (!JalistAutoPaginator.getInstance().isActive()) {
							System.out.println("[FabricSnitchMod] J key detected in JAList!");
							JalistAutoPaginator.getInstance().startAutoPagination();
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		LevelRenderEvents.END_MAIN.register(((context) -> {
			try {
				handleRenderBlockOverlay(context.poseStack());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}));
	}
}
