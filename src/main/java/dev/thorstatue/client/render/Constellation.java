package dev.thorstatue.client.render;

/**
 * The Thor constellation as real 3D data (nodes, edges, filled silhouette quads), authored in the same
 * proportions as the statue (x right, y up, z toward the viewer, origin = statue base centre).
 * FxRenderer places it behind-left of the statue at ~1.9x scale and builds camera-facing glow quads.
 */
public final class Constellation {
    private Constellation() {}

    public static final double SCALE = 1.9;
    public static final double[] OFFSET = {-3.4, -0.4, -3.2}; // statue space

    // Nodes
    public static final double[][] N = {
        /*0*/ {-0.8, 6.3, 0.6}, {0.8, 6.3, 0.6}, {0.8, 4.9, 0.6}, {-0.8, 4.9, 0.6},      // head front
        /*4*/ {-0.8, 6.3, -0.6}, {0.8, 6.3, -0.6}, {0.8, 4.9, -0.6}, {-0.8, 4.9, -0.6},  // head back
        /*8*/ {0.0, 4.7, 0.0},                                                             // neck
        /*9*/ {-1.8, 4.3, 0.0}, {1.8, 4.3, 0.0},                                           // shoulders
        /*11*/ {-2.4, 3.0, 0.4}, {2.4, 3.0, 0.4},                                          // elbows
        /*13*/ {-1.0, 2.6, 0.9}, {1.0, 2.6, 0.9},                                          // hands
        /*15*/ {0.0, 3.6, 0.0},                                                            // chest
        /*16*/ {-1.1, 2.0, 0.0}, {1.1, 2.0, 0.0},                                          // waist
        /*18*/ {-1.0, 0.6, 0.0}, {1.0, 0.6, 0.0},                                          // hips
        /*20*/ {-1.3, -1.2, 0.2}, {1.3, -1.2, 0.2},                                        // feet
        /*22*/ {1.6, 6.5, -0.5}, {4.2, 6.5, -0.5}, {4.2, 7.7, -0.5}, {1.6, 7.7, -0.5},     // hammer back
        /*26*/ {1.6, 6.5, 0.5}, {4.2, 6.5, 0.5}, {4.2, 7.7, 0.5}, {1.6, 7.7, 0.5},         // hammer front
        /*30*/ {-0.35, 5.7, 0.62}, {-0.1, 5.7, 0.62}, {0.1, 5.7, 0.62}, {0.35, 5.7, 0.62}, // eyes
        /*34*/ {2.9, 7.1, 0.52},                                                            // hammer sigil
    };

    // Constellation lines (index pairs); ordered roughly top-down so the reveal "draws" the figure
    public static final int[][] E = {
        {0,1},{1,2},{2,3},{3,0},{4,5},{5,6},{6,7},{7,4},{0,4},{1,5},{2,6},{3,7},
        {30,31},{32,33},
        {22,23},{23,24},{24,25},{25,22},{26,27},{27,28},{28,29},{29,26},{22,26},{23,27},{24,28},{25,29},
        {14,26},{12,14},{10,12},
        {3,8},{2,8},{8,9},{8,10},{8,15},{9,11},{11,13},{9,15},{10,15},
        {15,16},{15,17},{9,16},{10,17},{16,17},{16,18},{17,19},{18,19},{18,20},{19,21},
    };

    // Filled, faintly glowing silhouette quads (4 node indices each)
    public static final int[][] Q = {
        {0,1,2,3},{4,5,6,7},{0,1,5,4},{3,2,6,7},
        {22,23,24,25},{26,27,28,29},{22,23,27,26},{25,24,28,29},{22,25,29,26},{23,24,28,27},
        {9,10,17,16},{9,10,15,15},
    };

    // Chunky limbs rendered as wide soft ribbons: {a, b, width}
    public static final double[][] LIMBS = {
        {9,11,0.85},{11,13,0.7},{10,12,0.85},{12,14,0.7},{14,26,0.2},{16,18,0.9},{17,19,0.9},{18,20,0.7},{19,21,0.7},
    };

    public static double[] world(int node, double[] base, double fx, double fz) {
        double[] n = N[node];
        double x = n[0] * SCALE + OFFSET[0], y = n[1] * SCALE + OFFSET[1], z = n[2] * SCALE + OFFSET[2];
        double rx = fz, rz = -fx;
        return new double[] { base[0] + fx * z + rx * x, base[1] + y, base[2] + fz * z + rz * x };
    }
}
