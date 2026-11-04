#version 460 core
// Unpacks LodMesher quads (2 ints each) on the GPU. 6 vertices per quad, one instance per quad.
layout(std430, binding = 2) readonly buffer Quads { uvec2 quads[]; };
// Cell struct is 32 bytes = two vec4: [2*i] = origin xyz (camera-relative) + edge, [2*i+1] unused here.
layout(std430, binding = 0) readonly buffer CellsV { vec4 cellData[]; };

uniform mat4 uProj;
uniform mat4 uView;

out vec3 vNormal;
flat out uint vId;
out float vDist;

const vec3 NORMALS[6] = vec3[](vec3(-1,0,0), vec3(1,0,0), vec3(0,-1,0), vec3(0,1,0), vec3(0,0,-1), vec3(0,0,1));
// two triangles per face, corner offsets in the face plane
const ivec2 CORNERS[6] = ivec2[](ivec2(0,0), ivec2(1,0), ivec2(1,1), ivec2(0,0), ivec2(1,1), ivec2(0,1));

void main() {
    uvec2 q = quads[gl_BaseInstance + gl_InstanceID];
    uint a = q.x;
    ivec3 voxel = ivec3(a & 31u, (a >> 5) & 31u, (a >> 10) & 31u);
    uint face = (a >> 15) & 7u;
    ivec2 c = CORNERS[gl_VertexID];

    // axis the face is perpendicular to, and the two in-plane axes
    int axis = int(face) >> 1;
    int u = (axis + 1) % 3, v = (axis + 2) % 3;
    ivec3 pos = voxel;
    if ((face & 1u) == 1u) pos[axis] += 1;
    pos[u] += c.x;
    pos[v] += c.y;

    vec4 cell = cellData[2 * gl_DrawID];
    float vsize = cell.w / 16.0;                  // edge / 16: 1 for sections, 4 for far cells
    vec3 world = cell.xyz + vec3(pos) * vsize;
    if (vsize > 1.0) world.y -= (vsize - 1.0) * 0.5;   // coarse cells sit slightly low so they never poke through real terrain
    vDist = length(world);
    vNormal = NORMALS[face];
    vId = q.y;
    gl_Position = uProj * uView * vec4(world, 1.0);
}
