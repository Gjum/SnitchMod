package gjum.minecraft.civ.snitchmod.fabric;

import gjum.minecraft.civ.snitchmod.common.JalistAutoPaginator;
import gjum.minecraft.civ.snitchmod.common.SnitchMod;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.lwjgl.glfw.GLFW;

public class FabricSnitchMod extends SnitchMod implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		KeyBindingHelper.registerKeyBinding(openGuiKey);
		KeyBindingHelper.registerKeyBinding(toggleOverlayKey);
		KeyBindingHelper.registerKeyBinding(togglePlacementKey);
		KeyBindingHelper.registerKeyBinding(previewSnitchFieldKey);
		KeyBindingHelper.registerKeyBinding(toggleSnitchGoneStatusKey);
		KeyBindingHelper.registerKeyBinding(jalistAutoKey);

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
		WorldRenderEvents.BEFORE_TRANSLUCENT.register(((context) -> {
			try {
				handleRenderBlockOverlay(context.matrices());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}));
	}
}
