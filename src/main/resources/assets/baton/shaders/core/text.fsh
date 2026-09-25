#version 330

#moj_import <minecraft:dynamictransforms.glsl>

uniform sampler2D Sampler0;

in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

void main() {
    float dist = texture(Sampler0, texCoord0).r;
    float edge = max(fwidth(dist) * 0.5, 1e-4);
    float alpha = vertexColor.a * smoothstep(0.5 - edge, 0.5 + edge, dist);
    if (alpha <= 0.0) {
        discard;
    }
    fragColor = vec4(vertexColor.rgb, alpha) * ColorModulator;
}
