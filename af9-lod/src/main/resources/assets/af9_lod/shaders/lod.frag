#version 430 core
in vec3 vNormal;
flat in uint vId;
layout(location = 0) out vec4 outColor;

void main() {
    // Placeholder shading until H4 feeds the shader pack's gbuffers: id hash colour with simple directional light.
    float h = float((vId * 2654435761u) >> 8 & 0xFFFFu) / 65535.0;
    vec3 base = vec3(0.35 + 0.4 * h, 0.5 + 0.3 * fract(h * 7.0), 0.3 + 0.4 * fract(h * 13.0));
    float l = 0.55 + 0.45 * max(dot(normalize(vNormal), normalize(vec3(0.4, 0.9, 0.3))), 0.0);
    outColor = vec4(base * l, 1.0);
}
