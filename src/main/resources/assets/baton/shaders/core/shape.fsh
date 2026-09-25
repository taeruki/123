#version 150

uniform vec4 ColorModulator;

in vec4 vertexColor;
in vec2 localPosition;
flat in vec4 shape;
flat in float softness;

out vec4 fragColor;

void main() {
    vec2 q = abs(localPosition) - shape.xy + shape.z;
    float dist = min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - shape.z;
    float edge = max(fwidth(dist), softness) * 0.5;
    float halfStroke = shape.w * 0.5;
    dist = shape.w > 0.0 ? abs(dist + halfStroke) - halfStroke : dist;
    float alpha = vertexColor.a * (1.0 - smoothstep(-edge, edge, dist));
    if (alpha <= 0.0) {
        discard;
    }
    fragColor = vec4(vertexColor.rgb, alpha) * ColorModulator;
}
