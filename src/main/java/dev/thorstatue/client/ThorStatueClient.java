package dev.thorstatue.client;

import dev.thorstatue.ThorStatueMod;
import dev.thorstatue.client.cutscene.CutsceneController;
import dev.thorstatue.net.StartCutscenePayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.world.InteractionResult;

public class ThorStatueClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        dev.thorstatue.client.render.FxRenderer.init(); // registers shader pipelines before resources load
        BlockEntityRenderers.register(ThorStatueMod.THOR_STATUE_BE, ThorStatueRenderer::new);

        ClientPlayNetworking.registerGlobalReceiver(StartCutscenePayload.TYPE, (payload, context) ->
            context.client().execute(() -> CutsceneController.begin(payload.pos(), payload.direction())));

        ClientTickEvents.END_CLIENT_TICK.register(CutsceneController::tick);

        // no block interaction (or re-triggering) while the cutscene owns the player
        UseBlockCallback.EVENT.register((player, level, hand, hit) ->
            CutsceneController.isActive() && level.isClientSide() ? InteractionResult.FAIL : InteractionResult.PASS);
    }
}
