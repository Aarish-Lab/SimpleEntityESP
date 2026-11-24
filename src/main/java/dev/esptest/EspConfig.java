package dev.esptest;

/**
 * Plain data object that is (de)serialised with Gson to {@code config/esptest.json}.
 * Every field can be edited by hand, then applied in-game with {@code /esp reload}.
 */
public final class EspConfig {

	public enum ColorMode {
		/** One colour per entity category (players, hostile, ...). */
		CATEGORY,
		/** Visible entities get {@link #colorVisible}, entities behind blocks get {@link #colorHidden}. */
		VISIBILITY
	}

	/** Master switch (also bound to the toggle key). */
	public boolean enabled = true;

	/** Entities further away than this many blocks are not highlighted. */
	public double maxDistance = 128.0;

	// ---- which categories get an outline -------------------------------------------------
	public boolean players = true;
	public boolean hostile = true;
	public boolean passive = false;
	public boolean items = false;
	/** Armor stands, boats, minecarts, projectiles, ... everything that is not covered above. */
	public boolean other = false;

	// ---- filters -------------------------------------------------------------------------
	public boolean includeSelf = false;
	public boolean includeInvisible = true;
	/**
	 * Only outline entities from the "minecraft" namespace. Entities added by other mods can use render
	 * types that have no outline variant, which makes vanilla throw while drawing the outline.
	 */
	public boolean vanillaEntitiesOnly = true;

	public ColorMode colorMode = ColorMode.CATEGORY;

	// ---- colours, RRGGBB hex ---------------------------------------------------------------
	public String colorPlayers = "FF5555";
	public String colorHostile = "FFAA00";
	public String colorPassive = "55FF55";
	public String colorItems = "FFFF55";
	public String colorOther = "AAAAAA";
	public String colorVisible = "55FF55";
	public String colorHidden = "FF5555";
}
