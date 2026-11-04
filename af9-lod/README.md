# AF9 Horizon (af9-lod)

Clean-room GPU-driven distant terrain for Forge 1.20.1, aimed at AMD (RX 9070 XT) and working with Embeddium + Oculus
shader packs. No code from Voxy is used (Voxy is all rights reserved). Design: ../docs/vulkan-renderer-plan.md
(the mesh shader route was dropped in favour of plain OpenGL 4.6, which AMD supports fully).

Pipeline: chunk data -> LOD column store (CPU feeds it) -> GPU compute culling + level selection ->
glMultiDrawElementsIndirect into the pack's gbuffers (Oculus) so shader packs light it.

Milestones: H1 GL capability check + debug overlay; H2 LOD store + mesher; H3 compute culling + indirect draw;
H4 Oculus gbuffer/shadow integration; H5 streaming/persistence. Tests on Janne's PC (no GPU in the cloud).
Build: CI workflow "Build AF9 Horizon" (cloud cannot reach the Forge maven). Pure logic is in com.af9.lod.core.
