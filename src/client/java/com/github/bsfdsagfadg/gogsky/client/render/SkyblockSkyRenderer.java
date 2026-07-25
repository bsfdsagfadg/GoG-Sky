package com.github.bsfdsagfadg.gogsky.client.render;

import com.github.bsfdsagfadg.gogsky.client.ClientTickHandler;
import com.github.bsfdsagfadg.gogsky.client.util.VecHelper;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ARGB;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.Random;

/**
 * 水晶花园天空渲染器 (1.21.4 - 旧渲染 API，仅行星/极光/彩虹)
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

	public static void renderExtra(PoseStack ms, MultiBufferSource bufferSource, ClientLevel world, float partialTicks, float insideVoid) {
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
			VertexConsumer consumer = bufferSource.getBuffer(RenderType.celestial(planetTextures[p]));
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

			VertexConsumer consumer = bufferSource.getBuffer(RenderType.celestial(textureSkybox));
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
					consumer.addVertex(mat, xp, yo + y0 + y, zp).setUv(ut, 1F).setColor(rayColor);
					consumer.addVertex(mat, xp, yo + y0, zp).setUv(ut, 0).setColor(rayColor);
				} else {
					consumer.addVertex(mat, xp, yo + y0, zp).setUv(ut, 0).setColor(rayColor);
					consumer.addVertex(mat, xp, yo + y0 + y, zp).setUv(ut, 1F).setColor(rayColor);
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

		VertexConsumer consumer = bufferSource.getBuffer(RenderType.celestial(textureRainbow));
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
				consumer.addVertex(mat, xp, yo + y0 + y, zp).setUv(ut, 1F).setColor(rainbowColor);
				consumer.addVertex(mat, xp, yo + y0, zp).setUv(ut, 0).setColor(rainbowColor);
			} else {
				consumer.addVertex(mat, xp, yo + y0, zp).setUv(ut, 0).setColor(rainbowColor);
				consumer.addVertex(mat, xp, yo + y0 + y, zp).setUv(ut, 1F).setColor(rainbowColor);
			}
		}
		ms.popPose();
	}
}