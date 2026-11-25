package dev.thorstatue.client.render;

import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import dev.thorstatue.ThorStatueMod;
import dev.thorstatue.client.cutscene.CameraPath;
import dev.thorstatue.client.cutscene.CutsceneController;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Random;

/**
 * All cutscene VFX, drawn as one extra frame-graph pass after the main world pass:
 *   sky tint + star dome, curved dark cloud sea ("the world gets covered"), ground fog puffs,
 *   ground lightning, and the 3D constellation (nodes, lines, silhouette).
 * Everything is shaded by assets/thorstatue/shaders/core/cutscene_fx.{vsh,fsh}; the vertex colour red
 * channel selects the effect mode, UV carries per-effect coordinates.
 */
public final class FxRenderer {
    // modes (vertex red channel, 0..255)
    private static final int M_TINT = 0, M_DOME = 1, M_SEA = 2, M_PUFF = 3, M_LINE = 4, M_STAR = 5, M_BOLT = 6, M_FILL = 7;

    public static final RenderPipeline FX_ALPHA = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
        .withLocation(ThorStatueMod.id("pipeline/cutscene_fx_alpha"))
        .withVertexShader(ThorStatueMod.id("core/cutscene_fx"))
        .withFragmentShader(ThorStatueMod.id("core/cutscene_fx"))
        .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
        .withPrimitiveTopology(PrimitiveTopology.QUADS)
        .withCull(false)
        .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
        .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
        .build());

    public static final RenderPipeline FX_ADD = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
        .withLocation(ThorStatueMod.id("pipeline/cutscene_fx_add"))
        .withVertexShader(ThorStatueMod.id("core/cutscene_fx"))
        .withFragmentShader(ThorStatueMod.id("core/cutscene_fx"))
        .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
        .withPrimitiveTopology(PrimitiveTopology.QUADS)
        .withCull(false)
        .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
        .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
        .build());

    private FxRenderer() {}

    /**
     * Touching this class registers the pipelines. It MUST run during client init, before the first resource
     * reload: only registered pipelines are compiled with the mod's shader files; anything else falls back to a
     * vanilla-only cache and fails with "Failed to find or load pipeline".
     */
    public static void init() {}

    // ---------------------------------------------------------------- mesh helpers
    private static final class Mesh {
        final ByteBufferBuilder bytes = new ByteBufferBuilder(1 << 16);
        final BufferBuilder bb = new BufferBuilder(bytes, PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        int quads;
        void v(Vec3 cam, double x, double y, double z, float u, float v, int mode, int g, int b, int a) {
            bb.addVertex((float) (x - cam.x), (float) (y - cam.y), (float) (z - cam.z)).setUv(u, v).setColor(mode, g, b, a);
        }
        void quad(Vec3 cam, double[] p0, double[] p1, double[] p2, double[] p3, float[] uv, int mode, int g, int b, int a) {
            v(cam, p0[0], p0[1], p0[2], uv[0], uv[1], mode, g, b, a);
            v(cam, p1[0], p1[1], p1[2], uv[2], uv[3], mode, g, b, a);
            v(cam, p2[0], p2[1], p2[2], uv[4], uv[5], mode, g, b, a);
            v(cam, p3[0], p3[1], p3[2], uv[6], uv[7], mode, g, b, a);
            quads++;
        }
    }

    private static int c(double v) { return (int) Math.max(0, Math.min(255, Math.round(v * 255))); }

    /** Camera-facing ribbon between two points; uv.y runs -1..1 across the width. */
    private static void ribbon(Mesh m, Vec3 cam, double[] a, double[] b, double w, int mode, int g, int bch, int alpha, float uLen) {
        double dx = b[0] - a[0], dy = b[1] - a[1], dz = b[2] - a[2];
        double mx = (a[0] + b[0]) / 2 - cam.x, my = (a[1] + b[1]) / 2 - cam.y, mz = (a[2] + b[2]) / 2 - cam.z;
        // side = dir x toCamera
        double sx = dy * mz - dz * my, sy = dz * mx - dx * mz, sz = dx * my - dy * mx;
        double l = Math.sqrt(sx * sx + sy * sy + sz * sz);
        if (l < 1e-9) return;
        sx = sx / l * w; sy = sy / l * w; sz = sz / l * w;
        m.quad(cam,
            new double[]{a[0] - sx, a[1] - sy, a[2] - sz}, new double[]{a[0] + sx, a[1] + sy, a[2] + sz},
            new double[]{b[0] + sx, b[1] + sy, b[2] + sz}, new double[]{b[0] - sx, b[1] - sy, b[2] - sz},
            new float[]{0, -1, 0, 1, uLen, 1, uLen, -1}, mode, g, bch, alpha);
    }

    /** Camera-facing square billboard; uv in -1..1. */
    private static void billboard(Mesh m, Vec3 cam, double[] ctr, double[] right, double[] up, double size, int mode, int g, int b, int a) {
        double rx = right[0] * size, ry = right[1] * size, rz = right[2] * size;
        double ux = up[0] * size, uy = up[1] * size, uz = up[2] * size;
        m.quad(cam,
            new double[]{ctr[0] - rx - ux, ctr[1] - ry - uy, ctr[2] - rz - uz}, new double[]{ctr[0] + rx - ux, ctr[1] + ry - uy, ctr[2] + rz - uz},
            new double[]{ctr[0] + rx + ux, ctr[1] + ry + uy, ctr[2] + rz + uz}, new double[]{ctr[0] - rx + ux, ctr[1] - ry + uy, ctr[2] - rz + uz},
            new float[]{-1, -1, 1, -1, 1, 1, -1, 1}, mode, g, b, a);
    }

    // ---------------------------------------------------------------- scene building
    public static void render(RenderTarget target) {
        if (!CutsceneController.isActive()) return;
        double t = CutsceneController.time();
        Vec3 cam = CutsceneController.cameraPos();
        Vec3 baseV = CutsceneController.statueBase();
        double[] base = {baseV.x, baseV.y, baseV.z};
        Direction face = CutsceneController.facing();
        double fx = face.getStepX(), fz = face.getStepZ();

        // camera basis (Minecraft yaw/pitch convention)
        double yaw = Math.toRadians(CutsceneController.cameraYaw()), pit = Math.toRadians(CutsceneController.cameraPitch());
        double[] fwd = {-Math.sin(yaw) * Math.cos(pit), -Math.sin(pit), Math.cos(yaw) * Math.cos(pit)};
        double[] right = {-Math.cos(yaw), 0, -Math.sin(yaw)};
        double[] up = {right[1] * fwd[2] - right[2] * fwd[1], right[2] * fwd[0] - right[0] * fwd[2], right[0] * fwd[1] - right[1] * fwd[0]};

        // progress curves taken from the reference clip
        double cover = CameraPath.smooth(2.3, 4.3, t);          // world gets covered (sky + cloud sea)
        double fogUp = CameraPath.smooth(2.6, 4.8, t);          // ground fog
        double stars = CameraPath.smooth(3.4, 5.0, t);
        double constAlpha = CameraPath.smooth(4.4, 6.4, t);
        double silhouette = CameraPath.smooth(5.0, 6.8, t);

        Mesh alpha = new Mesh();
        Mesh add = new Mesh();
        Random rnd = new Random(1337);

        // 1) sky tint: inside-out cube hugging the camera tints the whole world dark blue
        if (cover > 0.001) {
            double r = 0.6;
            double[][] cube = {
                {-r,-r,-r},{r,-r,-r},{r,r,-r},{-r,r,-r},{-r,-r,r},{r,-r,r},{r,r,r},{-r,r,r}};
            int[][] faces = {{0,1,2,3},{5,4,7,6},{4,0,3,7},{1,5,6,2},{3,2,6,7},{4,5,1,0}};
            for (int[] f : faces) {
                double[][] p = new double[4][];
                for (int i = 0; i < 4; i++) p[i] = new double[]{cam.x + cube[f[i]][0], cam.y + cube[f[i]][1], cam.z + cube[f[i]][2]};
                alpha.quad(cam, p[0], p[1], p[2], p[3], new float[]{0,0,1,0,1,1,0,1}, M_TINT, c(cover * 0.55), 0, 255);
            }
        }

        // 2) star / cloud dome (a big box around the camera; shader works from world direction)
        if (cover > 0.001) {
            double r = 140;
            double[][] cube = {
                {-r,-r,-r},{r,-r,-r},{r,r,-r},{-r,r,-r},{-r,-r,r},{r,-r,r},{r,r,r},{-r,r,r}};
            int[][] faces = {{0,1,2,3},{5,4,7,6},{4,0,3,7},{1,5,6,2},{3,2,6,7},{4,5,1,0}};
            for (int[] f : faces) {
                double[][] p = new double[4][];
                for (int i = 0; i < 4; i++) p[i] = new double[]{cam.x + cube[f[i]][0], cam.y + cube[f[i]][1], cam.z + cube[f[i]][2]};
                alpha.quad(cam, p[0], p[1], p[2], p[3], new float[]{0,0,1,0,1,1,0,1}, M_DOME, c(stars), 0, c(cover));
            }
        }

        // 3) cloud sea: a huge curved annulus (sphere cap, radius 120) with a hole around the statue island
        if (cover > 0.001) {
            double R = 120, hole = 8.5, outer = 105;
            int rings = 14, seg = 48;
            double cy = base[1] + 0.35 - R;
            for (int ri = 0; ri < rings; ri++) {
                double r0 = hole + (outer - hole) * Math.pow(ri / (double) rings, 1.6);
                double r1 = hole + (outer - hole) * Math.pow((ri + 1) / (double) rings, 1.6);
                for (int si = 0; si < seg; si++) {
                    double a0 = si * Math.PI * 2 / seg, a1 = (si + 1) * Math.PI * 2 / seg;
                    double[][] p = new double[4][];
                    double[] rr = {r0, r1, r1, r0}, aa = {a0, a0, a1, a1};
                    for (int i = 0; i < 4; i++) {
                        double x = Math.cos(aa[i]) * rr[i], z = Math.sin(aa[i]) * rr[i];
                        double y = cy + Math.sqrt(R * R - rr[i] * rr[i]);
                        p[i] = new double[]{base[0] + x, y, base[2] + z};
                    }
                    float[] uv = new float[8];
                    for (int i = 0; i < 4; i++) { uv[i*2] = (float) ((p[i][0]) * 0.03); uv[i*2+1] = (float) ((p[i][2]) * 0.03); }
                    // g = radial position 0 (hole) .. 255 (outer) for soft edges; b = which ring set
                    alpha.quad(cam, p[0], p[1], p[2], p[3], uv, M_SEA, c(ri / (double) rings), 0, c(cover));
                }
            }
        }

        // 4) ground fog puffs hugging the island (soft billboards)
        if (fogUp > 0.001) {
            for (int i = 0; i < 70; i++) {
                double ang = rnd.nextDouble() * Math.PI * 2, rad = 3 + rnd.nextDouble() * 18;
                double y = base[1] + 0.4 + rnd.nextDouble() * 2.6 * (0.4 + 0.6 * rnd.nextDouble());
                double drift = t * (0.15 + 0.1 * rnd.nextDouble());
                double[] ctr = {base[0] + Math.cos(ang + drift * 0.05) * rad, y, base[2] + Math.sin(ang + drift * 0.05) * rad};
                double size = 3.5 + rnd.nextDouble() * 5.0;
                billboard(alpha, cam, ctr, right, up, size, M_PUFF, rnd.nextInt(255), rnd.nextInt(255), c(fogUp * 0.38));
            }
        }

        // 5) ground lightning crawling from the statue base toward the kneeling player (1.3s - 3.2s)
        if (t > 1.3 && t < 3.3) {
            double flick = ((long) (t * 24)) % 3 == 0 ? 0.55 : 1.0;
            double life = CameraPath.smooth(1.3, 1.6, t) * (1 - CameraPath.smooth(2.9, 3.3, t));
            Random br = new Random((long) (t * 24) * 7919L);
            double[] pl = {0, 0, 4};
            for (int bolt = 0; bolt < 7; bolt++) {
                double ang = (bolt - 3) * 0.32 + br.nextDouble() * 0.2;
                double len = 5 + br.nextDouble() * 7;
                double[] prev = {0, 0.05, 0.8};
                int steps = 14;
                for (int s = 1; s <= steps; s++) {
                    double u = s / (double) steps;
                    double lx = Math.sin(ang) * len * u + (br.nextDouble() - 0.5) * 0.9;
                    double lz = 0.8 + Math.cos(ang) * len * u + (br.nextDouble() - 0.5) * 0.9;
                    double[] cur = {lx, 0.05 + br.nextDouble() * 0.12, lz};
                    double[] a = worldLocal(base, fx, fz, prev), b = worldLocal(base, fx, fz, cur);
                    ribbon(add, cam, a, b, 0.11, M_BOLT, 255, 255, c(life * flick * (1 - u * 0.5)), 1f);
                    ribbon(add, cam, a, b, 0.42, M_BOLT, 90, 255, c(life * flick * 0.28 * (1 - u * 0.6)), 1f);
                    prev = cur;
                }
            }
        }

        // 6) the 3D constellation
        if (constAlpha > 0.001) {
            int ec = Constellation.E.length;
            // lines draw themselves on one after another
            for (int i = 0; i < ec; i++) {
                double start = 4.3 + 2.0 * i / ec;
                double k = CameraPath.smooth(start, start + 0.45, t);
                if (k <= 0.001) continue;
                double[] a = Constellation.world(Constellation.E[i][0], base, fx, fz);
                double[] bpt = Constellation.world(Constellation.E[i][1], base, fx, fz);
                double[] e = {a[0] + (bpt[0] - a[0]) * k, a[1] + (bpt[1] - a[1]) * k, a[2] + (bpt[2] - a[2]) * k};
                ribbon(add, cam, a, e, 0.07, M_LINE, 200, 255, c(0.95 * constAlpha), 1f);
                ribbon(add, cam, a, e, 0.28, M_LINE, 80, 255, c(0.22 * constAlpha), 1f);
            }
            // soft limbs + filled silhouette (the faint blue "body" visible in the clip)
            if (silhouette > 0.001) {
                for (double[] l : Constellation.LIMBS) {
                    double[] a = Constellation.world((int) l[0], base, fx, fz), b = Constellation.world((int) l[1], base, fx, fz);
                    ribbon(add, cam, a, b, l[2] * Constellation.SCALE * 0.5, M_FILL, 120, 255, c(0.34 * silhouette), 1f);
                }
                for (int[] q : Constellation.Q) {
                    double[][] p = new double[4][];
                    for (int i = 0; i < 4; i++) p[i] = Constellation.world(q[i], base, fx, fz);
                    add.quad(cam, p[0], p[1], p[2], p[3], new float[]{-1,-1,1,-1,1,1,-1,1}, M_FILL, 60, 255, c(0.22 * silhouette));
                }
            }
            // star nodes
            for (int i = 0; i < Constellation.N.length; i++) {
                double tw = 0.75 + 0.25 * Math.sin(t * 3.1 + i * 1.7);
                double k = CameraPath.smooth(4.3 + 0.05 * i, 4.9 + 0.05 * i, t);
                if (k <= 0.001) continue;
                double size = (i == 34 ? 0.75 : 0.42) * tw * k;
                billboard(add, cam, Constellation.world(i, base, fx, fz), right, up, size, M_STAR, 255, 255, c(constAlpha));
            }
        }

        // 7) a few extra twinkling sky stars + a moon-like glow on the left, as in the aerial shots
        if (stars > 0.001) {
            Random sr = new Random(99);
            for (int i = 0; i < 120; i++) {
                double th = sr.nextDouble() * Math.PI * 2, ph = Math.acos(0.15 + sr.nextDouble() * 0.85);
                double[] ctr = {cam.x + Math.sin(ph) * Math.cos(th) * 120, cam.y + Math.cos(ph) * 120, cam.z + Math.sin(ph) * Math.sin(th) * 120};
                double tw = 0.5 + 0.5 * Math.sin(t * (1.5 + sr.nextDouble() * 2) + i);
                billboard(add, cam, ctr, right, up, 0.9 + sr.nextDouble() * 0.8, M_STAR, 255, 255, c(stars * tw * 0.8));
            }
            double[] moon = {cam.x - 60, cam.y + 38, cam.z + 80};
            billboard(add, cam, moon, right, up, 7.0, M_STAR, 255, 255, c(stars * 0.55));
        }

        draw(target, alpha, FX_ALPHA, add, FX_ADD);
    }

    private static double[] worldLocal(double[] base, double fx, double fz, double[] l) {
        double rx = fz, rz = -fx;
        return new double[]{base[0] + fx * l[2] + rx * l[0], base[1] + l[1], base[2] + fz * l[2] + rz * l[0]};
    }

    // ---------------------------------------------------------------- GPU submission
    private static void draw(RenderTarget target, Mesh first, RenderPipeline p1, Mesh second, RenderPipeline p2) {
        try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
            () -> "Thor cutscene fx", target.getColorTextureView(), Optional.empty(), target.getDepthTextureView(), OptionalDouble.empty())) {
            RenderSystem.bindDefaultUniforms(pass);
            submit(pass, first, p1);
            submit(pass, second, p2);
        }
    }

    private static void submit(RenderPass pass, Mesh m, RenderPipeline pipeline) {
        if (m.quads == 0) { m.bytes.close(); return; }
        try (MeshData data = m.bb.buildOrThrow()) {
            GpuBuffer vb = RenderSystem.getDevice().createBuffer(() -> "Thor fx vertices", GpuBuffer.USAGE_VERTEX, data.vertexBuffer());
            RenderSystem.AutoStorageIndexBuffer indices = RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS);
            int indexCount = m.quads * 6;
            GpuBuffer ib = indices.getBuffer(indexCount);
            GpuBufferSlice transforms = RenderSystem.getDynamicUniforms()
                .writeTransform(new Matrix4f(RenderSystem.getModelViewStack()), new Vector4f(1, 1, 1, 1));
            pass.setPipeline(RenderSystem.getCompiledPipeline(pipeline));
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("DynamicTransforms", transforms);
            pass.setVertexBuffer(0, vb.slice());
            pass.setIndexBuffer(ib, indices.type());
            pass.drawIndexed(indexCount, 1, 0, 0, 0);
            vb.close();
        } finally {
            m.bytes.close();
        }
    }
}
