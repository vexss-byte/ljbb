package dev.thorstatue.block;

import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import dev.thorstatue.ThorStatueMod;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class ThorStatueBlockEntity extends BlockEntity implements GeoBlockEntity {
    /** Set on the client by the cutscene; makes every statue play the one-shot power_up animation. */
    public static volatile boolean powerUp = false;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public ThorStatueBlockEntity(BlockPos pos, BlockState state) {
        super(ThorStatueMod.THOR_STATUE_BE, pos, state);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // "idle" loops forever; the cutscene client code triggers "power_up" by name (see CutsceneController)
        controllers.add(new AnimationController<ThorStatueBlockEntity>("main",
            state -> powerUp
                ? state.setAndContinue(RawAnimation.begin().thenPlayAndHold("animation.thor_statue.power_up"))
                : state.setAndContinue(RawAnimation.begin().thenLoop("animation.thor_statue.idle"))));
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
