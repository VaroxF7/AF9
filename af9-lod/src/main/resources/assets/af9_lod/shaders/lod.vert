#version 430 core
// Unpacks LodMesher quads (2 ints each) on the GPU. 6 vertices per quad, one instance per quad.
layout(std430, binding = 2) readonly buffer Quads { uvec2 quads[]; };
layout(std430, binding = 0) readonly buffer CellsV { vec4 cellOrigin[]; };  // xyz camera-relative origin, w = voxel size

uniform mat4 uProj;
uniform mat4 uView;
uniform uint uCellOfQuadStride;  // unused placeholder for H4 (per-cell origin lookup via draw id)

out vec3 vNormal;
flat out uint vId;

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

    vec3 world = vec3(pos);   // H4 multiplies by voxel size and adds the cell origin
    vNormal = NORMALS[face];
    vId = q.y;
    gl_Position = uProj * uView * vec4(world, 1.0);
}
