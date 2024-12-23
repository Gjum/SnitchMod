package gjum.minecraft.civ.snitchmod.neoforge;

import gjum.minecraft.civ.snitchmod.common.SnitchMod;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod("snitchmod")
public class NeoForgeSnitchMod extends SnitchMod {
    public NeoForgeSnitchMod(IEventBus bus) {
        NeoForge.EVENT_BUS.register(this);
        bus.addListener(this::registerKeyMappings);
    }

    public void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(openGuiKey);
        event.register(toggleOverlayKey);
        event.register(togglePlacementKey);
        event.register(previewSnitchFieldKey);
        event.register(toggleSnitchGoneStatusKey);
    }

    @SubscribeEvent
    public void onRenderLevelLast(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_TRIPWIRE_BLOCKS) {
            handleRenderBlockOverlay(event.getPoseStack().last().pose());
        }
    }
    
    @SubscribeEvent
    public void onClientTick(ClientTickEvent.Pre event) {
        handleTick();
    }
}
