package gjum.minecraft.civ.snitchmod.forge;

import gjum.minecraft.civ.snitchmod.common.SnitchMod;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod("snitchmod")
public class ForgeSnitchMod extends SnitchMod {
	public ForgeSnitchMod(FMLJavaModLoadingContext fmlJavaModLoadingContext) {
		MinecraftForge.EVENT_BUS.register(this);
	}

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(openGuiKey);
        event.register(toggleOverlayKey);
        event.register(togglePlacementKey);
        event.register(previewSnitchFieldKey);
        event.register(toggleSnitchGoneStatusKey);
    }

	@SubscribeEvent
	public void onClientTick(TickEvent.ClientTickEvent event) {
		if (event.phase == TickEvent.Phase.START) {
			handleTick();
		}
	}
}
