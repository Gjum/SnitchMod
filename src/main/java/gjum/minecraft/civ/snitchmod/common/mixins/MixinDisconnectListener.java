package gjum.minecraft.civ.snitchmod.common.mixins;

import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static gjum.minecraft.civ.snitchmod.common.SnitchMod.getMod;

@Mixin(Connection.class)
public abstract class MixinDisconnectListener {
	@Inject(method = "disconnect", at = @At("HEAD"))
	protected void onDisconnect(Component component, CallbackInfo ci) {
		Minecraft.getInstance().execute(() -> {
			try {
				getMod().handleDisconnectedFromServer();
			} catch (Throwable e) {
				e.printStackTrace();
			}
		});
	}
}