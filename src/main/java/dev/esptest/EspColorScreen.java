package dev.esptest;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Colour sub-screen of {@link EspConfigScreen}: click a button to step through the 16 chat colours.
 * Any other RRGGBB value can still be typed into config/esptest.json (shown here as "#RRGGBB").
 */
public class EspColorScreen extends Screen {

	private record Swatch(String name, int rgb) {
	}

	private static final Swatch[] PALETTE = {
			new Swatch("Black", 0x000000),
			new Swatch("Dark Blue", 0x0000AA),
			new Swatch("Dark Green", 0x00AA00),
			new Swatch("Dark Aqua", 0x00AAAA),
			new Swatch("Dark Red", 0xAA0000),
			new Swatch("Dark Purple", 0xAA00AA),
			new Swatch("Gold", 0xFFAA00),
			new Swatch("Gray", 0xAAAAAA),
			new Swatch("Dark Gray", 0x555555),
			new Swatch("Blue", 0x5555FF),
			new Swatch("Green", 0x55FF55),
			new Swatch("Aqua", 0x55FFFF),
			new Swatch("Red", 0xFF5555),
			new Swatch("Light Purple", 0xFF55FF),
			new Swatch("Yellow", 0xFFFF55),
			new Swatch("White", 0xFFFFFF)
	};

	private static final int COLUMN_WIDTH = 150;
	private static final int GAP = 10;
	private static final int ROW_HEIGHT = 24;

	private final Screen parent;
	private int titleX;
	private int titleY;

	public EspColorScreen(Screen parent) {
		super(Component.literal("ESP Colors"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int left = this.width / 2 - COLUMN_WIDTH - GAP / 2;
		int right = this.width / 2 + GAP / 2;
		int y = Math.max(32, this.height / 2 - 60);

		this.titleX = left;
		this.titleY = y - 18;

		slot(left, y, "Players", () -> cfg().colorPlayers, v -> cfg().colorPlayers = v);
		slot(right, y, "Hostile mobs", () -> cfg().colorHostile, v -> cfg().colorHostile = v);
		y += ROW_HEIGHT;

		slot(left, y, "Passive mobs", () -> cfg().colorPassive, v -> cfg().colorPassive = v);
		slot(right, y, "Items", () -> cfg().colorItems, v -> cfg().colorItems = v);
		y += ROW_HEIGHT;

		slot(left, y, "Other", () -> cfg().colorOther, v -> cfg().colorOther = v);
		slot(right, y, "Visible", () -> cfg().colorVisible, v -> cfg().colorVisible = v);
		y += ROW_HEIGHT;

		slot(left, y, "Behind blocks", () -> cfg().colorHidden, v -> cfg().colorHidden = v);
		this.addRenderableWidget(Button.builder(Component.literal("Back"), button -> this.onClose())
				.bounds(right, y, COLUMN_WIDTH, 20).build());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		graphics.text(this.font, "ESP Test - colors (Visible / Behind blocks apply to the line-of-sight mode)",
				this.titleX, this.titleY, 0xFFFFFFFF, true);
	}

	@Override
	public void onClose() {
		EspManager.save();
		this.minecraft.setScreen(this.parent);
	}

	// ---------------------------------------------------------------------------------------

	private void slot(int x, int y, String name, Supplier<String> getter, Consumer<String> setter) {
		this.addRenderableWidget(Button.builder(label(name, getter.get()), button -> {
			int next = (indexOf(getter.get()) + 1) % PALETTE.length;
			setter.accept(String.format("%06X", PALETTE[next].rgb()));
			button.setMessage(label(name, getter.get()));
			EspManager.refresh();
		}).bounds(x, y, COLUMN_WIDTH, 20).build());
	}

	private static EspConfig cfg() {
		return EspManager.config();
	}

	private static int indexOf(String hex) {
		int rgb = EspManager.parseRgb(hex);
		for (int i = 0; i < PALETTE.length; i++) {
			if (PALETTE[i].rgb() == rgb) {
				return i;
			}
		}
		return -1;
	}

	private static Component label(String name, String hex) {
		int index = indexOf(hex);
		String value = index >= 0 ? PALETTE[index].name() : "#" + (hex == null ? "?" : hex.trim().toUpperCase());
		return Component.literal(name + ": " + value);
	}
}
