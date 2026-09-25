#version 330

#moj_import <minecraft:dynamictransforms.glsl>

uniform sampler2D Sampler0;

in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

void main() {
    float dist = texture(Sampler0, texCoord0).r;
    float pixel = max(length(vec2(dFdx(dist), dFdy(dist))), 1e-4);
    float alpha = vertexColor.a * clamp((dist - 0.5) / pixel + 0.5, 0.0, 1.0);
    if (alpha <= 0.0) {
        discard;
    }
    fragColor = vec4(vertexColor.rgb, alpha) * ColorModulator;
}
