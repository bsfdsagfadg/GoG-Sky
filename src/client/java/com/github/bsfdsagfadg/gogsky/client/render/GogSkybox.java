/*
 * This class is adapted from Botania's SkyblockSkyRenderer (c) Vazkii.
 * Botania is Open Source and distributed under the Botania License.
 * Source: https://github.com/Vazkii/Botania
 */
package com.github.bsfdsagfadg.gogsky.client.render;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.platform.DestFactor;
import com.mojang.blaze3d.platform.SourceFactor;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import me.flashyreese.mods.nuit.api.skyboxes.SkyboxRenderContext;
import me.flashyreese.mods.nuit.api.skyboxes.SkyboxTextureProvider;
import me.flashyreese.mods.nuit.components.Blend;
import me.flashyreese.mods.nuit.components.Conditions;
import me.flashyreese.mods.nuit.components.Properties;
import me.flashyreese.mods.nuit.render.NuitRenderBackend;
import me.flashyreese.mods.nuit.render.NuitRenderPipelines;
import me.flashyreese.mods.nuit.skybox.AbstractSkybox;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.attribute.EnvironmentAttributes;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Quaternionf;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Random;

public class GogSkybox extends AbstractSkybox implements SkyboxTextureProvider {
    public static final Codec<GogSkybox> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Properties.CODEC.optionalFieldOf("properties", Properties.of()).forGetter(GogSkybox::getProperties),
            Conditions.CODEC.optionalFieldOf("conditions", Conditions.of()).forGetter(GogSkybox::getConditions),
            Blend.CODEC.optionalFieldOf("blend", Blend.normal()).forGetter(GogSkybox::getBlend)
    ).apply(instance, GogSkybox::new));

    private static final Identifier TEXTURE_SKYBOX = Identifier.fromNamespaceAndPath("gog-sky", "textures/environment/skybox.png");
    private static final Identifier TEXTURE_RAINBOW = Identifier.fromNamespaceAndPath("gog-sky", "textures/environment/rainbow.png");
    private static final Identifier TEXTURE_SUN = Identifier.withDefaultNamespace("textures/environment/celestial/sun.png");
    private static final Identifier TEXTURE_MOON = Identifier.withDefaultNamespace("textures/environment/celestial/moon/full_moon.png");

    private static final Identifier[] TEXTURE_PLANETS = {
            Identifier.fromNamespaceAndPath("gog-sky", "textures/environment/planet0.png"),
            Identifier.fromNamespaceAndPath("gog-sky", "textures/environment/planet1.png"),
            Identifier.fromNamespaceAndPath("gog-sky", "textures/environment/planet2.png"),
            Identifier.fromNamespaceAndPath("gog-sky", "textures/environment/planet3.png"),
            Identifier.fromNamespaceAndPath("gog-sky", "textures/environment/planet4.png"),
            Identifier.fromNamespaceAndPath("gog-sky", "textures/environment/planet5.png")
    };

    private Blend blend;

    // Animation state
    private float ticksInGame;

    private static final BlendFunction ADDITIVE = new BlendFunction(
            SourceFactor.SRC_ALPHA, DestFactor.ONE
    );
    private static final BlendFunction NORMAL_ALPHA = new BlendFunction(
            SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA
    );

    public GogSkybox(Properties properties, Conditions conditions, Blend blend) {
        super(properties, conditions);
        this.blend = blend;
    }

    @Override
    public void tick(ClientLevel level) {
        super.tick(level);
        if (!Minecraft.getInstance().isPaused()) {
            this.ticksInGame++;
        }
    }

    @Override
    public void render(SkyboxRenderContext context) {
        if (this.alpha <= 0F) return;

        context.applyFog();
        ClientLevel level = Objects.requireNonNull((ClientLevel) context.camera().entity().level());
        float tickDelta = context.tickDelta();
        Camera camera = context.camera();

        float rain = 1.0F - level.getRainLevel(tickDelta);
        float celAng = (float) camera.attributeProbe().getValue(
                EnvironmentAttributes.SUN_ANGLE, tickDelta);
        float effCelAng = celAng > 0.5F ? 0.5F - (celAng - 0.5F) : celAng;

        // Each element renders independently into the sky model view stack
        renderPlanets(context, rain, effCelAng);
        renderRays(context, rain, effCelAng, tickDelta);
        renderRainbow(context, celAng, level);
        renderStars(context, rain, effCelAng, tickDelta);
        renderCelestial(context, celAng);
    }

    // ──────────────────────────────────────────────
    //  Planets — six textured quads
    // ──────────────────────────────────────────────
    private void renderPlanets(SkyboxRenderContext context, float rain, float effCelAng) {
        float lowA = Math.max(0.0F, effCelAng - 0.3F) * rain;
        float a = Math.max(0.1F, lowA) * alpha;
        if (a <= 0.001F) return;
        // Botania multiplies alpha by 4: setShaderColor(1,1,1,a*4)
        float planetAlpha = a * 4F;

        Matrix4fStack stack = RenderSystem.getModelViewStack();
        stack.pushMatrix();
        try {
            stack.set(context.skyModelViewStack());
            stack.rotate(new Quaternionf().rotateAxis(toRad(90), 0.5F, 0.5F, 0F));

            RenderPipeline pipeline = NuitRenderPipelines.texturedSkybox(blend.getBlendFunction());
            Vector4f color = new Vector4f(1F, 1F, 1F, planetAlpha);
            float scale = 20.0F;

            for (int p = 0; p < TEXTURE_PLANETS.length; p++) {
                GpuBufferSlice transforms = NuitRenderBackend.createDynamicTransforms(new Matrix4f(stack), color);
                drawQuad(pipeline, transforms, TEXTURE_PLANETS[p],
                        -scale, 100, -scale, scale, 100, scale);
                // Advance position for next planet
                switch (p) {
                    case 0 -> { stack.rotate(new Quaternionf().rotateX(toRad(70))); scale = 12F; }
                    case 1 -> { stack.rotate(new Quaternionf().rotateZ(toRad(120))); scale = 15F; }
                    case 2 -> { stack.rotate(new Quaternionf().rotateAxis(toRad(80), 1, 0, 1)); scale = 25F; }
                    case 3 -> { stack.rotate(new Quaternionf().rotateZ(toRad(100))); scale = 10F; }
                    case 4 -> { stack.rotate(new Quaternionf().rotateAxis(toRad(-60), 1, 0, 0.5F)); scale = 40F; }
                }
            }
        } finally {
            stack.popMatrix();
        }
    }

    // ──────────────────────────────────────────────
    //  Sun rays — 3 rotating rings of quads
    // ──────────────────────────────────────────────
    private void renderRays(SkyboxRenderContext context, float rain, float effCelAng, float tickDelta) {
        float lowA = Math.max(0.0F, effCelAng - 0.3F) * rain;
        float a = lowA * alpha;
        if (a <= 0.001F) return;

        Matrix4fStack stack = RenderSystem.getModelViewStack();
        stack.pushMatrix();
        try {
            stack.set(context.skyModelViewStack());
            stack.translate(0, -1, 0);
            stack.rotate(new Quaternionf().rotateX(toRad(220)));

            float total = ticksInGame + tickDelta;
            float scale = 20.0F;
            int angles = 90;
            float y = 2.0F;
            float y0 = 0.0F;
            float uPer = 1.0F / 360.0F;
            float anglePer = 360.0F / angles;
            double fuzzPer = Math.PI * 10 / angles;
            float rotSpeed = 1.0F;
            float rotSpeedMod = 0.4F;

            RenderPipeline pipeline = NuitRenderPipelines.texturedSkybox(ADDITIVE);

            for (int p = 0; p < 3; p++) {
                float baseAngle = rotSpeed * rotSpeedMod * total;
                stack.rotate(new Quaternionf().rotateY(total * 0.25F * rotSpeed * rotSpeedMod));

                Vector4f color;
                switch (p) {
                    case 0 -> color = new Vector4f(1.0F, 0.4F * a, 0.4F * a, a);
                    case 1 -> color = new Vector4f(0.4F * a, 1.0F, 0.7F * a, a);
                    default -> color = new Vector4f(a, a, a, a);
                }
                GpuBufferSlice transforms = NuitRenderBackend.createDynamicTransforms(new Matrix4f(stack), color);

                drawRayRing(pipeline, transforms, scale, angles, y, y0, uPer, anglePer, fuzzPer, baseAngle);

                switch (p) {
                    case 0 -> {
                        stack.rotate(new Quaternionf().rotateX(toRad(20)));
                        fuzzPer = Math.PI * 14 / angles;
                        rotSpeed = 0.2F;
                    }
                    case 1 -> {
                        stack.rotate(new Quaternionf().rotateX(toRad(50)));
                        fuzzPer = Math.PI * 6 / angles;
                        rotSpeed = 2.0F;
                    }
                }
            }
        } finally {
            stack.popMatrix();
        }
    }

    private void drawRayRing(RenderPipeline pipeline, GpuBufferSlice transforms,
                             float scale, int angles, float y, float y0,
                             float uPer, float anglePer, double fuzzPer, float baseAngle) {
        // Each angle iteration = 2 vertices, so 2 * angles vertices
        int vertexCount = angles * 2;
        VertexFormat fmt = DefaultVertexFormat.POSITION_TEX;
        try (ByteBufferBuilder buf = new ByteBufferBuilder(fmt.getVertexSize() * vertexCount)) {
            BufferBuilder builder = new BufferBuilder(buf, VertexFormat.Mode.QUADS, fmt);
            for (int i = 0; i < angles; i++) {
                int j = (i % 2 == 0) ? i - 1 : i;
                float ang = j * anglePer + baseAngle;
                float xp = (float) Math.cos(ang * Mth.DEG_TO_RAD) * scale;
                float zp = (float) Math.sin(ang * Mth.DEG_TO_RAD) * scale;
                float yo = (float) Math.sin(fuzzPer * j) * 1.0F;
                float ut = ang * uPer;

                if (i % 2 == 0) {
                    builder.addVertex(xp, yo + y0 + y, zp).setUv(ut, 1.0F);
                    builder.addVertex(xp, yo + y0, zp).setUv(ut, 0.0F);
                } else {
                    builder.addVertex(xp, yo + y0, zp).setUv(ut, 0.0F);
                    builder.addVertex(xp, yo + y0 + y, zp).setUv(ut, 1.0F);
                }
            }
            NuitRenderBackend.drawTextured(pipeline, builder.buildOrThrow(), transforms,
                    "Sampler0", TEXTURE_SKYBOX);
        }
    }

    // ──────────────────────────────────────────────
    //  Rainbow arc
    // ──────────────────────────────────────────────
    private void renderRainbow(SkyboxRenderContext context, float celAng, ClientLevel level) {
        float eff = celAng;
        if (eff > 0.25F) eff = 1.0F - eff;
        eff = 0.25F - Math.min(0.25F, eff);
        eff *= alpha;
        if (eff <= 0.001F) return;

        long time = level.getDayTime() + 1000;
        int day = (int) (time / 24000L);
        Random rand = new Random(day * 0xFF);
        float angle1 = rand.nextFloat() * 360.0F;
        float angle2 = rand.nextFloat() * 360.0F;

        Matrix4fStack stack = RenderSystem.getModelViewStack();
        stack.pushMatrix();
        try {
            stack.set(context.skyModelViewStack());
            stack.rotate(new Quaternionf().rotateY(toRad(angle1)));
            stack.rotate(new Quaternionf().rotateZ(toRad(angle2)));

            // Rainbow uses normal alpha blend in Botania (GlStateManager._blendFuncSeparate(770,771,1,0))
            RenderPipeline pipeline = NuitRenderPipelines.texturedSkybox(NORMAL_ALPHA);
            Vector4f color = new Vector4f(eff, eff, eff, eff);
            GpuBufferSlice transforms = NuitRenderBackend.createDynamicTransforms(new Matrix4f(stack), color);

            float scale = 10.0F;
            int angles = 90;
            float y = 2.0F;
            float y0 = 0.0F;
            float uPer = 1.0F / 360.0F;
            float anglePer = 360.0F / angles;

            drawRainbowRing(pipeline, transforms, scale, angles, y, y0, uPer, anglePer);
        } finally {
            stack.popMatrix();
        }
    }

    private void drawRainbowRing(RenderPipeline pipeline, GpuBufferSlice transforms,
                                 float scale, int angles, float y, float y0,
                                 float uPer, float anglePer) {
        int vertexCount = angles * 2;
        VertexFormat fmt = DefaultVertexFormat.POSITION_TEX;
        try (ByteBufferBuilder buf = new ByteBufferBuilder(fmt.getVertexSize() * vertexCount)) {
            BufferBuilder builder = new BufferBuilder(buf, VertexFormat.Mode.QUADS, fmt);
            for (int i = 0; i < angles; i++) {
                int j = (i % 2 == 0) ? i - 1 : i;
                float ang = j * anglePer;
                float xp = (float) Math.cos(ang * Mth.DEG_TO_RAD) * scale;
                float zp = (float) Math.sin(ang * Mth.DEG_TO_RAD) * scale;
                float ut = ang * uPer;

                if (i % 2 == 0) {
                    builder.addVertex(xp, y0 + y0 + y, zp).setUv(ut, 1.0F);
                    builder.addVertex(xp, y0 + y0, zp).setUv(ut, 0.0F);
                } else {
                    builder.addVertex(xp, y0 + y0, zp).setUv(ut, 0.0F);
                    builder.addVertex(xp, y0 + y0 + y, zp).setUv(ut, 1.0F);
                }
            }
            NuitRenderBackend.drawTextured(pipeline, builder.buildOrThrow(), transforms,
                    "Sampler0", TEXTURE_RAINBOW);
        }
    }

    // ──────────────────────────────────────────────
    //  Stars — colored animated layers via vanilla star VBO
    // ──────────────────────────────────────────────
    private void renderStars(SkyboxRenderContext context, float rain, float effCelAng, float tickDelta) {
        float starAlpha = rain * Math.max(0.1F, effCelAng * 2.0F) * alpha;
        if (starAlpha <= 0.001F) return;

        // t in degrees — Botania's VecHelper.rotateY converts degrees to radians
        float tDeg = (this.ticksInGame + tickDelta + 2000) * 0.005F;
        PoseStack ps = new PoseStack();

        // Y-axis layers — Botania white/cyan/pink tint via the mixin
        ps.pushPose(); ps.mulPose(new Quaternionf().rotateY(toRad(tDeg * 3F)));
        com.github.bsfdsagfadg.gogsky.client.mixin.SkyRendererMixin.gogSetStarColor(1F, 1F, 1F);
        context.renderStars(starAlpha, ps); ps.popPose();
        ps.pushPose(); ps.mulPose(new Quaternionf().rotateY(toRad(tDeg)));
        com.github.bsfdsagfadg.gogsky.client.mixin.SkyRendererMixin.gogSetStarColor(0.5F, 1F, 1F);
        context.renderStars(starAlpha, ps); ps.popPose();
        ps.pushPose(); ps.mulPose(new Quaternionf().rotateY(toRad(tDeg * 2F)));
        com.github.bsfdsagfadg.gogsky.client.mixin.SkyRendererMixin.gogSetStarColor(1F, 0.75F, 0.75F);
        context.renderStars(starAlpha, ps); ps.popPose();

        // Z-axis layers (dimmed)
        ps.pushPose(); ps.mulPose(new Quaternionf().rotateZ(toRad(tDeg * 3F)));
        com.github.bsfdsagfadg.gogsky.client.mixin.SkyRendererMixin.gogSetStarColor(1F, 1F, 1F);
        context.renderStars(starAlpha * 0.25F, ps); ps.popPose();
        ps.pushPose(); ps.mulPose(new Quaternionf().rotateZ(toRad(tDeg)));
        com.github.bsfdsagfadg.gogsky.client.mixin.SkyRendererMixin.gogSetStarColor(0.5F, 1F, 1F);
        context.renderStars(starAlpha * 0.25F, ps); ps.popPose();
        ps.pushPose(); ps.mulPose(new Quaternionf().rotateZ(toRad(tDeg * 2F)));
        com.github.bsfdsagfadg.gogsky.client.mixin.SkyRendererMixin.gogSetStarColor(1F, 0.75F, 0.75F);
        context.renderStars(starAlpha * 0.25F, ps); ps.popPose();
    }

    // ──────────────────────────────────────────────
    //  Sun & Moon — scaled to Botania proportions (2×, 1.5×)
    // ──────────────────────────────────────────────
    private void renderCelestial(SkyboxRenderContext context, float celAng) {
        Matrix4fStack stack = RenderSystem.getModelViewStack();
        RenderPipeline pipeline = NuitRenderPipelines.texturedSkybox(blend.getBlendFunction());
        Vector4f color = new Vector4f(1F, 1F, 1F, alpha);

        // Sun: 2× (vanilla 30 → 60)
        stack.pushMatrix();
        try {
            stack.set(context.skyModelViewStack());
            stack.rotate(new Quaternionf().rotateY(-1.5707964F));
            stack.rotate(new Quaternionf().rotateY(celAng * 6.2831855F));
            stack.translate(0F, 100F, 0F);
            GpuBufferSlice transforms = NuitRenderBackend.createDynamicTransforms(new Matrix4f(stack), color);
            drawQuad(pipeline, transforms, TEXTURE_SUN, -60F, 0F, -60F, 60F, 0F, 60F);
        } finally {
            stack.popMatrix();
        }

        // Moon: 1.5× (vanilla 20 → 30)
        stack.pushMatrix();
        try {
            stack.set(context.skyModelViewStack());
            stack.rotate(new Quaternionf().rotateY(-1.5707964F));
            stack.rotate(new Quaternionf().rotateY(celAng * 6.2831855F));
            stack.translate(0F, 100F, 0F);
            GpuBufferSlice transforms = NuitRenderBackend.createDynamicTransforms(new Matrix4f(stack), color);
            drawQuad(pipeline, transforms, TEXTURE_MOON, -30F, 0F, -30F, 30F, 0F, 30F);
        } finally {
            stack.popMatrix();
        }
    }

    // ──────────────────────────────────────────────
    //  Helpers
    // ──────────────────────────────────────────────
    private static void drawQuad(RenderPipeline pipeline, GpuBufferSlice transforms,
                                 Identifier texture, float x1, float y1, float z1,
                                 float x2, float y2, float z2) {
        VertexFormat fmt = DefaultVertexFormat.POSITION_TEX;
        try (ByteBufferBuilder buf = new ByteBufferBuilder(fmt.getVertexSize() * 4)) {
            BufferBuilder builder = new BufferBuilder(buf, VertexFormat.Mode.QUADS, fmt);
            // Face must be normal-facing for backface culling: counter-clockwise
            builder.addVertex(x1, y1, z1).setUv(0.0F, 0.0F);
            builder.addVertex(x2, y1, z1).setUv(1.0F, 0.0F);
            builder.addVertex(x2, y2, z2).setUv(1.0F, 1.0F);
            builder.addVertex(x1, y2, z2).setUv(0.0F, 1.0F);
            NuitRenderBackend.drawTextured(pipeline, builder.buildOrThrow(), transforms,
                    "Sampler0", texture);
        }
    }

    private static float toRad(float deg) { return deg * Mth.DEG_TO_RAD; }

    // ──────────────────────────────────────────────
    //  Codec properties
    // ──────────────────────────────────────────────
    public Properties getProperties() { return properties; }
    public Conditions getConditions() { return conditions; }
    public Blend getBlend() { return this.blend; }

    @Override
    public Collection<Identifier> getTexturesToRegister() {
        List<Identifier> list = new ArrayList<>();
        list.add(TEXTURE_SKYBOX);
        list.add(TEXTURE_RAINBOW);
        list.add(TEXTURE_SUN);
        list.add(TEXTURE_MOON);
        list.addAll(List.of(TEXTURE_PLANETS));
        return list;
    }
}
