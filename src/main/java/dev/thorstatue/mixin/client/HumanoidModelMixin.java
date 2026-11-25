package dev.thorstatue.mixin.client;

import dev.thorstatue.client.cutscene.CutsceneController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Kneeling pose for the local player during the cutscene: right leg forward (knee up), left leg folded back
 * (knee on the ground), body lowered, head bowed, hands clasped in front. Vanilla legs are single boxes, so the
 * "knee" is approximated by swinging the thighs.
 */
@Mixin(HumanoidModel.class)
public abstract class HumanoidModelMixin {
    @Shadow @Final public ModelPart head;
    @Shadow @Final public ModelPart body;
    @Shadow @Final public ModelPart rightArm;
    @Shadow @Final public ModelPart leftArm;
    @Shadow @Final public ModelPart rightLeg;
    @Shadow @Final public ModelPart leftLeg;

    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V", at = @At("TAIL"))
    private void thorstatue$kneel(HumanoidRenderState state, CallbackInfo ci) {
        if (!CutsceneController.isActive() || !(state instanceof AvatarRenderState avatar)) return;
        var p = Minecraft.getInstance().player;
        if (p == null || avatar.id != p.getId()) return; // local player only
        float k = CutsceneController.kneel();
        if (k <= 0f) return;
        float drop = 8.0f * k;
        head.y += drop; body.y += drop; rightArm.y += drop; leftArm.y += drop;
        rightLeg.y += drop - 1.0f; leftLeg.y += drop - 1.0f;
        head.xRot = head.xRot * (1 - k) + 0.55f * k;
        body.xRot = 0.12f * k;
        rightLeg.xRot = -1.45f * k; rightLeg.yRot = 0.08f * k;
        leftLeg.xRot = 1.35f * k;   leftLeg.yRot = -0.05f * k;
        rightArm.xRot = -0.95f * k; leftArm.xRot = -0.95f * k;
        rightArm.zRot = 0.35f * k;  leftArm.zRot = -0.35f * k;
    }
}
