package dev.thorstatue.mixin.client;

import dev.thorstatue.client.cutscene.CameraPath;
import dev.thorstatue.client.cutscene.CutsceneController;
import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Overrides the camera pose every frame while the cutscene runs (right after the vanilla entity alignment). */
@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow protected abstract void setRotation(float yRot, float xRot);
    @Shadow protected abstract void setPosition(Vec3 position);

    @Inject(method = "alignWithEntity", at = @At("TAIL"))
    private void thorstatue$cutscenePose(float partialTicks, CallbackInfo ci) {
        if (!CutsceneController.isActive()) return;
        CameraPath.Pose pose = CutsceneController.sample();
        this.setRotation(pose.yaw(), pose.pitch());
        this.setPosition(pose.pos());
    }
}
