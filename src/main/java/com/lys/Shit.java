package com.lys;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

public class Shit implements ModInitializer {
	@Override
	public void onInitialize() {
		// 使用v2版本的CommandRegistrationCallback
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
				ShitCommand.register(dispatcher)
		);
	}
}