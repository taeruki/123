#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <baton:scene.glsl>

in vec4 vertexColor;
in vec2 local;
flat in vec4 shape;

out vec4 fragColor;

void main() {
    float dist = batonBox(local, shape.xy, shape.z);
    float soft = 0.5 * fwidth(local.x);
    if (shape.w > 0.0) {
        dist = abs(dist + shape.w * 0.5) - shape.w * 0.5;
    } else if (shape.w < 0.0) {
        soft = max(soft, -shape.w);
    }
    float alpha = vertexColor.a * (1.0 - smoothstep(-soft, soft, dist));
    if (alpha <= 0.0) {
        discard;
    }
    fragColor = vec4(vertexColor.rgb, alpha) * ColorModulator;
}
