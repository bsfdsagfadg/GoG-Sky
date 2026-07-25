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
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ARGB;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.Random;

/**
 * 水晶花园天空渲染器
 * 适配自 Botania (植物魔法) 的 Garden of Glass 天空效果。
 * 针对 Minecraft 1.21.8 的 Blaze3D Next 渲染引擎进行了重写，使用了现代的缓冲渲染 API。
 */
public class SkyblockSkyRenderer {


	private static final ResourceLocation textureSkybox = ResourceLocation.fromNamespaceAndPath("gog-sky", "textures/environment/skybox.png");
	private static final ResourceLocation textureRainbow = ResourceLocation.fromNamespaceAndPath("gog-sky", "textures/environment/rainbow.png");
	private static final ResourceLocation[] planetTextures = new ResourceLocation[] {
			ResourceLocation.fromNamespaceAndPath("gog-sky", "textures/environment/planet0.png"),
			ResourceLocation.fromNamespaceAndPath("gog-sky", "textures/environment/planet1.png"),
			ResourceLocation.fromNamespaceAndPath("gog-sky", "textures/environment/planet2.png"),
			ResourceLocation.fromNamespaceAndPath("gog-sky", "textures/environment/planet3.png"),
			ResourceLocation.fromNamespaceAndPath("gog-sky", "textures/environment/planet4.png"),
			ResourceLocation.fromNamespaceAndPath("gog-sky", "textures/environment/planet5.png")
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
	public static void renderExtra(PoseStack ms, MultiBufferSource bufferSource, ClientLevel world, float partialTicks, float insideVoid) {
		com.mojang.blaze3d.opengl.GlStateManager._disableDepthTest();
		com.mojang.blaze3d.opengl.GlStateManager._depthMask(false);
		float rain = 1.0F - world.getRainLevel(partialTicks);
		float celAng = world.getTimeOfDay(partialTicks);
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
			VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(planetTextures[p]));
			Matrix4f mat = ms.last().pose();
			consumer.addVertex(mat, -scale, 100, -scale).setUv(0.0F, 0.0F).setColor(planetColor).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0, 1, 0);
			consumer.addVertex(mat, scale, 100, -scale).setUv(1.0F, 0.0F).setColor(planetColor).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0, 1, 0);
			consumer.addVertex(mat, scale, 100, scale).setUv(1.0F, 1.0F).setColor(planetColor).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0, 1, 0);
			consumer.addVertex(mat, -scale, 100, scale).setUv(0.0F, 1.0F).setColor(planetColor).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0, 1, 0);

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

			VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(textureSkybox));
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

		VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(textureRainbow));
		Matrix4f mat = ms.last().pose();
		for (int i = 0; i < angles; i++) {
			int j = i;
			if (i % 2 == 0) j--;

			float ang = j * anglePer;
			float xp = (float) Math.cos(ang * Math.PI / 180F) * scale;
			float zp = (float) Math.sin(ang * Math.PI / 180F) * scale;
			float yo = 0;
			float ut = ang * uPer;

			if (i % 2 == 0) {
				consumer.addVertex(mat, xp, yo + y0 + y, zp).setUv(ut, 1F).setColor(rainbowColor).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0, 1, 0);
				consumer.addVertex(mat, xp, yo + y0, zp).setUv(ut, 0).setColor(rainbowColor).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0, 1, 0);
			} else {
				consumer.addVertex(mat, xp, yo + y0, zp).setUv(ut, 0).setColor(rainbowColor).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0, 1, 0);
				consumer.addVertex(mat, xp, yo + y0 + y, zp).setUv(ut, 1F).setColor(rainbowColor).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0, 1, 0);
			}
		}
		ms.popPose();
		com.mojang.blaze3d.opengl.GlStateManager._depthMask(true);
		com.mojang.blaze3d.opengl.GlStateManager._enableDepthTest();
	}

	/**
	 * 渲染多层旋转星空
	 * 针对 1.21.8 的 WebGPU-like 架构，通过 RenderPass 实现多层渲染叠加。
	 */
	public static void renderStars(GpuBuffer starBuffer, GpuBuffer starIndices, int starIndexCount, RenderSystem.AutoStorageIndexBuffer starIndexBuffer, PoseStack ms, float partialTicks) {
		Minecraft mc = Minecraft.getInstance();
		float rain = 1.0F - mc.level.getRainLevel(partialTicks);
		float celAng = mc.level.getTimeOfDay(partialTicks);
		float effCelAng = celAng;
		if (celAng > 0.5) {
			effCelAng = 0.5F - (celAng - 0.5F);
		}
		float alpha = rain * Math.max(0.1F, effCelAng * 2);

		if (alpha <= 0) return;

		float t = (ClientTickHandler.total() + 2000) * 0.005F;

		// 植物魔法原版通过 6 层不同旋转和颜色的星星 VBO 叠加实现深邃感
		drawStarLayer(starBuffer, starIndices, starIndexCount, starIndexBuffer, ms, VecHelper.rotateY(t * 3), new Vector4f(alpha, alpha, alpha, alpha), "Stars 1");
		drawStarLayer(starBuffer, starIndices, starIndexCount, starIndexBuffer, ms, VecHelper.rotateY(t * 1), new Vector4f(0.5f * alpha, alpha, alpha, alpha), "Stars 2");
		drawStarLayer(starBuffer, starIndices, starIndexCount, starIndexBuffer, ms, VecHelper.rotateY(t * 2), new Vector4f(alpha, 0.75f * alpha, 0.75f * alpha, alpha), "Stars 3");

		drawStarLayer(starBuffer, starIndices, starIndexCount, starIndexBuffer, ms, VecHelper.rotateZ(t * 3), new Vector4f(alpha, alpha, alpha, 0.25f * alpha), "Stars 4");
		drawStarLayer(starBuffer, starIndices, starIndexCount, starIndexBuffer, ms, VecHelper.rotateZ(t * 1), new Vector4f(0.5f * alpha, alpha, alpha, 0.25f * alpha), "Stars 5");
		drawStarLayer(starBuffer, starIndices, starIndexCount, starIndexBuffer, ms, VecHelper.rotateZ(t * 2), new Vector4f(alpha, 0.75f * alpha, 0.75f * alpha, 0.25f * alpha), "Stars 6");
	}

	/**
	 * 绘制单层星星
	 */
	private static void drawStarLayer(GpuBuffer starBuffer, GpuBuffer starIndices, int starIndexCount, RenderSystem.AutoStorageIndexBuffer starIndexBuffer, PoseStack ms, Quaternionf rotation, Vector4f color, String name) {
		Matrix4fStack matrix4fStack = RenderSystem.getModelViewStack();
		matrix4fStack.pushMatrix();
		matrix4fStack.mul(ms.last().pose());
		matrix4fStack.rotate(rotation);

		RenderPipeline renderPipeline = RenderPipelines.STARS;
		GpuTextureView gpuTextureView = Minecraft.getInstance().getMainRenderTarget().getColorTextureView();
		GpuTextureView gpuTextureView2 = Minecraft.getInstance().getMainRenderTarget().getDepthTextureView();
		
		var gpuBufferSlice = RenderSystem.getDynamicUniforms().writeTransform(matrix4fStack, color, new Vector3f(), new Matrix4f(), 0.0F);
		
		RenderPass renderPass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> name, gpuTextureView, OptionalInt.empty(), gpuTextureView2, OptionalDouble.empty());

		try {
			renderPass.setPipeline(renderPipeline);
			RenderSystem.bindDefaultUniforms(renderPass);
			renderPass.setUniform("DynamicTransforms", gpuBufferSlice);
			renderPass.setVertexBuffer(0, starBuffer);
			renderPass.setIndexBuffer(starIndices, starIndexBuffer.type());
			renderPass.drawIndexed(0, 0, starIndexCount, 1);
		} finally {
			if (renderPass != null) {
				renderPass.close();
			}
		}
		matrix4fStack.popMatrix();
	}
}