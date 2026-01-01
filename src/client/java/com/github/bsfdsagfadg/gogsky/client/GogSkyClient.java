package com.github.bsfdsagfadg.gogsky.client;

import net.fabricmc.api.ClientModInitializer;

/**
 * 客户端模组初始化入口
 */
public class GogSkyClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// 初始化 Tick 处理器
		ClientTickHandler.init();
	}
}