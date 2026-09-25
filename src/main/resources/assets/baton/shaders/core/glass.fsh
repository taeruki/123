#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <baton:scene.glsl>

in vec4 vertexColor;
in vec2 local;
in vec2 screen;
flat in vec4 shape;
flat in vec3 frame;

out vec4 fragColor;

void main() {
    float pixel = fwidth(screen.x);
    float dist = batonBox(local, shape.xy, shape.z);
    float mask = 1.0 - smoothstep(-0.5 * pixel, 0.5 * pixel, dist);
    if (mask <= 0.0) {
        discard;
    }
    float top = clamp(0.5 - local.y / (2.0 * shape.y), 0.0, 1.0);
    vec3 color = batonScene(screen, frame.xy, frame.z, 1.0, pixel);
    color = mix(color, vertexColor.rgb, shape.w / 125.0 * (0.7 + 0.6 * top));
    float rim = 1.0 - smoothstep(0.0, pixel * 1.2, abs(dist + pixel * 0.6));
    color += rim * mix(0.03, 0.17, top * top);
    fragColor = vec4(color, mask * vertexColor.a) * ColorModulator;
}
