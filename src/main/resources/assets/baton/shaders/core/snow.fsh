#version 330

#moj_import <minecraft:dynamictransforms.glsl>

in vec4 vertexColor;
in vec2 local;
flat in vec4 shape;

out vec4 fragColor;

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

void main() {
    float time = shape.z + shape.w / 1000.0;
    float edge = fwidth(local.x);
    float snow = 0.0;
    for (int i = 0; i < 3; i++) {
        float layer = float(i);
        float cell = 58.0 - layer * 14.0;
        float radius = 1.3 - layer * 0.35;
        vec2 p = local + vec2(layer * 71.0 - time * 2.0, -time * (16.0 - layer * 5.0));
        vec2 id = floor(p / cell);
        float seed = hash(id + layer * 17.0);
        vec2 center = vec2(hash(id + 3.7), hash(id + 9.1)) * (cell - 14.0) + 7.0;
        center.x += sin(time * 0.7 + seed * 40.0) * 3.0;
        float dist = length(p - id * cell - center) - radius;
        float soft = edge + radius * 0.35;
        snow += step(seed, 0.42 - layer * 0.1) * (1.0 - smoothstep(-soft, soft, dist)) * (0.55 - layer * 0.17);
    }
    fragColor = vec4(mix(vertexColor.rgb, vec3(0.9, 0.95, 1.0), min(snow, 1.0)), vertexColor.a) * ColorModulator;
}
