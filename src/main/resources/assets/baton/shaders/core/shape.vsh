#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV2;

out vec4 vertexColor;
out vec2 local;
flat out vec4 shape;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    vertexColor = Color;
    local = UV0;
    vec2 params = vec2(UV2) / 8.0;
    shape = vec4(abs(UV0) - 1.0 - max(-params.y, 0.0), params);
}
