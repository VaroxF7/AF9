#version 430 core
in vec3 vNormal;
flat in uint vId;
in float vDist;
layout(location = 0) out vec4 outColor;
// Extra attachments so a shader pack's gbuffers get sane data instead of garbage (ignored when not bound):
layout(location = 1) out vec4 outNormal;    // colortex1: view-independent normal, encoded n * 0.5 + 0.5
layout(location = 2) out vec4 outLight;     // colortex2: (block light, sky light, 0, 1)

uniform float uLight;       // 0..1 sky brightness (day/night)
uniform vec3 uFogColor;
uniform vec2 uFog;          // fog start, end (blocks)

void main() {
    vec3 base = vec3(float((vId >> 16) & 255u), float((vId >> 8) & 255u), float(vId & 255u)) / 255.0;
    float sun = max(dot(normalize(vNormal), normalize(vec3(0.4, 0.9, 0.3))), 0.0);
    float shade = mix(0.55, 1.0, sun) * mix(0.18, 1.0, uLight);
    vec3 col = base * shade;
    float f = clamp((vDist - uFog.x) / max(uFog.y - uFog.x, 1.0), 0.0, 1.0);
    outColor = vec4(mix(col, uFogColor, f * f), 1.0);
    outNormal = vec4(normalize(vNormal) * 0.5 + 0.5, 1.0);
    outLight = vec4(0.0, 1.0, 0.0, 1.0);
}
