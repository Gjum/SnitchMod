package gjum.minecraft.civ.snitchmod.fabric;

import gjum.minecraft.civ.snitchmod.common.SnitchMod;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;

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
            handleTick();
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
