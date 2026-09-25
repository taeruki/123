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
        float cell = 40.0 - layer * 9.0;
        float radius = 0.55 - layer * 0.12;
        vec2 p = local + vec2(layer * 71.0 - time * 1.5, -time * (13.0 - layer * 3.5));
        vec2 id = floor(p / cell);
        float seed = hash(id + layer * 17.0);
        vec2 center = vec2(hash(id + 3.7), hash(id + 9.1)) * (cell - 8.0) + 4.0;
        center.x += sin(time * 0.6 + seed * 40.0) * 2.5;
        float dist = length(p - id * cell - center) - radius;
        snow += step(seed, 0.55 - layer * 0.1) * (1.0 - smoothstep(-edge, edge, dist)) * (0.5 - layer * 0.14);
    }
    fragColor = vec4(mix(vertexColor.rgb, vec3(0.9, 0.95, 1.0), min(snow, 1.0)), vertexColor.a) * ColorModulator;
}
