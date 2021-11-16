package gjum.minecraft.civ.snitchmod.forge;

import gjum.minecraft.civ.snitchmod.common.SnitchMod;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod("snitchmod")
public class ForgeSnitchMod extends SnitchMod {
	public ForgeSnitchMod() {
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::clientSetup);
		MinecraftForge.EVENT_BUS.register(this);
	}

	public void clientSetup(FMLClientSetupEvent event) {
		init();
	}

	@SubscribeEvent
	public void onRenderWorldLast(RenderWorldLastEvent event) {
		try {
			handleRenderBlockOverlay(event.getMatrixStack(), event.getPartialTicks());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@SubscribeEvent
	public void onClientTick(TickEvent.ClientTickEvent event) {
		if (event.phase == TickEvent.Phase.START) {
			handleTick();
		}
	}

	@Override
	public void registerKeyBinding(KeyMapping mapping) {
		ClientRegistry.registerKeyBinding(mapping);
	}
}
