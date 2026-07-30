package com.github.bsfdsagfadg.gogsky.client.render;

/**
 * Thread-local star color state shared between GogSkybox (renderer) and
 * SkyRendererMixin (which patches Vector4f color in SkyRenderer.renderStars).
 * <p>
 * Why a separate class? Mixin classes MUST NOT contain non-private methods
 * (they'd be treated as injectable methods).  This helper keeps the public
 * API surface out of the mixin class.
 */
public final class StarColorHelper {
    private static float starR = 1F;
    private static float starG = 1F;
    private static float starB = 1F;

    /** Called by GogSkybox before each context.renderStars() call. */
    public static void set(float r, float g, float b) {
        starR = r;
        starG = g;
        starB = b;
    }

    /** Called by SkyRendererMixin's @ModifyArg handlers. */
    public static float modifierR(float original) { return original * starR; }
    public static float modifierG(float original) { return original * starG; }
    public static float modifierB(float original) { return original * starB; }

    private StarColorHelper() {}
}
