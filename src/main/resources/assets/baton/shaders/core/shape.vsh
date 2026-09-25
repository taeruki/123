#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV2;

out vec4 vertexColor;
out vec2 local;
out vec2 screen;
flat out vec4 shape;
flat out vec3 frame;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position.xy, 0.0, 1.0);
    vec2 scale = vec2(ProjMat[0][0], ProjMat[1][1]);
    vertexColor = Color;
    local = UV0;
    screen = gl_Position.xy / scale;
#ifdef PADDED
    shape = vec4(abs(UV0) - Position.z, vec2(UV2) / 8.0);
    frame = vec3(1.0 / abs(scale), 0.0);
#else
    shape = vec4(abs(UV0) - 1.0, vec2(UV2) / 8.0);
    frame = vec3(1.0 / abs(scale), Position.z);
#endif
}
