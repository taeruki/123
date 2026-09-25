#version 330

#moj_import <minecraft:dynamictransforms.glsl>

in vec4 vertexColor;
in vec2 local;
flat in vec4 shape;

out vec4 fragColor;

void main() {
    float radius = min(shape.z, min(shape.x, shape.y));
    vec2 q = abs(local) - shape.xy + radius;
    float dist = min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - radius;
    float pixel = length(vec2(dFdx(local.x), dFdy(local.x)));
    float coverage;
    if (shape.w < 0.0) {
        coverage = 1.0 - smoothstep(shape.w, -shape.w, dist);
    } else {
        if (shape.w > 0.0) {
            dist = abs(dist + shape.w * 0.5) - shape.w * 0.5;
        }
        coverage = clamp(0.5 - dist / pixel, 0.0, 1.0);
    }
    float alpha = vertexColor.a * coverage;
    if (alpha <= 0.0) {
        discard;
    }
    fragColor = vec4(vertexColor.rgb, alpha) * ColorModulator;
}
