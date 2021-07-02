package me.pugabyte.nexus.utils;

import io.papermc.paper.text.PaperComponents;
import lombok.experimental.UtilityClass;
import me.lexikiq.HasUniqueId;
import me.pugabyte.nexus.framework.interfaces.IsColoredAndNamed;
import me.pugabyte.nexus.framework.interfaces.IsColoredAndNicknamed;
import net.kyori.adventure.identity.Identified;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import net.md_5.bungee.api.ChatColor;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@UtilityClass
public class AdventureUtils {
	private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.builder().extractUrls().hexColors().build();
	private static final LegacyComponentSerializer LEGACY_AMPERSAND_SERIALIZER = LegacyComponentSerializer.builder().extractUrls().hexColors().character('&').build();
	public static final Title.Times BASIC_TIMES = Title.Times.of(TimeUtils.Time.SECOND.duration(1, 2), TimeUtils.Time.SECOND.duration(5), TimeUtils.Time.SECOND.duration(1, 2));

	public static Component stripColor(ComponentLike componentLike) {
		Component component = componentLike.asComponent();
		component = component.style(Style.empty());
		if (component instanceof TranslatableComponent tComponent) {
			component = tComponent.args(stripColor(tComponent.args()));
		}
		return component.children(stripColor(component.children()));
	}

	public static List<Component> stripColor(List<Component> components) {
		return components.stream().map(AdventureUtils::stripColor).collect(Collectors.toList());
	}

	public static String asPlainText(ComponentLike component) {
		return PaperComponents.plainSerializer().serialize(component.asComponent());
	}

	public static String asLegacyText(ComponentLike component) {
		return PaperComponents.legacySectionSerializer().serialize(component.asComponent());
	}

	public static Component fromLegacyText(String string) {
		return LEGACY_SERIALIZER.deserialize(string);
	}

	public static Component fromLegacyAmpersandText(String string) {
		return LEGACY_AMPERSAND_SERIALIZER.deserialize(string);
	}

	public static Identity identityOf(Identified object) {
		return object.identity();
	}

	public static Identity identityOf(UUID uuid) {
		return Identity.identity(uuid);
	}

	public static Identity identityOf(HasUniqueId player) {
		return identityOf(player.getUniqueId());
	}

	public static TextComponent getPrefix(String prefix) {
		return Component.text("", NamedTextColor.DARK_AQUA)
				.append(Component.text("[", NamedTextColor.DARK_GRAY, TextDecoration.BOLD))
				.append(Component.text(prefix, NamedTextColor.YELLOW))
				.append(Component.text("]", NamedTextColor.DARK_GRAY, TextDecoration.BOLD))
				.append(Component.text(" ", NamedTextColor.DARK_AQUA));
	}

	@Contract("null -> null; !null -> !null")
	public static TextColor textColorOf(@Nullable Color color) {
		if (color == null)
			return null;
		return TextColor.color(color.getRGB());
	}

	@Contract("null -> null; !null -> !null")
	public static TextColor textColorOf(@Nullable org.bukkit.Color color) {
		if (color == null)
			return null;
		return TextColor.color(color.asRGB());
	}

	@Contract("null -> null; !null -> !null")
	public static TextColor textColorOf(@Nullable ChatColor color) {
		if (color == null)
			return null;
		return textColorOf(color.getColor());
	}

	/**
	 * Parses a hexadecimal number
	 * @param string number in the format "#FFFFFF" (# optional)
	 * @throws IllegalArgumentException string contained an invalid hexadecimal number
	 * @return corresponding text color
	 */
	@Contract("null -> null; !null -> !null")
	public static TextColor textColorOf(@Nullable String string) throws IllegalArgumentException {
		if (string == null)
			return null;
		if (string.startsWith("#"))
			string = string.substring(1);
		try {
			return TextColor.color(Integer.parseInt(string, 16));
		} catch ( NumberFormatException ex ) {
			throw new IllegalArgumentException("Illegal hex string " + string);
		}
	}

	@NotNull
	public static TextComponent colorText(@Nullable ChatColor color, @NotNull String text) {
		return Component.text(text, textColorOf(color));
	}

	@NotNull
	public static TextComponent colorText(@Nullable Color color, @NotNull String text) {
		return Component.text(text, textColorOf(color));
	}

	@NotNull
	public static TextComponent colorText(@Nullable org.bukkit.Color color, @NotNull String text) {
		return Component.text(text, textColorOf(color));
	}

	/**
	 * Parses a hexadecimal number
	 * @param color number in the format "#FFFFFF" (# optional)
	 * @param text text to color
	 * @throws IllegalArgumentException string contained an invalid hexadecimal number
	 * @return corresponding text color
	 */
	@NotNull
	public static TextComponent colorText(@Nullable String color, @NotNull String text) throws IllegalArgumentException {
		return Component.text(text, textColorOf(color));
	}

	@NotNull
	public static TextComponent colorText(@NotNull IsColoredAndNamed coloredAndNamed) {
		return Component.text(coloredAndNamed.getName(), coloredAndNamed.colored());
	}

	@NotNull
	public static TextComponent colorText(@NotNull IsColoredAndNicknamed coloredAndNicknamed) {
		return Component.text(coloredAndNicknamed.getNickname(), coloredAndNicknamed.colored());
	}

	/**
	 * Returns a component that has separated the input list of components with commas.
	 * <p>
	 * If the input list is empty, a blank component will be returned.
	 * <br>
	 * If the list has one item, it will be returned.
	 * <br>
	 * If the list has two items, "[component1] and [component2]" will be returned.
	 * <br>
	 * Else, "[component1], [component2], [...], and [componentX]" will be returned.
	 * @param components components to separate by commas.
	 * @param color optional color to use for the commas
	 * @return a formatted TextComponent
	 */
	public static TextComponent commaJoinText(List<? extends ComponentLike> components, @Nullable TextColor color) {
		TextComponent component = Component.text("", color);

		if (components.isEmpty())
			return component;
		if (components.size() == 1)
			return component.append(components.get(0));
		if (components.size() == 2)
			return component.append(components.get(0))
				    .append(Component.text(" and "))
					.append(components.get(1));

		TextComponent.Builder builder = component.toBuilder();
		for (int i = 0; i < components.size(); i++) {
			builder.append(components.get(i));
			if (i < components.size()-2)
				builder.append(Component.text(", "));
			else if (i == components.size()-2)
				builder.append(Component.text(", and "));
		}
		return builder.build();
	}

	/**
	 * Returns a component that has separated the input list of components with commas.
	 * <p>
	 * If the input list is empty, a blank component will be returned.
	 * <br>
	 * If the list has one item, it will be returned.
	 * <br>
	 * If the list has two items, "[component1] and [component2]" will be returned.
	 * <br>
	 * Else, "[component1], [component2], [...], and [componentX]" will be returned.
	 * @param components components to separate by commas.
	 * @return a formatted TextComponent
	 */
	public static TextComponent commaJoinText(List<? extends ComponentLike> components) {
		return commaJoinText(components, null);
	}

	/**
	 * Maps a list of {@link ComponentLike} to {@link Component}
	 */
	public static List<Component> asComponentList(List<? extends ComponentLike> components) {
		return components.stream().map(ComponentLike::asComponent).collect(Collectors.toList());
	}

	/**
	 * Maps a list of {@link ComponentLike} to {@link Component}
	 */
	public static List<Component> asComponentList(ComponentLike... components) {
		return Arrays.stream(components).map(ComponentLike::asComponent).collect(Collectors.toList());
	}
}
