package dev.esptest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Owns the config and answers one question for the mixins: "should this entity get an ESP outline,
 * and in which colour?" ({@link #outlineColor(Entity)}).
 *
 * <p>Nothing in here touches the network or the server: the ESP is purely a client-side render change.
 */
public final class EspManager {

	public enum Category { PLAYER, HOSTILE, PASSIVE, ITEM, OTHER }

	private static final Logger LOGGER = LoggerFactory.getLogger(EspTestClient.MOD_ID);
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path CONFIG_PATH =
			FabricLoader.getInstance().getConfigDir().resolve(EspTestClient.MOD_ID + ".json");

	/** entity id -> "line of sight to the local player", valid for the current client tick only. */
	private static final Map<Integer, Boolean> LOS_CACHE = new ConcurrentHashMap<>();

	private static volatile EspConfig config = new EspConfig();

	// Parsed ARGB colours (alpha = 0xFF), rebuilt by rebuild().
	private static int colPlayers;
	private static int colHostile;
	private static int colPassive;
	private static int colItems;
	private static int colOther;
	private static int colVisible;
	private static int colHidden;
	private static double maxDistanceSq = 128.0 * 128.0;

	private static boolean failureLogged;

	private EspManager() {
	}

	// ---------------------------------------------------------------------------------------
	// config handling
	// ---------------------------------------------------------------------------------------

	public static EspConfig config() {
		return config;
	}

	/** Reads config/esptest.json (creating it with defaults if needed). */
	public static void load() {
		EspConfig loaded = null;

		if (Files.isRegularFile(CONFIG_PATH)) {
			try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
				loaded = GSON.fromJson(reader, EspConfig.class);
			} catch (IOException | RuntimeException e) {
				LOGGER.error("Could not read {} - falling back to defaults", CONFIG_PATH, e);
			}
		}

		config = loaded != null ? loaded : new EspConfig();
		rebuild();
		save(); // also writes fields that were added in newer versions of this mod
	}

	public static void save() {
		try {
			Files.createDirectories(CONFIG_PATH.getParent());
			try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
				GSON.toJson(config, writer);
			}
		} catch (IOException | RuntimeException e) {
			LOGGER.error("Could not write {}", CONFIG_PATH, e);
		}
	}

	/** Call after changing a config value: re-parses colours / distance and persists the file. */
	public static void changed() {
		rebuild();
		save();
	}

	/** Re-parses colours / distance after an in-memory change (used by the settings screen; does not write the file). */
	public static void refresh() {
		rebuild();
	}

	public static boolean toggle() {
		config.enabled = !config.enabled;
		save();
		return config.enabled;
	}

	private static void rebuild() {
		EspConfig c = config;
		if (c.colorMode == null) {
			c.colorMode = EspConfig.ColorMode.CATEGORY;
		}

		colPlayers = argb(c.colorPlayers, 0xFF5555);
		colHostile = argb(c.colorHostile, 0xFFAA00);
		colPassive = argb(c.colorPassive, 0x55FF55);
		colItems = argb(c.colorItems, 0xFFFF55);
		colOther = argb(c.colorOther, 0xAAAAAA);
		colVisible = argb(c.colorVisible, 0x55FF55);
		colHidden = argb(c.colorHidden, 0xFF5555);

		double distance = Math.max(1.0, c.maxDistance);
		maxDistanceSq = distance * distance;
	}

	/** @return 0xRRGGBB, or -1 if the text is not a valid hex colour (optional leading #, up to 6 digits). */
	public static int parseRgb(String hex) {
		if (hex == null) {
			return -1;
		}
		String s = hex.trim();
		if (s.startsWith("#")) {
			s = s.substring(1);
		}
		if (s.isEmpty() || s.length() > 6) {
			return -1;
		}
		int value = 0;
		for (int i = 0; i < s.length(); i++) {
			int digit = Character.digit(s.charAt(i), 16);
			if (digit < 0) {
				return -1;
			}
			value = value * 16 + digit;
		}
		return value;
	}

	private static int argb(String hex, int fallbackRgb) {
		int rgb = parseRgb(hex);
		return 0xFF000000 | (rgb >= 0 ? rgb : fallbackRgb);
	}

	// ---------------------------------------------------------------------------------------
	// per-tick housekeeping
	// ---------------------------------------------------------------------------------------

	public static void onClientTick() {
		LOS_CACHE.clear();
	}

	// ---------------------------------------------------------------------------------------
	// the actual decision (called from the mixins, once or twice per visible entity per frame)
	// ---------------------------------------------------------------------------------------

	/**
	 * @return an opaque ARGB outline colour, or {@code 0} if this entity must not be highlighted.
	 */
	public static int outlineColor(Entity entity) {
		EspConfig c = config;
		if (!c.enabled || entity == null) {
			return 0;
		}

		try {
			Minecraft mc = Minecraft.getInstance();
			if (mc.player == null) {
				return 0;
			}

			boolean self = entity == mc.player;
			if (self && !c.includeSelf) {
				return 0;
			}
			if (!c.includeInvisible && entity.isInvisible()) {
				return 0;
			}

			EntityType<?> type = entity.getType();
			if (c.vanillaEntitiesOnly
					&& !"minecraft".equals(BuiltInRegistries.ENTITY_TYPE.getKey(type).getNamespace())) {
				return 0;
			}

			Category category = classify(entity, type);
			if (!isEnabled(c, category)) {
				return 0;
			}

			if (!self && mc.player.distanceToSqr(entity) > maxDistanceSq) {
				return 0;
			}

			if (c.colorMode == EspConfig.ColorMode.VISIBILITY) {
				return hasLineOfSight(mc, entity) ? colVisible : colHidden;
			}
			return categoryColor(category);
		} catch (RuntimeException e) {
			// This runs inside the render loop - never let a bug here take the game down.
			if (!failureLogged) {
				failureLogged = true;
				LOGGER.error("ESP check failed, entity skipped (further errors are not logged)", e);
			}
			return 0;
		}
	}

	private static boolean hasLineOfSight(Minecraft mc, Entity entity) {
		return LOS_CACHE.computeIfAbsent(entity.getId(), id -> mc.player.hasLineOfSight(entity));
	}

	private static Category classify(Entity entity, EntityType<?> type) {
		if (type == EntityType.PLAYER) {
			return Category.PLAYER;
		}
		if (type == EntityType.ITEM || type == EntityType.EXPERIENCE_ORB) {
			return Category.ITEM;
		}
		if (type == EntityType.ARMOR_STAND) {
			return Category.OTHER;
		}
		if (type.getCategory() == MobCategory.MONSTER) {
			return Category.HOSTILE;
		}
		if (entity instanceof LivingEntity) {
			// animals, villagers, golems, fish, bats, ...
			return Category.PASSIVE;
		}
		return Category.OTHER;
	}

	private static boolean isEnabled(EspConfig c, Category category) {
		return switch (category) {
			case PLAYER -> c.players;
			case HOSTILE -> c.hostile;
			case PASSIVE -> c.passive;
			case ITEM -> c.items;
			case OTHER -> c.other;
		};
	}

	private static int categoryColor(Category category) {
		return switch (category) {
			case PLAYER -> colPlayers;
			case HOSTILE -> colHostile;
			case PASSIVE -> colPassive;
			case ITEM -> colItems;
			case OTHER -> colOther;
		};
	}
}
