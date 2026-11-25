package dev.thorstatue.mixin.client;

import dev.thorstatue.client.cutscene.CutsceneController;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Mouse look is ignored during the cutscene so the viewpoint can't be moved. */
@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
    @Inject(method = "turnPlayer", at = @At("HEAD"), cancellable = true)
    private void thorstatue$lockLook(double mousea, CallbackInfo ci) {
        if (CutsceneController.isActive()) ci.cancel();
    }
}
