# Vulkan renderer for 1.20.1 Forge (Embeddium + Oculus/Iris) - plan

Status: plan only, no code. Written 2026-11-04 after a feasibility study of Nvidium on AMD RX 9070 XT.

## Why Nvidium cannot just be ported
Nvidium (Fabric 1.21, last commit Aug 2024, ~4.5k lines Java, ~1.2k lines GLSL) uses
GL_NV_mesh_shader, NV_gpu_shader5, NV_shader_buffer_load, NV_bindless_texture,
conservative raster and representative fragment test. AMD's Windows OpenGL driver exposes
none of these. The Vulkan equivalents are VK_EXT_mesh_shader, buffer device address and
descriptor indexing, all supported on RDNA 4.

## Hard constraint
Minecraft 1.20.1 renders with OpenGL (LWJGL GL context). Iris/Oculus shader packs are
OpenGL GLSL (gbuffers, composite, shadow passes). A Vulkan engine that also runs shader packs
needs one of these architectures:

1. **Vulkan terrain beside GL (interop).** Keep the GL context for everything. A Vulkan
   device renders only chunk terrain (mesh shaders, GPU culling) into images shared with GL
   through GL_EXT_memory_object + GL_EXT_semaphore (AMD supports both). Iris keeps running
   in GL and consumes the shared gbuffer images. Iris pack GLSL is untouched.
   Risk: terrain gbuffer shaders from the pack must also run on the Vulkan side, so pack
   GLSL has to be compiled to SPIR-V (glslang, GL-flavoured). Many packs use GL-only
   features, so expect partial pack compatibility.
2. **Zink under the whole game (drop-in).** No mod work, supports shader packs as they are,
   but no mesh shaders and an unknown FPS change. Cheap test first.
3. **Full Vulkan backend (VulkanMod-style).** Replaces all rendering. Breaks mods with GL
   calls, Iris/Oculus is not supported. Not compatible with this pack's goals.

Recommended: step 0 test Zink, then build option 1 incrementally.

## Milestones (option 1)
- M0 (days): FPS baseline with a spark profile; Zink drop-in test; VulkanMod Forge port test copy.
- M1: Standalone LWJGL Vulkan window rendering a mesh-shader cube grid (proves EXT_mesh_shader on RDNA 4, no Minecraft).
- M2: GL/Vulkan interop proof: Vulkan renders to an image, GL samples it in a Forge client mod.
- M3: Hook Embeddium chunk meshing: upload section vertex data into a Vulkan buffer, draw with mesh shaders, no shader pack.
- M4: GPU culling (port the Nvidium task/mesh culling shaders to EXT_mesh_shader, credit LGPL-3).
- M5: Iris/Oculus gbuffer interop: terrain draws into the pack's gbuffer attachments.
- M6: translucency, shadow pass, fog, entities-stay-in-GL.
Realistic effort: M1-M3 about weeks, M5 months. Each milestone needs testing on Janne's PC.

## Environment limits
Cloud container can reach Maven Central but not maven.minecraftforge.net, so Forge mod builds
only work in GitHub CI or on Janne's PC. No GPU here: nothing Vulkan can be run here.
Licenses: Nvidium LGPL-3.0, Meshelium LGPL-3.0 (reuse needs attribution).

## References
Nvidium github.com/MCRcortex/nvidium, Meshelium github.com/ded811/Meshelium,
Radionium github.com/Kasijo/Radionium, VulkanMod Forge port (CurseForge), FSR Reloaded (Modrinth).
