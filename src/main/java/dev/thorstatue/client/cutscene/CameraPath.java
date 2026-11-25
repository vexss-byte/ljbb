package dev.thorstatue.client.cutscene;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

/**
 * Cinematic camera path. Keys are authored in "statue space" (x = screen-right when looking at the
 * statue's front, y = up, z = distance in front of the statue) so the cutscene works for any block facing.
 * Position and look-target are both interpolated with a C1-continuous non-uniform Catmull-Rom (Hermite)
 * spline, giving the smooth, constantly-moving feel of the reference clip. Yaw/pitch are derived from
 * (target - position) so there is no angle wrapping.
 */
public final class CameraPath {
    public record Pose(Vec3 pos, float yaw, float pitch) {}

    private final double[] times;
    private final Vec3[] pos;
    private final Vec3[] tgt;

    private CameraPath(double[] t, Vec3[] p, Vec3[] g) { times = t; pos = p; tgt = g; }

    /** Statue-space -> world. */
    public static Vec3 toWorld(Vec3 base, Direction facing, double x, double y, double z) {
        double fx = facing.getStepX(), fz = facing.getStepZ();
        double rx = fz, rz = -fx; // viewer's right when looking from the front toward the statue
        return new Vec3(base.x + fx * z + rx * x, base.y + y, base.z + fz * z + rz * x);
    }

    public static CameraPath build(Vec3 base, Direction f, Vec3 startEye, Vec3 startTarget, double px, double pz) {
        double D = Math.max(pz, 3.2);
        double[][] k = {
            // t,   cam x,y,z (statue space)       target x,y,z
            {0.70, px * 0.5, 1.75, D - 0.6,        0.0, 3.0, 0.0},
            {1.40, px * 0.3, 1.60, D - 1.3,        0.0, 2.7, 0.0},
            {1.90, px + 1.0, 0.95, pz + 1.1,       px - 0.2, 0.45, pz - 0.5},
            {2.50, -1.6, 1.90, D + 0.2,            0.3, 3.1, 0.2},
            {3.10, -4.2, 3.40, D + 3.5,            0.0, 2.6, 0.0},
            {3.60, -8.5, 9.00, D + 7.5,            0.0, 1.5, 0.0},
            {4.00, -11.0, 15.5, D + 9.0,           0.5, 0.0, 0.0},
            {4.60, -3.5, 1.30, D + 4.5,            0.0, 4.2, 0.0},
            {5.40, -3.0, 1.20, D + 3.8,            -0.3, 4.6, 0.0},
            {6.20, -2.2, 1.00, D + 2.8,            -0.8, 4.9, -0.5},
            {7.00, -1.0, 0.90, D + 2.3,            -1.2, 5.3, -1.2},
            {8.00, -0.7, 0.90, D + 2.1,            -1.3, 5.4, -1.4},
        };
        int n = k.length + 1;
        double[] t = new double[n];
        Vec3[] p = new Vec3[n];
        Vec3[] g = new Vec3[n];
        t[0] = 0; p[0] = startEye; g[0] = startTarget;
        for (int i = 0; i < k.length; i++) {
            t[i + 1] = k[i][0];
            p[i + 1] = toWorld(base, f, k[i][1], k[i][2], k[i][3]);
            g[i + 1] = toWorld(base, f, k[i][4], k[i][5], k[i][6]);
        }
        return new CameraPath(t, p, g);
    }

    private static Vec3 hermite(Vec3 p0, Vec3 p1, Vec3 m0, Vec3 m1, double u) {
        double u2 = u * u, u3 = u2 * u;
        double h00 = 2 * u3 - 3 * u2 + 1, h10 = u3 - 2 * u2 + u, h01 = -2 * u3 + 3 * u2, h11 = u3 - u2;
        return p0.scale(h00).add(m0.scale(h10)).add(p1.scale(h01)).add(m1.scale(h11));
    }

    private Vec3 tangent(Vec3[] a, int i) {
        int lo = Math.max(i - 1, 0), hi = Math.min(i + 1, a.length - 1);
        if (hi == lo) return Vec3.ZERO;
        return a[hi].subtract(a[lo]).scale(1.0 / (times[hi] - times[lo]));
    }

    private Vec3 eval(Vec3[] a, double t) {
        if (t <= times[0]) return a[0];
        if (t >= times[times.length - 1]) return a[a.length - 1];
        int i = 0;
        while (t > times[i + 1]) i++;
        double dt = times[i + 1] - times[i];
        double u = (t - times[i]) / dt;
        return hermite(a[i], a[i + 1], tangent(a, i).scale(dt), tangent(a, i + 1).scale(dt), u);
    }

    public Pose sample(double t, double shakeSeed) {
        t = Math.min(t, times[times.length - 1] + 3600);
        Vec3 p = eval(pos, t), g = eval(tgt, t);
        // gentle handheld sway so the held end shot never looks frozen
        double s = 0.012;
        p = p.add(Math.sin(t * 1.3 + shakeSeed) * s, Math.sin(t * 1.7 + 1.1) * s, Math.cos(t * 1.1) * s);
        Vec3 d = g.subtract(p);
        double h = Math.sqrt(d.x * d.x + d.z * d.z);
        float yaw = (float) (Math.toDegrees(Math.atan2(-d.x, d.z)));
        float pitch = (float) (-Math.toDegrees(Math.atan2(d.y, h)));
        return new Pose(p, yaw, pitch);
    }

    public static double smooth(double a, double b, double x) {
        double u = Math.max(0, Math.min(1, (x - a) / (b - a)));
        return u * u * (3 - 2 * u);
    }
}
