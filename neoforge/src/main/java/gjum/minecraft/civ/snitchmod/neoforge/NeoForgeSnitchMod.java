package gjum.minecraft.civ.snitchmod.neoforge;

import gjum.minecraft.civ.snitchmod.common.JalistAutoPaginator;
import gjum.minecraft.civ.snitchmod.common.SnitchMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.lifecycle.ClientStartedEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

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
        event.register(jalistAutoKey);
    }

    @SubscribeEvent
    public void onAfterTripwire(RenderLevelStageEvent.AfterTripwireBlocks event) {
        handleRenderBlockOverlay(event.getPoseStack());
    }
    
    @SubscribeEvent
    public void onClientTick(ClientTickEvent.Pre event) {
        handleTick();

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
    }

    @SubscribeEvent
    public void onClientLoaded(ClientStartedEvent event) {
        mc = Minecraft.getInstance();
    }
}
