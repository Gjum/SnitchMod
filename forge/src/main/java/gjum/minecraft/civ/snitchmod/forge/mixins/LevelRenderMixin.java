package gjum.minecraft.civ.snitchmod.forge.mixins;

import com.mojang.blaze3d.vertex.PoseStack;
import gjum.minecraft.civ.snitchmod.forge.ForgeSnitchMod;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRenderMixin { // REMINDER: Forge sucks
    @Inject(method = "renderSectionLayer", at = @At("RETURN"))
    private void renderScarpetThingsLate(RenderType arg, double d, double e, double f, Matrix4f matrix4f, Matrix4f matrix4f2, CallbackInfo ci) {
        if (arg == RenderType.translucent()) {
            ForgeSnitchMod.getMod().handleRenderBlockOverlay(new PoseStack().last().pose());
        }
    }
}
