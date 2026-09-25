#version 330

float batonHash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

float batonBox(vec2 p, vec2 halfSize, float radius) {
    radius = min(radius, min(halfSize.x, halfSize.y));
    vec2 q = abs(p) - halfSize + radius;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - radius;
}

vec3 batonScene(vec2 screen, vec2 halfScreen, float time, float blur, float pixel) {
    vec2 uv = screen / halfScreen;
    float light = 1.0 - smoothstep(0.0, 1.7, length(vec2(uv.x * 0.75, uv.y + 0.3)));
    vec3 color = mix(vec3(0.016, 0.017, 0.022), vec3(0.062, 0.066, 0.082), light);
    float snow = 0.0;
    for (int i = 0; i < 3; i++) {
        float layer = float(i);
        float cell = 40.0 - layer * 9.0;
        float radius = 0.55 - layer * 0.12;
        vec2 p = screen + vec2(layer * 71.0 - time * 1.5, -time * (13.0 - layer * 3.5));
        vec2 id = floor(p / cell);
        float seed = batonHash(id + layer * 17.0);
        vec2 center = vec2(batonHash(id + 3.7), batonHash(id + 9.1)) * (cell - 8.0) + 4.0;
        center.x += sin(time * 0.6 + seed * 40.0) * 2.5;
        float soft = pixel + blur * (2.4 - layer * 0.5);
        float dist = length(p - id * cell - center) - radius;
        float energy = radius / (radius + blur * 1.2);
        snow += step(seed, 0.55 - layer * 0.1) * (1.0 - smoothstep(-soft, soft, dist)) * (0.5 - layer * 0.14) * energy;
    }
    color = mix(color, vec3(0.86, 0.89, 0.95), min(snow, 1.0));
    return color + (batonHash(floor(screen / pixel)) - 0.5) / 255.0;
}
