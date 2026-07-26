package com.github.bsfdsagfadg.gogsky.client.render;

import com.github.bsfdsagfadg.gogsky.client.ClientTickHandler;
import com.github.bsfdsagfadg.gogsky.client.util.VecHelper;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.Random;
import java.util.Map;
import java.util.HashMap;

/**
 * 水晶花园天空渲染器
 * 适配自 Botania (植物魔法) 的 Garden of Glass 天空效果。
 */
public class SkyblockSkyRenderer {
	private static final Identifier textureSkybox = Identifier.fromNamespaceAndPath("gog-sky", "textures/environment/skybox.png");
	private static final Identifier textureRainbow = Identifier.fromNamespaceAndPath("gog-sky", "textures/environment/rainbow.png");
	private static final Identifier[] planetTextures = new Identifier[] {
			Identifier.fromNamespaceAndPath("gog-sky", "textures/environment/planet0.png"),
			Identifier.fromNamespaceAndPath("gog-sky", "textures/environment/planet1.png"),
			Identifier.fromNamespaceAndPath("gog-sky", "textures/environment/planet2.png"),
			Identifier.fromNamespaceAndPath("gog-sky", "textures/environment/planet3.png"),
			Identifier.fromNamespaceAndPath("gog-sky", "textures/environment/planet4.png"),
			Identifier.fromNamespaceAndPath("gog-sky", "textures/environment/planet5.png")
	};

	/**
	 * 渲染额外的大气效果（行星、极光、彩虹）
	 * 
	 * @param ms 姿态堆栈
	 * @param bufferSource 缓冲源
	 * @param world 客户端世界
	 * @param partialTicks 帧内插值时间
	 * @param insideVoid 虚空深度透明度修正
	 */
	public static void renderExtra(PoseStack ms, MultiBufferSource bufferSource, ClientLevel world, float celAng, float partialTicks, float insideVoid) {
		float rain = 1.0F - world.getRainLevel(partialTicks);
		float effCelAng = celAng;
		if (celAng > 0.5) {
			effCelAng = 0.5F - (celAng - 0.5F);
		}

		// --- 渲染行星 ---
		float scale = 20F;
		float lowA = Math.max(0F, effCelAng - 0.3F) * rain;
		float a = Math.max(0.1F, lowA);
		int planetColor = ARGB.white(a * 4 * (1F - insideVoid));

		ms.pushPose();
		ms.mulPose(new Quaternionf().rotateAxis(VecHelper.toRadians(90), 0.5F, 0.5F, 0F));
		for (int p = 0; p < planetTextures.length; p++) {
			RenderType renderType = SkyblockSkyRenderer.getPlanetRenderType(planetTextures[p]);
			VertexConsumer consumer = bufferSource.getBuffer(renderType);
			Matrix4f mat = ms.last().pose();
			consumer.addVertex(mat, -scale, 100, -scale).setUv(0.0F, 0.0F).setColor(planetColor);
			consumer.addVertex(mat, scale, 100, -scale).setUv(1.0F, 0.0F).setColor(planetColor);
			consumer.addVertex(mat, scale, 100, scale).setUv(1.0F, 1.0F).setColor(planetColor);
			consumer.addVertex(mat, -scale, 100, scale).setUv(0.0F, 1.0F).setColor(planetColor);

			switch (p) {
				case 0 -> {
					ms.mulPose(VecHelper.rotateX(70));
					scale = 12F;
				}
				case 1 -> {
					ms.mulPose(VecHelper.rotateZ(120));
					scale = 15F;
				}
				case 2 -> {
					ms.mulPose(new Quaternionf().rotateAxis(VecHelper.toRadians(80), 1, 0, 1));
					scale = 25F;
				}
				case 3 -> {
					ms.mulPose(VecHelper.rotateZ(100));
					scale = 10F;
				}
				case 4 -> {
					ms.mulPose(new Quaternionf().rotateAxis(VecHelper.toRadians(-60), 1, 0, 0.5F));
					scale = 40F;
				}
			}
		}
		ms.popPose();

		// --- 渲染极光/光带 (Rays) ---
		scale = 20F;
		a = lowA;
		int rayBaseColor = ARGB.white(a);
		ms.pushPose();
		ms.translate(0, -1, 0);
		ms.mulPose(VecHelper.rotateX(220));

		int angles = 90;
		float y = 2F;
		float y0 = 0F;
		float uPer = 1F / 360F;
		float anglePer = 360F / angles;
		double fuzzPer = Math.PI * 10 / angles;
		float rotSpeed = 1F;
		float rotSpeedMod = 0.4F;

		for (int p = 0; p < 3; p++) {
			float baseAngle = rotSpeed * rotSpeedMod * ClientTickHandler.total();
			ms.mulPose(VecHelper.rotateY(ClientTickHandler.total() * 0.25F * rotSpeed * rotSpeedMod));

			int rayColor = rayBaseColor;
			if (p == 1) rayColor = ARGB.color((int) (a * 255), 255, 102, 102);
			if (p == 2) rayColor = ARGB.color((int) (a * 255), 102, 255, 178);

			VertexConsumer consumer = bufferSource.getBuffer(RenderTypes.eyes(textureSkybox));
			Matrix4f mat = ms.last().pose();
			for (int i = 0; i < angles; i++) {
				int j = i;
				if (i % 2 == 0) j--;

				float ang = j * anglePer + baseAngle;
				float xp = (float) Math.cos(ang * Math.PI / 180F) * scale;
				float zp = (float) Math.sin(ang * Math.PI / 180F) * scale;
				float yo = (float) Math.sin(fuzzPer * j) * 1;
				float ut = ang * uPer;

				if (i % 2 == 0) {
					consumer.addVertex(mat, xp, yo + y0 + y, zp).setUv(ut, 1F).setColor(rayColor).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0, 1, 0);
					consumer.addVertex(mat, xp, yo + y0, zp).setUv(ut, 0).setColor(rayColor).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0, 1, 0);
				} else {
					consumer.addVertex(mat, xp, yo + y0, zp).setUv(ut, 0).setColor(rayColor).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0, 1, 0);
					consumer.addVertex(mat, xp, yo + y0 + y, zp).setUv(ut, 1F).setColor(rayColor).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0, 1, 0);
				}
			}

			switch (p) {
				case 0 -> {
					ms.mulPose(VecHelper.rotateX(20));
					fuzzPer = Math.PI * 14 / angles;
					rotSpeed = 0.2F;
				}
				case 1 -> {
					ms.mulPose(VecHelper.rotateX(50));
					fuzzPer = Math.PI * 6 / angles;
					rotSpeed = 2F;
				}
			}
		}
		ms.popPose();

		// --- 渲染彩虹 (Rainbow) ---
		ms.pushPose();
		float effCelAng1 = celAng;
		if (effCelAng1 > 0.25F) {
			effCelAng1 = 1F - effCelAng1;
		}
		effCelAng1 = 0.25F - Math.min(0.25F, effCelAng1);

		long time = world.getDayTime() + 1000;
		int day = (int) (time / 24000L);
		Random rand = new Random(day * 0xFF);
		float angle1 = rand.nextFloat() * 360F;
		float angle2 = rand.nextFloat() * 360F;
		int rainbowColor = ARGB.white(effCelAng1 * (1F - insideVoid));

		ms.mulPose(VecHelper.rotateY(angle1));
		ms.mulPose(VecHelper.rotateZ(angle2));

		VertexConsumer consumer = bufferSource.getBuffer(RenderTypes.eyes(textureRainbow));
		Matrix4f mat = ms.last().pose();
		for (int i = 0; i < angles; i++) {
			int j = i;
			if (i % 2 == 0) j--;

			float ang = j * anglePer;
			float xp = (float) Math.cos(ang * Math.PI / 180F) * scale;
			float zp = (float) Math.sin(ang * Math.PI / 180F) * scale;
			float ut = ang * uPer;

			if (i % 2 == 0) {
				consumer.addVertex(mat, xp, y0 + y, zp).setUv(ut, 1F).setColor(rainbowColor).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0, 1, 0);
				consumer.addVertex(mat, xp, y0, zp).setUv(ut, 0).setColor(rainbowColor).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0, 1, 0);
			} else {
				consumer.addVertex(mat, xp, y0, zp).setUv(ut, 0).setColor(rainbowColor).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0, 1, 0);
				consumer.addVertex(mat, xp, y0 + y, zp).setUv(ut, 1F).setColor(rainbowColor).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0, 1, 0);
			}
		}
		ms.popPose();
	}

	/**
	 * 渲染多层旋转星空
	 * 针对 1.21.11 的 WebGPU-like 架构，通过 RenderPass 实现多层渲染叠加。
	 */
	public static void renderStars(GpuBuffer starBuffer, GpuBuffer quadIndices, int starIndexCount, RenderSystem.AutoStorageIndexBuffer quadIndexBuffer, PoseStack ms, float celAng, float partialTicks) {
		Minecraft mc = Minecraft.getInstance();
		float rain = 1.0F - mc.level.getRainLevel(partialTicks);
		float effCelAng = celAng;
		if (celAng > 0.5) {
			effCelAng = 0.5F - (celAng - 0.5F);
		}
		float alpha = rain * Math.max(0.1F, effCelAng * 2);

		if (alpha <= 0) return;

		float t = (ClientTickHandler.total() + 2000) * 0.005F;

		// 植物魔法原版通过 6 层不同旋转和颜色的星星 VBO 叠加实现深邃感
		drawStarLayer(starBuffer, quadIndices, starIndexCount, quadIndexBuffer, ms, VecHelper.rotateY(t * 3), new Vector4f(alpha, alpha, alpha, alpha), "Stars 1");
		drawStarLayer(starBuffer, quadIndices, starIndexCount, quadIndexBuffer, ms, VecHelper.rotateY(t * 1), new Vector4f(0.5f * alpha, alpha, alpha, alpha), "Stars 2");
		drawStarLayer(starBuffer, quadIndices, starIndexCount, quadIndexBuffer, ms, VecHelper.rotateY(t * 2), new Vector4f(alpha, 0.75f * alpha, 0.75f * alpha, alpha), "Stars 3");

		drawStarLayer(starBuffer, quadIndices, starIndexCount, quadIndexBuffer, ms, VecHelper.rotateZ(t * 3), new Vector4f(alpha, alpha, alpha, 0.25f * alpha), "Stars 4");
		drawStarLayer(starBuffer, quadIndices, starIndexCount, quadIndexBuffer, ms, VecHelper.rotateZ(t * 1), new Vector4f(0.5f * alpha, alpha, alpha, 0.25f * alpha), "Stars 5");
		drawStarLayer(starBuffer, quadIndices, starIndexCount, quadIndexBuffer, ms, VecHelper.rotateZ(t * 2), new Vector4f(alpha, 0.75f * alpha, 0.75f * alpha, 0.25f * alpha), "Stars 6");
	}

	/**
	 * 绘制单层星星
	 */
	private static void drawStarLayer(GpuBuffer starBuffer, GpuBuffer quadIndices, int starIndexCount, RenderSystem.AutoStorageIndexBuffer quadIndexBuffer, PoseStack ms, Quaternionf rotation, Vector4f color, String name) {
		Matrix4fStack matrix4fStack = RenderSystem.getModelViewStack();
		matrix4fStack.pushMatrix();
		matrix4fStack.mul(ms.last().pose());
		matrix4fStack.rotate(rotation);

		RenderPipeline renderPipeline = RenderPipelines.STARS;
		GpuTextureView gpuTextureView = Minecraft.getInstance().getMainRenderTarget().getColorTextureView();
		GpuTextureView gpuTextureView2 = Minecraft.getInstance().getMainRenderTarget().getDepthTextureView();
		
		var gpuBufferSlice = RenderSystem.getDynamicUniforms().writeTransform(matrix4fStack, color, new Vector3f(), new Matrix4f());
		
		RenderPass renderPass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> name, gpuTextureView, OptionalInt.empty(), gpuTextureView2, OptionalDouble.empty());

		try (renderPass) {
			renderPass.setPipeline(renderPipeline);
			RenderSystem.bindDefaultUniforms(renderPass);
			renderPass.setUniform("DynamicTransforms", gpuBufferSlice);
			renderPass.setVertexBuffer(0, starBuffer);
			renderPass.setIndexBuffer(quadIndices, quadIndexBuffer.type());
			renderPass.drawIndexed(0, 0, starIndexCount, 1);
		}
		matrix4fStack.popMatrix();
	}

	private static final Map<Identifier, RenderType> PLANET_RENDER_TYPES = new HashMap<>();

	private static RenderType getPlanetRenderType(Identifier texture) {
		return PLANET_RENDER_TYPES.computeIfAbsent(texture, id ->
			RenderType.create("gog_planet",
				RenderSetup.builder(RenderPipelines.CELESTIAL)
					.withTexture("Sampler0", id)
					.createRenderSetup()
			)
		);
	}
}
