#version 330

#moj_import <minecraft:dynamictransforms.glsl>

in vec4 vertexColor;
in vec2 local;
flat in vec4 shape;

out vec4 fragColor;

void main() {
    float radius = shape.z / 8.0;
    float stroke = shape.w / 8.0;
    vec2 q = abs(local) - shape.xy + radius;
    float dist = min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - radius;
    if (stroke > 0.0) {
        dist = abs(dist + stroke * 0.5) - stroke * 0.5;
    }
    float edge = 0.5 * fwidth(local.x);
    float alpha = vertexColor.a * (1.0 - smoothstep(-edge, edge, dist));
    if (alpha <= 0.0) {
        discard;
    }
    fragColor = vec4(vertexColor.rgb, alpha) * ColorModulator;
}
