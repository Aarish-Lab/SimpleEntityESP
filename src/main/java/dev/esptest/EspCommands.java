package dev.esptest;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;

import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * {@code /esp} client command tree.
 *
 * <pre>
 * /esp                         status
 * /esp toggle
 * /esp reload                  re-read config/esptest.json
 * /esp range &lt;blocks&gt;
 * /esp mode category|visibility
 * /esp players|hostile|passive|items|other [true|false]
 * /esp self|invisible|vanillaonly [true|false]
 * </pre>
 */
final class EspCommands {

	// Arguments use Brigadier's own RequiredArgumentBuilder.argument(), so the only Fabric helper needed is ClientCommands.literal().
	private EspCommands() {
	}

	static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
		LiteralArgumentBuilder<FabricClientCommandSource> root = ClientCommands.literal("esp")
				.executes(ctx -> {
					status(ctx.getSource());
					return 1;
				})
				.then(ClientCommands.literal("toggle").executes(ctx -> {
					boolean on = EspManager.toggle();
					ctx.getSource().sendFeedback(Component.literal("ESP " + (on ? "enabled" : "disabled")));
					return 1;
				}))
				.then(ClientCommands.literal("reload").executes(ctx -> {
					EspManager.load();
					ctx.getSource().sendFeedback(Component.literal("ESP config reloaded"));
					return 1;
				}))
				.then(ClientCommands.literal("range").then(
						RequiredArgumentBuilder.<FabricClientCommandSource, Double>argument("blocks", DoubleArgumentType.doubleArg(1.0, 1024.0)).executes(ctx -> {
							double blocks = DoubleArgumentType.getDouble(ctx, "blocks");
							EspManager.config().maxDistance = blocks;
							EspManager.changed();
							ctx.getSource().sendFeedback(Component.literal("ESP range = " + blocks + " blocks"));
							return 1;
						})))
				.then(ClientCommands.literal("mode")
						.then(ClientCommands.literal("category")
								.executes(ctx -> setMode(ctx.getSource(), EspConfig.ColorMode.CATEGORY)))
						.then(ClientCommands.literal("visibility")
								.executes(ctx -> setMode(ctx.getSource(), EspConfig.ColorMode.VISIBILITY))));

		flag(root, "players", c -> c.players, (c, v) -> c.players = v);
		flag(root, "hostile", c -> c.hostile, (c, v) -> c.hostile = v);
		flag(root, "passive", c -> c.passive, (c, v) -> c.passive = v);
		flag(root, "items", c -> c.items, (c, v) -> c.items = v);
		flag(root, "other", c -> c.other, (c, v) -> c.other = v);
		flag(root, "self", c -> c.includeSelf, (c, v) -> c.includeSelf = v);
		flag(root, "invisible", c -> c.includeInvisible, (c, v) -> c.includeInvisible = v);
		flag(root, "vanillaonly", c -> c.vanillaEntitiesOnly, (c, v) -> c.vanillaEntitiesOnly = v);

		dispatcher.register(root);
	}

	/** Adds {@code /esp <name>} (query) and {@code /esp <name> <true|false>} (set). */
	private static void flag(LiteralArgumentBuilder<FabricClientCommandSource> root, String name,
							 Function<EspConfig, Boolean> getter, BiConsumer<EspConfig, Boolean> setter) {
		root.then(ClientCommands.literal(name)
				.executes(ctx -> {
					boolean value = getter.apply(EspManager.config());
					ctx.getSource().sendFeedback(Component.literal("ESP " + name + " = " + value));
					return 1;
				})
				.then(RequiredArgumentBuilder.<FabricClientCommandSource, Boolean>argument("value", BoolArgumentType.bool()).executes(ctx -> {
					boolean value = BoolArgumentType.getBool(ctx, "value");
					setter.accept(EspManager.config(), value);
					EspManager.changed();
					ctx.getSource().sendFeedback(Component.literal("ESP " + name + " = " + value));
					return 1;
				})));
	}

	private static int setMode(FabricClientCommandSource source, EspConfig.ColorMode mode) {
		EspManager.config().colorMode = mode;
		EspManager.changed();
		source.sendFeedback(Component.literal("ESP colour mode = " + mode.name().toLowerCase()));
		return 1;
	}

	private static void status(FabricClientCommandSource source) {
		EspConfig c = EspManager.config();
		source.sendFeedback(Component.literal(
				"ESP " + (c.enabled ? "ON" : "OFF") + " | range " + c.maxDistance + " | mode "
						+ c.colorMode.name().toLowerCase()));
		source.sendFeedback(Component.literal(
				"players=" + c.players + " hostile=" + c.hostile + " passive=" + c.passive
						+ " items=" + c.items + " other=" + c.other));
		source.sendFeedback(Component.literal(
				"self=" + c.includeSelf + " invisible=" + c.includeInvisible
						+ " vanillaonly=" + c.vanillaEntitiesOnly));
	}
}
