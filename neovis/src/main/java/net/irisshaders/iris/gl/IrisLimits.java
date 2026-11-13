package net.irisshaders.iris.gl;

public class IrisLimits {
	/**
	 * The maximum number of color textures that a shader pack can write to and read from in gbuffer and composite
	 * programs.
	 * <p>
	 * Neovis: raised from 16 to 32 like Iris 1.10 (colortex0-31). Color textures are only allocated when a program
	 * actually reads or writes them (RenderTargets.getOrCreate / createIfUnsure), so unused targets cost nothing.
	 * Needed by Voxy-compatible packs such as Photon, which writes distant water into colortex16.
	 */
	public static final int MAX_COLOR_BUFFERS = 32;
	public static final boolean VK_CONFORMANCE = false;
}
