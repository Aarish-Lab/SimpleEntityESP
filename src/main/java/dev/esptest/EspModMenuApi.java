package dev.esptest;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/**
 * Mod Menu entrypoint ("modmenu" in fabric.mod.json): adds the config button for this mod.
 * Only Mod Menu ever loads this class, so Mod Menu stays an optional dependency.
 */
public final class EspModMenuApi implements ModMenuApi {

	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return EspConfigScreen::new;
	}
}
