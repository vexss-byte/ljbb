package dev.thorstatue.mixin.client;

import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.framegraph.FramePass;
import dev.thorstatue.client.cutscene.CutsceneController;
import dev.thorstatue.client.render.FxRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Appends the cutscene VFX as a frame-graph pass right after the main world pass. */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    @Shadow @org.spongepowered.asm.mixin.Final private LevelTargetBundle targets;

    @Inject(method = "addMainPass", at = @At("TAIL"))
    private void thorstatue$addFxPass(FrameGraphBuilder frame, FeatureRenderDispatcher.PreparedFrame featureFrame,
                                      GpuBufferSlice terrainFog, ChunkSectionsToRender sections,
                                      boolean consistentDepthRequired, CallbackInfo ci) {
        if (!CutsceneController.isActive()) return;
        FramePass pass = frame.addPass("thorstatue_cutscene");
        this.targets.main = pass.readsAndWrites(this.targets.main);
        pass.executes(() -> FxRenderer.render(this.targets.main.get()));
    }
}
