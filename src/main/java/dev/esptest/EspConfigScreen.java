package dev.esptest;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * In-game settings screen. Opened from Mod Menu (config button) or with the "Open ESP Menu" key,
 * which can be rebound under Options > Controls > Key Binds.
 *
 * <p>Changes apply immediately; the config file is written when the screen is closed.
 */
public class EspConfigScreen extends Screen {

	private static final int COLUMN_WIDTH = 150;
	private static final int GAP = 10;
	private static final int ROW_HEIGHT = 24;

	private static final int[] RANGE_PRESETS = {32, 64, 96, 128, 192, 256, 384, 512};
	private static final int RANGE_STEP = 16;
	private static final int RANGE_MIN = 16;
	private static final int RANGE_MAX = 512;

	private final Screen parent;
	private Button rangeButton;
	private int titleX;
	private int titleY;

	public EspConfigScreen(Screen parent) {
		super(Component.literal("ESP Test"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int left = this.width / 2 - COLUMN_WIDTH - GAP / 2;
		int right = this.width / 2 + GAP / 2;
		int fullWidth = COLUMN_WIDTH * 2 + GAP;
		int y = Math.max(32, this.height / 2 - 90);

		this.titleX = left;
		this.titleY = y - 18;

		toggle(left, y, "ESP", () -> cfg().enabled, v -> cfg().enabled = v);
		this.addRenderableWidget(Button.builder(modeLabel(), button -> {
			cfg().colorMode = cfg().colorMode == EspConfig.ColorMode.CATEGORY
					? EspConfig.ColorMode.VISIBILITY
					: EspConfig.ColorMode.CATEGORY;
			button.setMessage(modeLabel());
			EspManager.refresh();
		}).bounds(right, y, COLUMN_WIDTH, 20).build());
		y += ROW_HEIGHT;

		toggle(left, y, "Players", () -> cfg().players, v -> cfg().players = v);
		toggle(right, y, "Hostile mobs", () -> cfg().hostile, v -> cfg().hostile = v);
		y += ROW_HEIGHT;

		toggle(left, y, "Passive mobs", () -> cfg().passive, v -> cfg().passive = v);
		toggle(right, y, "Items", () -> cfg().items, v -> cfg().items = v);
		y += ROW_HEIGHT;

		toggle(left, y, "Other entities", () -> cfg().other, v -> cfg().other = v);
		toggle(right, y, "Show myself", () -> cfg().includeSelf, v -> cfg().includeSelf = v);
		y += ROW_HEIGHT;

		toggle(left, y, "Invisible", () -> cfg().includeInvisible, v -> cfg().includeInvisible = v);
		toggle(right, y, "Vanilla mobs only", () -> cfg().vanillaEntitiesOnly, v -> cfg().vanillaEntitiesOnly = v);
		y += ROW_HEIGHT;

		// Range row:  [-]  [ Range: 128 blocks (click = next preset) ]  [+]
		this.addRenderableWidget(Button.builder(Component.literal("-"), button -> setRange(cfg().maxDistance - RANGE_STEP))
				.bounds(left, y, 20, 20).build());

		this.rangeButton = Button.builder(rangeLabel(), button -> setRange(nextPreset(cfg().maxDistance)))
				.bounds(left + 24, y, fullWidth - 48, 20).build();
		this.addRenderableWidget(this.rangeButton);

		this.addRenderableWidget(Button.builder(Component.literal("+"), button -> setRange(cfg().maxDistance + RANGE_STEP))
				.bounds(left + fullWidth - 20, y, 20, 20).build());
		y += ROW_HEIGHT;

		this.addRenderableWidget(Button.builder(Component.literal("Colors..."),
				button -> this.minecraft.setScreen(new EspColorScreen(this))).bounds(left, y, COLUMN_WIDTH, 20).build());
		this.addRenderableWidget(Button.builder(Component.literal("Done"), button -> this.onClose())
				.bounds(right, y, COLUMN_WIDTH, 20).build());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		// font, text, x, y, colour (ARGB), shadow
		graphics.text(this.font, "ESP Test - settings", this.titleX, this.titleY, 0xFFFFFFFF, true);
	}

	@Override
	public void onClose() {
		EspManager.save();
		this.minecraft.setScreen(this.parent);
	}

	// ---------------------------------------------------------------------------------------

	private void toggle(int x, int y, String name, BooleanSupplier getter, Consumer<Boolean> setter) {
		this.addRenderableWidget(Button.builder(stateLabel(name, getter.getAsBoolean()), button -> {
			boolean value = !getter.getAsBoolean();
			setter.accept(value);
			button.setMessage(stateLabel(name, value));
			EspManager.refresh();
		}).bounds(x, y, COLUMN_WIDTH, 20).build());
	}

	private void setRange(double value) {
		double clamped = Math.max(RANGE_MIN, Math.min(RANGE_MAX, Math.round(value)));
		cfg().maxDistance = clamped;
		EspManager.refresh();
		if (this.rangeButton != null) {
			this.rangeButton.setMessage(rangeLabel());
		}
	}

	private static double nextPreset(double current) {
		for (int preset : RANGE_PRESETS) {
			if (preset > current + 0.5) {
				return preset;
			}
		}
		return RANGE_PRESETS[0];
	}

	private static EspConfig cfg() {
		return EspManager.config();
	}

	private static Component stateLabel(String name, boolean on) {
		return Component.literal(name + ": " + (on ? "ON" : "OFF"));
	}

	private static Component rangeLabel() {
		return Component.literal("Range: " + (int) cfg().maxDistance + " blocks");
	}

	private static Component modeLabel() {
		boolean visibility = cfg().colorMode == EspConfig.ColorMode.VISIBILITY;
		return Component.literal("Mode: " + (visibility ? "Line of sight" : "By type"));
	}
}
