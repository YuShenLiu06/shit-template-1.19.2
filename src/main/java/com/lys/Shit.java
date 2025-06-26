package com.lys;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

public class Shit implements ModInitializer {
	@Override
	public void onInitialize() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
				ShitCommand.register(dispatcher)
		);

		System.out.println("[ShitMod] 模组已加载");
	}
}