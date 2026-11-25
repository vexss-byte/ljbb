package dev.thorstatue.client.cutscene;

import dev.thorstatue.block.ThorStatueBlockEntity;
import dev.thorstatue.client.ContractScreen;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

/** Client-side state machine for the 8 second contract cutscene. */
public final class CutsceneController {
    public static final double LENGTH = 8.0;
    public static final double PANEL_AT = 5.6;

    private static boolean active;
    private static long startNs;
    private static BlockPos statuePos;
    private static Direction facing;
    private static CameraPath path;
    private static boolean panelOpened;
    private static CameraType savedCameraType;
    private static boolean savedHideGui;
    private static Vec3 lastPos = Vec3.ZERO;
    private static float lastYaw, lastPitch;

    private CutsceneController() {}

    public static boolean isActive() { return active; }
    public static BlockPos statuePos() { return statuePos; }
    public static Direction facing() { return facing; }
    public static Vec3 statueBase() { return new Vec3(statuePos.getX() + 0.5, statuePos.getY(), statuePos.getZ() + 0.5); }
    public static Vec3 cameraPos() { return lastPos; }
    public static float cameraYaw() { return lastYaw; }
    public static float cameraPitch() { return lastPitch; }

    /** Seconds since the cutscene began (real time; the panel stays up after LENGTH). */
    public static double time() { return active ? (System.nanoTime() - startNs) / 1.0e9 : 0.0; }

    /** 0..1 how deep the player is kneeling. */
    public static float kneel() {
        return active ? (float) CameraPath.smooth(0.9, 1.7, time()) : 0f;
    }

    public static void begin(BlockPos pos, Direction facing) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (active || p == null) return;
        statuePos = pos;
        CutsceneController.facing = facing;
        Vec3 base = statueBase();

        // face the statue
        double dx = base.x - p.getX(), dz = base.z - p.getZ();
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        p.setYRot(yaw); p.setYHeadRot(yaw); p.setXRot(0f);
        p.setYBodyRot(yaw);

        Vec3 eye = p.getEyePosition();
        Vec3 look = eye.add(p.getLookAngle().scale(10));
        // player in statue space
        double fx = facing.getStepX(), fz = facing.getStepZ();
        double rx = fz, rz = -fx;
        double ox = p.getX() - base.x, oz = p.getZ() - base.z;
        double pz = ox * fx + oz * fz;
        double px = ox * rx + oz * rz;
        path = CameraPath.build(base, facing, eye, look, px, pz);

        savedCameraType = mc.options.getCameraType();
        savedHideGui = mc.gui.hud.isHidden();
        mc.options.setCameraType(CameraType.THIRD_PERSON_BACK); // so the kneeling player is visible
        startNs = System.nanoTime();
        panelOpened = false;
        active = true;
        ThorStatueBlockEntity.powerUp = false;
    }

    public static void end() {
        if (!active) return;
        Minecraft mc = Minecraft.getInstance();
        active = false;
        ThorStatueBlockEntity.powerUp = false;
        if (savedCameraType != null) mc.options.setCameraType(savedCameraType);
        setHud(mc, savedHideGui);
    }

    /** 26.3 only exposes Hud.toggle(), so flip it only when the state differs. */
    private static void setHud(Minecraft mc, boolean hidden) {
        if (mc.gui.hud.isHidden() != hidden) mc.gui.hud.toggle();
    }

    /** Called every client tick. */
    public static void tick(Minecraft mc) {
        if (!active) return;
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null) { end(); return; }
        // freeze the player in place while kneeling
        p.setDeltaMovement(0, Math.min(0, p.getDeltaMovement().y), 0);
        p.setSprinting(false);
        double t = time();
        ThorStatueBlockEntity.powerUp = t >= 2.4;
        if (t >= 1.4) setHud(mc, true); // the clip drops the hotbar once the camera leaves the player
        if (!panelOpened && t >= PANEL_AT) {
            panelOpened = true;
            setHud(mc, false);
            mc.gui.setScreen(new ContractScreen());
        }
    }

    /** Called from the Camera mixin each frame; returns the pose to force onto the camera. */
    public static CameraPath.Pose sample() {
        CameraPath.Pose pose = path.sample(Math.min(time(), LENGTH + 600), 0.37);
        lastPos = pose.pos(); lastYaw = pose.yaw(); lastPitch = pose.pitch();
        return pose;
    }
}
