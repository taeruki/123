#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <baton:scene.glsl>

in vec2 screen;
flat in vec3 frame;

out vec4 fragColor;

void main() {
    fragColor = vec4(batonScene(screen, frame.xy, frame.z, 0.0, fwidth(screen.x)), 1.0) * ColorModulator;
}
