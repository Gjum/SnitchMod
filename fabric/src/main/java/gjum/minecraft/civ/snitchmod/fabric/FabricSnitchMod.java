package gjum.minecraft.civ.snitchmod.fabric;

import gjum.minecraft.civ.snitchmod.common.SnitchMod;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;

public class FabricSnitchMod extends SnitchMod implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		KeyBindingHelper.registerKeyBinding(openGuiKey);
		KeyBindingHelper.registerKeyBinding(toggleOverlayKey);
		KeyBindingHelper.registerKeyBinding(togglePlacementKey);
		KeyBindingHelper.registerKeyBinding(previewSnitchFieldKey);
		KeyBindingHelper.registerKeyBinding(toggleSnitchGoneStatusKey);

		ClientTickEvents.START_CLIENT_TICK.register(client -> {
			try {
				handleTick();
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		WorldRenderEvents.LAST.register(((context) -> {
			try {
				handleRenderBlockOverlay(context.matrixStack().last().pose());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}));
	}
}
