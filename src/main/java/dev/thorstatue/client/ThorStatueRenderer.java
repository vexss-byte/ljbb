package dev.thorstatue.client;

import com.geckolib.renderer.GeoBlockRenderer;
import dev.thorstatue.ThorStatueMod;
import dev.thorstatue.block.ThorStatueBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;

/** GeckoLib renderer. Model/animation/texture are resolved from the block entity type id (thorstatue:thor_statue). */
public class ThorStatueRenderer extends GeoBlockRenderer<ThorStatueBlockEntity, BlockEntityRenderState> {
    public ThorStatueRenderer(BlockEntityRendererProvider.Context ctx) {
        super(ctx, ThorStatueMod.THOR_STATUE_BE);
    }
}
