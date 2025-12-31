package com.github.bsfdsagfadg.gogsky.client;

import net.fabricmc.api.ClientModInitializer;

public class GogSkyClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ClientTickHandler.init();
	}
}