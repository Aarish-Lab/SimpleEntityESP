package dev.esptest;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class EspTestClient implements ClientModInitializer {

	public static final String MOD_ID = "esptest";

	private static KeyMapping toggleKey;
	private static KeyMapping menuKey;

	@Override
	public void onInitializeClient() {
		EspManager.load();

		// Both keys show up (and can be rebound) under Options > Controls > Key Binds > "ESP Test".
		KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));

		toggleKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.esptest.toggle",
				InputConstants.Type.KEYSYM,
				InputConstants.KEY_G,
				category
		));

		menuKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.esptest.menu",
				InputConstants.Type.KEYSYM,
				InputConstants.KEY_O,
				category
		));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			EspManager.onClientTick();

			while (toggleKey.consumeClick()) {
				boolean on = EspManager.toggle();
				if (client.player != null) {
					client.player.sendSystemMessage(Component.literal("[ESP] " + (on ? "enabled" : "disabled")));
				}
			}

			boolean openMenu = false;
			while (menuKey.consumeClick()) {
				openMenu = true;
			}
			if (openMenu) {
				client.setScreen(new EspConfigScreen(null));
			}
		});

		// /esp ... (client-side command, nothing is sent to the server)
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, buildContext) -> EspCommands.register(dispatcher));
	}
}
