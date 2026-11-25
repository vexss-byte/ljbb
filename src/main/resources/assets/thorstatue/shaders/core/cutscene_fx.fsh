#version 450

layout(std140) uniform Globals {
    ivec3 CameraBlockPos;
    float GlintAlpha;
    vec3 CameraOffset;
    float GameTime;      // fraction of a 24000 tick day
    vec2 ScreenSize;
    int MenuBlurRadius;
    int UseRgss;
};

// Declared so the pipeline's Fog bind group is always present in the shader (it is bound by default).
layout(std140) uniform Fog {
    vec4 FogColor;
    float EnvironmentalStart;
    float EnvironmentalEnd;
    float RenderDistanceStart;
    float RenderDistanceEnd;
    float SkyEnd;
    float CloudEnd;
};

layout(location = 0) in vec2 texCoord0;
layout(location = 1) in vec4 vertexColor;
layout(location = 2) in vec3 localPos;

layout(location = 0) out vec4 fragColor;

// ---- value-noise FBM (clouds) -----------------------------------------------------------
float hash(vec2 p) { return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453123); }
float noise(vec2 p) {
    vec2 i = floor(p), f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i), hash(i + vec2(1, 0)), f.x),
               mix(hash(i + vec2(0, 1)), hash(i + vec2(1, 1)), f.x), f.y);
}
float fbm(vec2 p) {
    float v = 0.0, a = 0.5;
    for (int i = 0; i < 5; i++) { v += a * noise(p); p = p * 2.03 + 17.0; a *= 0.5; }
    return v;
}

void main() {
    int mode = int(vertexColor.r * 255.0 + 0.5);
    float g = vertexColor.g;
    float b = vertexColor.b;
    float a = vertexColor.a;
    float time = GameTime * 1200.0;
    vec4 outc = vec4(0.0);

    if (mode == 0) {                                   // full-world dark-blue tint
        outc = vec4(0.02, 0.05, 0.14, g);
    } else if (mode == 1) {                            // star/cloud dome
        vec3 d = normalize(localPos);
        float h = clamp(d.y * 0.5 + 0.5, 0.0, 1.0);
        vec3 col = mix(vec3(0.03, 0.06, 0.16), vec3(0.005, 0.01, 0.045), pow(h, 0.7));
        vec2 sp = d.xz / (abs(d.y) + 0.25);
        float cl = fbm(sp * 1.6 + time * 0.02);
        col += vec3(0.05, 0.08, 0.16) * smoothstep(0.45, 0.85, cl) * (1.0 - h * 0.6);
        float horizon = smoothstep(0.35, 0.0, abs(d.y)); // thick mist at the horizon
        col = mix(col, vec3(0.07, 0.10, 0.17), horizon * 0.8);
        outc = vec4(col, a);
    } else if (mode == 2) {                            // curved dark cloud sea
        vec2 uv = texCoord0;
        float n = fbm(uv * 2.2 + vec2(time * 0.03, time * 0.012));
        float n2 = fbm(uv * 5.5 - vec2(time * 0.02, 0.0));
        float dens = smoothstep(0.30, 0.78, n * 0.75 + n2 * 0.35);
        vec3 col = mix(vec3(0.02, 0.04, 0.10), vec3(0.10, 0.14, 0.24), n2);
        float edge = smoothstep(0.0, 0.18, g);         // soft inner rim around the island
        outc = vec4(col, a * edge * (0.55 + 0.45 * dens));
    } else if (mode == 3) {                            // ground fog puff
        vec2 p = texCoord0;
        float r = length(p);
        float n = fbm(p * 2.0 + vec2(g, b) * 9.0 + time * 0.015);
        float fall = smoothstep(1.0, 0.1, r) * smoothstep(0.1, 0.6, n);
        outc = vec4(vec3(0.20, 0.25, 0.34) * (0.7 + 0.5 * n), a * fall);
    } else if (mode == 4 || mode == 6 || mode == 7) {  // glowing ribbon (constellation line / bolt / limb)
        float w = abs(texCoord0.y);
        float core = exp(-w * w * 14.0);
        float halo = exp(-w * w * 3.2);
        vec3 tint = (mode == 6) ? vec3(0.78, 0.86, 1.0) : vec3(0.35, 0.62, 1.0);
        if (mode == 7) { halo = exp(-w * w * 1.6); core = 0.0; tint = vec3(0.22, 0.46, 0.95); }
        vec3 col = tint * (0.5 + halo) + vec3(1.0) * core * 0.9;
        outc = vec4(col, a * clamp(core + halo * 0.55, 0.0, 1.0));
    } else if (mode == 5) {                            // star node: glow + 4-point spikes
        vec2 p = texCoord0;
        float r = length(p);
        float glow = exp(-r * r * 9.0);
        float spike = exp(-abs(p.x) * 26.0) * exp(-abs(p.y) * 3.0) + exp(-abs(p.y) * 26.0) * exp(-abs(p.x) * 3.0);
        float s = clamp(glow + spike * 0.55, 0.0, 1.0);
        vec3 col = mix(vec3(0.45, 0.7, 1.0), vec3(1.0), glow);
        outc = vec4(col, a * s);
    }

    if (outc.a <= 0.003) discard;
    fragColor = outc + vec4(0.0, 0.0, 0.0, FogColor.a * 0.0 + EnvironmentalEnd * 0.0);
}
