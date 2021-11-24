package gjum.minecraft.civ.snitchmod.fabric;

import gjum.minecraft.civ.snitchmod.common.SnitchMod;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.KeyMapping;

public class FabricSnitchMod extends SnitchMod implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		init();
		ClientTickEvents.START_CLIENT_TICK.register(client -> {
			try {
				handleTick();
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		WorldRenderEvents.AFTER_TRANSLUCENT.register(((context) -> {
			try {
				handleRenderBlockOverlay(context.matrixStack(), context.tickDelta());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}));
	}

	@Override
	public void registerKeyBinding(KeyMapping mapping) {
		KeyBindingHelper.registerKeyBinding(mapping);
	}
}
