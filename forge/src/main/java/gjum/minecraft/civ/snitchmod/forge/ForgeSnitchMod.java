package gjum.minecraft.civ.snitchmod.forge;

import gjum.minecraft.civ.snitchmod.common.SnitchMod;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod("snitchmod")
public class ForgeSnitchMod extends SnitchMod {
	public ForgeSnitchMod() {
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::registerKeyMappings);
		MinecraftForge.EVENT_BUS.register(this);
	}

	public void registerKeyMappings(RegisterKeyMappingsEvent event) {
		event.register(openGuiKey);
		event.register(toggleOverlayKey);
		event.register(togglePlacementKey);
		event.register(previewSnitchFieldKey);
		event.register(deleteSnitchKey);
	}

	@SubscribeEvent
	public void onRenderLevelLast(RenderLevelStageEvent event) {
		if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) { // Forge broke the PoseStack in AFTER_TRANSLUCENT_BLOCKS (???)
			try {
				handleRenderBlockOverlay(event.getPoseStack()); // Ignore, if Forge removes this it can be replaced with a mixin probably
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@SubscribeEvent
	public void onClientTick(TickEvent.ClientTickEvent event) {
		if (event.phase == TickEvent.Phase.START) {
			handleTick();
		}
	}
}
