package me.pugabyte.nexus.utils;

import lombok.Getter;
import lombok.NoArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TextComponent.Builder;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEventSource;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static me.pugabyte.nexus.utils.StringUtils.colorize;

@SuppressWarnings({"unused", "UnusedReturnValue"})
@NoArgsConstructor
public class JsonBuilder implements ComponentLike {
	@NonNull private Builder builder = Component.text();
	@NonNull private final Builder result = Component.text();

	private final List<String> lore = new ArrayList<>();
	private boolean loreize = true;

	/**
	 * Helper boolean that does not affect anything within the builder itself.
	 * Can be used by external methods to determine if the builder has been setup.
	 * To set this boolean to true, use {@link #initialize()}
	 */
	@Getter
	private boolean initialized = false;

	/**
	 * Creates a new builder with its color set
	 */
	public JsonBuilder(@Nullable TextColor color) {
		color(color);
	}

	/**
	 * Creates a new builder with its color set
	 */
	public JsonBuilder(@Nullable ChatColor color) {
		color(color);
	}

	/**
	 * Creates a new builder with its color set
	 */
	public JsonBuilder(@Nullable Color color) {
		color(color);
	}

	/**
	 * Creates a new builder with its color set
	 */
	public JsonBuilder(@Nullable org.bukkit.Color color) {
		color(color);
	}

	/**
	 * Creates a new builder with its color and text decorations set
	 */
	public JsonBuilder(@Nullable TextColor color, @NotNull TextDecoration... decorations) {
		color(color);
		decorate(decorations);
	}

	/**
	 * Creates a new builder with its color and text decorations set
	 */
	public JsonBuilder(@Nullable ChatColor color, @NotNull TextDecoration... decorations) {
		color(color);
		decorate(decorations);
	}

	/**
	 * Creates a new builder with its color and text decorations set
	 */
	public JsonBuilder(@Nullable Color color, @NotNull TextDecoration... decorations) {
		color(color);
		decorate(decorations);
	}

	/**
	 * Creates a new builder with its color and text decorations set
	 */
	public JsonBuilder(@Nullable org.bukkit.Color color, @NotNull TextDecoration... decorations) {
		color(color);
		decorate(decorations);
	}

	/**
	 * Creates a new builder with its text decorations set
	 */
	public JsonBuilder(@NotNull TextDecoration... decorations) {
		decorate(decorations);
	}

	/**
	 * Converts the input text to a component and appends it to the internal builder
	 * <p>Note: this does not apply any text, color, or formatting changes to the builder itself
	 */
	public JsonBuilder(@Nullable String formattedText) {
		next(formattedText);
	}

	/**
	 * Converts the input to a component and appends it to the internal builder. Also copies {@link #loreize} and {@link #initialized}.
	 * <p>Note: this does not apply any text, color, or formatting changes to the builder itself
	 */
	public JsonBuilder(@Nullable JsonBuilder json) {
		if (json != null) {
			next(json);
			loreize = json.loreize;
			initialized = json.initialized;
		}
	}

	/**
	 * Converts the input to a component and appends it to the internal builder
	 * <p>Note: this does not apply any text, color, or formatting changes to the builder itself
	 */
	public JsonBuilder(@Nullable ComponentLike component) {
		next(component);
	}

	/**
	 * Creates a new builder with its raw text set and the provided color applied
	 * @param rawText raw text, meaning formatting codes are ignored
	 */
	public JsonBuilder(@NotNull String rawText, @NotNull TextDecoration... decorations) {
		content(rawText);
		decorate(decorations);
	}

	/**
	 * Creates a new builder with its raw text set and the provided color applied
	 * @param rawText raw text, meaning formatting codes are ignored
	 */
	public JsonBuilder(@NotNull String rawText, @Nullable TextColor color) {
		content(rawText);
		color(color);
	}

	/**
	 * Creates a new builder with its raw text set and the provided color applied
	 * @param rawText raw text, meaning formatting codes are ignored
	 */
	public JsonBuilder(@NotNull String rawText, @Nullable ChatColor color) {
		content(rawText);
		color(color);
	}

	/**
	 * Creates a new builder with its raw text set and the provided color applied
	 * @param rawText raw text, meaning formatting codes are ignored
	 */
	public JsonBuilder(@NotNull String rawText, @Nullable Color color) {
		content(rawText);
		color(color);
	}

	/**
	 * Creates a new builder with its raw text set and the provided color applied
	 * @param rawText raw text, meaning formatting codes are ignored
	 */
	public JsonBuilder(@NotNull String rawText, @Nullable org.bukkit.Color color) {
		content(rawText);
		color(color);
	}

	/**
	 * Creates a new builder with its raw text set, provided color applied, and text decorations set
	 * @param rawText raw text, meaning formatting codes are ignored
	 */
	public JsonBuilder(@NotNull String rawText, @Nullable TextColor color, @NotNull TextDecoration... decorations) {
		content(rawText);
		color(color);
		decorate(decorations);
	}

	/**
	 * Creates a new builder with its raw text set, provided color applied, and text decorations set
	 * @param rawText raw text, meaning formatting codes are ignored
	 */
	public JsonBuilder(@NotNull String rawText, @Nullable ChatColor color, @NotNull TextDecoration... decorations) {
		content(rawText);
		color(color);
		decorate(decorations);
	}

	/**
	 * Creates a new builder with its raw text set, provided color applied, and text decorations set
	 * @param rawText raw text, meaning formatting codes are ignored
	 */
	public JsonBuilder(@NotNull String rawText, @Nullable Color color, @NotNull TextDecoration... decorations) {
		content(rawText);
		color(color);
		decorate(decorations);
	}

	/**
	 * Creates a new builder with its raw text set, provided color applied, and text decorations set
	 * @param rawText raw text, meaning formatting codes are ignored
	 */
	public JsonBuilder(@NotNull String rawText, @Nullable org.bukkit.Color color, @NotNull TextDecoration... decorations) {
		content(rawText);
		color(color);
		decorate(decorations);
	}

	/**
	 * Creates a new builder with its raw text set and provided style applied
	 * @param rawText raw text, meaning formatting codes are ignored
	 * @param style style that overwrites saved colors and text decorations
	 */
	public JsonBuilder(@NotNull String rawText, @NotNull Style style) {
		content(rawText);
		style(style);
	}

	/**
	 * Creates a new builder with a prefix at the start and the color set to DARK_AQUA.
	 * @param prefix prefix text
	 * @param contents component to append after the prefix, or null for nothing
	 */
	private JsonBuilder(@NotNull String prefix, @Nullable ComponentLike contents) {
		color(NamedTextColor.DARK_AQUA).next("&8&l[").next(prefix, NamedTextColor.YELLOW).next("&8&l]").next(" ").next(contents);
	}

	/**
	 * Creates a new builder with a prefix.
	 * @param prefix prefix text
	 * @return a new builder
	 */
	@Contract("_ -> new")
	public static JsonBuilder fromPrefix(@NotNull String prefix) {
		return fromPrefix(prefix, (ComponentLike) null);
	}

	/**
	 * Creates a new builder with a prefix.
	 * @param prefix prefix text
	 * @param contents contents to append to the prefix
	 * @return a new builder
	 */
	@Contract("_, _ -> new")
	public static JsonBuilder fromPrefix(@NotNull String prefix, @Nullable ComponentLike contents) {
		return new JsonBuilder(prefix, contents);
	}

	/**
	 * Creates a new builder with a prefix. Converts the input text to a component and appends it to the internal builder
	 * <p>
	 * Note: this does not apply any text, color, or formatting changes to the builder itself
	 * @param prefix prefix text
	 * @param contents contents to append to the prefix
	 * @return a new builder
	 */
	@Contract("_, _ -> new")
	public static JsonBuilder fromPrefix(@NotNull String prefix, @Nullable String contents) {
		return new JsonBuilder(prefix, new JsonBuilder(contents));
	}

	/**
	 * Creates a new builder formatted as an error.
	 * @param prefix prefix text
	 * @param error error to display
	 * @return a new builder
	 */
	@Contract("_, _ -> new")
	public static JsonBuilder fromError(@NotNull String prefix, @NotNull ComponentLike error) {
		return new JsonBuilder(prefix, error).color(NamedTextColor.RED);
	}

	/**
	 * Creates a new builder formatted as an error.
	 * @param prefix prefix text
	 * @param error error to display
	 * @return a new builder
	 */
	@Contract("_, _ -> new")
	public static JsonBuilder fromError(@NotNull String prefix, @NotNull String error) {
		return new JsonBuilder(prefix, Component.text(error)).color(NamedTextColor.RED);
	}

	/**
	 * Converts the input text to a colored and formatted component and appends it to the internal builder
	 * @param formattedText text formatted with ampersands or section symbols
	 * @return this builder
	 */
	@NotNull @Contract("_ -> this")
	public JsonBuilder next(@Nullable String formattedText) {
		if (formattedText != null)
			builder.append(AdventureUtils.fromLegacyText(colorize(formattedText)));
		return this;
	}

	/**
	 * Creates a component with its text and color set and appends it to the internal builder
	 * @param rawText raw text, color codes are ignored
	 * @return this builder
	 */
	@NotNull @Contract("_, _ -> this")
	public JsonBuilder next(@Nullable String rawText, @Nullable TextColor color) {
		if (rawText != null)
			builder.append(Component.text(rawText, color));
		return this;
	}

	/**
	 * Creates a component with its text and color set and appends it to the internal builder
	 * @param rawText raw text, color codes are ignored
	 * @return this builder
	 */
	@NotNull @Contract("_, _ -> this")
	public JsonBuilder next(@Nullable String rawText, @Nullable ChatColor color) {
		return next(rawText, AdventureUtils.textColorOf(color));
	}

	/**
	 * Creates a component with its text and color set and appends it to the internal builder
	 * @param rawText raw text, color codes are ignored
	 * @return this builder
	 */
	@NotNull @Contract("_, _ -> this")
	public JsonBuilder next(@Nullable String rawText, @Nullable Color color) {
		return next(rawText, AdventureUtils.textColorOf(color));
	}

	/**
	 * Creates a component with its text and color set and appends it to the internal builder
	 * @param rawText raw text, color codes are ignored
	 * @return this builder
	 */
	@NotNull @Contract("_, _ -> this")
	public JsonBuilder next(@Nullable String rawText, @Nullable org.bukkit.Color color) {
		return next(rawText, AdventureUtils.textColorOf(color));
	}

	/**
	 * Creates a component with its text, color, and decorations set, and appends it to the internal builder
	 * @param rawText raw text, color codes are ignored
	 * @return this builder
	 */
	@NotNull @Contract("_, _, _ -> this")
	public JsonBuilder next(@Nullable String rawText, @Nullable TextColor color, TextDecoration... decorations) {
		if (rawText != null)
			builder.append(Component.text(rawText, color, decorations));
		return this;
	}

	/**
	 * Creates a component with its text, color, and decorations set, and appends it to the internal builder
	 * @param rawText raw text, color codes are ignored
	 * @return this builder
	 */
	@NotNull @Contract("_, _, _ -> this")
	public JsonBuilder next(@Nullable String rawText, @Nullable ChatColor color, TextDecoration... decorations) {
		return next(rawText, AdventureUtils.textColorOf(color), decorations);
	}

	/**
	 * Creates a component with its text, color, and decorations set, and appends it to the internal builder
	 * @param rawText raw text, color codes are ignored
	 * @return this builder
	 */
	@NotNull @Contract("_, _, _ -> this")
	public JsonBuilder next(@Nullable String rawText, @Nullable Color color, TextDecoration... decorations) {
		return next(rawText, AdventureUtils.textColorOf(color), decorations);
	}

	/**
	 * Creates a component with its text, color, and decorations set, and appends it to the internal builder
	 * @param rawText raw text, color codes are ignored
	 * @return this builder
	 */
	@NotNull @Contract("_, _, _ -> this")
	public JsonBuilder next(@Nullable String rawText, @Nullable org.bukkit.Color color, TextDecoration... decorations) {
		return next(rawText, AdventureUtils.textColorOf(color), decorations);
	}

	/**
	 * Creates a component with its text, color, and decorations set, and appends it to the internal builder
	 * @param rawText raw text, color codes are ignored
	 * @return this builder
	 */
	@NotNull @Contract("_, _ -> this")
	public JsonBuilder next(@Nullable String rawText, TextDecoration... decorations) {
		if (rawText != null)
			builder.append(new JsonBuilder(rawText, decorations));
		return this;
	}

	/**
	 * Sets the raw text for the base text component
	 * @param rawText raw text, color codes are ignored
	 * @return this builder
	 */
	@NotNull @Contract("_ -> this")
	public JsonBuilder content(@NotNull String rawText) {
		builder.content(rawText);
		return this;
	}

	/**
	 * Appends a component to the internal builder
	 * @return this builder
	 */
	@NotNull @Contract("_ -> this")
	public JsonBuilder next(@Nullable ComponentLike component) {
		if (component != null)
			builder.append(component);
		return this;
	}

	/**
	 * Applies saved hover text, appends the current working Component to the output Component, and resets the working
	 * component to default. This effectively saves the working Component and switches to building a new one.
	 * @return this builder
	 */
	@NotNull @Contract("-> this")
	public JsonBuilder group() {
		if (!lore.isEmpty()) {
			List<String> lines = new ArrayList<>();
			lore.forEach(line -> {
				if (loreize) line = StringUtils.loreize(line);
				line = line.replaceAll("\\|\\|", System.lineSeparator()); // TODO remove...
				lines.addAll(Arrays.asList(colorize(line).split(System.lineSeparator())));
			});

			Builder hover = Component.text();

			Iterator<String> iterator = lines.iterator();
			while (iterator.hasNext()) {
				hover.append(AdventureUtils.fromLegacyText(iterator.next()));
				if (iterator.hasNext())
					hover.append(Component.newline());
			}

			builder.hoverEvent(hover.build().asHoverEvent());
			lore.clear();
		}

		result.append(builder);
		builder = Component.text();
		return this;
	}

	/**
	 * Adds a new line and creates a new {@link #group()}
	 * @return this builder
	 */
	@NotNull @Contract("-> this")
	public JsonBuilder newline() {
		return newline(false);
	}

	/**
	 * Adds a new line and optionally creates a new {@link #group()}
	 * @return this builder
	 */
	@NotNull @Contract("_ -> this")
	public JsonBuilder newline(boolean newGroup) {
		builder.append(Component.text(System.lineSeparator()));
		if (newGroup)
			group();
		return this;
	}

	/**
	 * Creates an empty line (2x {@link #newline()})
	 */
	@NotNull @Contract("-> this")
	public JsonBuilder line() {
		newline();
		newline();
		return this;
	}

	/**
	 * Sets the color for the internal builder
	 * @return this builder
	 */
	@NotNull @Contract("_ -> this")
	public JsonBuilder color(@Nullable TextColor color) {
		builder.color(color);
		return this;
	}

	/**
	 * Sets the color for the internal builder
	 * @return this builder
	 */
	@NotNull @Contract("_ -> this")
	public JsonBuilder color(@Nullable ChatColor color) {
		return color(AdventureUtils.textColorOf(color));
	}

	/**
	 * Sets the color for the internal builder
	 * @return this builder
	 */
	@NotNull @Contract("_ -> this")
	public JsonBuilder color(@Nullable Color color) {
		return color(AdventureUtils.textColorOf(color));
	}

	/**
	 * Sets the color for the internal builder
	 * @return this builder
	 */
	@NotNull @Contract("_ -> this")
	public JsonBuilder color(@Nullable org.bukkit.Color color) {
		return color(AdventureUtils.textColorOf(color));
	}

	/**
	 * Parses a hexadecimal number
	 * @param color number in the format "#FFFFFF" (# optional)
	 * @throws IllegalArgumentException string contained an invalid hexadecimal number
	 * @return this builder
	 */
	@NotNull @Contract("_ -> this")
	public JsonBuilder color(@Nullable String color) throws IllegalArgumentException {
		return color(AdventureUtils.textColorOf(color));
	}

	/**
	 * Enables the provided text decorations
	 * @param decorations varargs list of decorations
	 * @return this builder
	 */
	@NotNull @Contract("_ -> this")
	public JsonBuilder decorate(@NotNull TextDecoration... decorations) {
		return decorate(true, decorations);
	}

	/**
	 * Sets the state of text decorations
	 * @param state whether to enable or disable the decorations
	 * @param decorations varargs list of decorations
	 * @return this builder
	 */
	@NotNull @Contract("_, _ -> this")
	public JsonBuilder decorate(boolean state, @NotNull TextDecoration... decorations) {
		return decorate(state, Arrays.asList(decorations));
	}

	/**
	 * Sets the state of text decorations
	 * @param state state to set the decorations to
	 * @param decorations varargs list of decorations
	 * @return this builder
	 */
	@NotNull @Contract("_, _ -> this")
	public JsonBuilder decorate(@NotNull TextDecoration.State state, @NotNull TextDecoration... decorations) {
		return decorate(state, Arrays.asList(decorations));
	}

	/**
	 * Sets the state of text decorations
	 * @param state whether to enable or disable the decorations
	 * @param decorations collection of decorations
	 * @return this builder
	 */
	@NotNull @Contract("_, _ -> this")
	public JsonBuilder decorate(boolean state, @NotNull Collection<TextDecoration> decorations) {
		return decorate(TextDecoration.State.byBoolean(state), new HashSet<>(decorations));
	}

	/**
	 * Sets the state of text decorations
	 * @param state state to set the decorations to
	 * @param decorations collection of decorations
	 * @return this builder
	 */
	@NotNull @Contract("_, _ -> this")
	public JsonBuilder decorate(@NotNull TextDecoration.State state, @NotNull Collection<TextDecoration> decorations) {
		decorations.forEach(decoration -> builder.decoration(decoration, state));
		return this;
	}

	/**
	 * Sets the state of text decorations
	 * @param decorations map of decorations to states
	 * @return this builder
	 */
	@NotNull @Contract("_ -> this")
	public JsonBuilder decorate(@NotNull Map<TextDecoration, TextDecoration.State> decorations) {
		decorations.forEach((decoration, state) -> builder.decoration(decoration, state));
		return this;
	}

	/**
	 * Enables bold on the internal builder
	 * @return this builder
	 */
	@NotNull @Contract("-> this")
	public JsonBuilder bold() {
		return bold(true);
	}

	/**
	 * Sets the state of bold on the internal builder
	 * @return this builder
	 */
	@NotNull @Contract("_ -> this")
	public JsonBuilder bold(boolean state) {
		return bold(TextDecoration.State.byBoolean(state));
	}

	/**
	 * Sets the state of bold on the internal builder
	 * @return this builder
	 */
	@NotNull @Contract("_ -> this")
	public JsonBuilder bold(@NotNull TextDecoration.State state) {
		return decorate(state, TextDecoration.BOLD);
	}

	/**
	 * Enables italicization on the internal builder
	 * @return this builder
	 */
	@NotNull @Contract("-> this")
	public JsonBuilder italic() {
		return italic(true);
	}

	/**
	 * Sets the state of italicization on the internal builder
	 * @return this builder
	 */
	@NotNull @Contract("_ -> this")
	public JsonBuilder italic(boolean state) {
		return italic(TextDecoration.State.byBoolean(state));
	}

	/**
	 * Sets the state of italicization on the internal builder
	 * @return this builder
	 */
	@NotNull @Contract("_ -> this")
	public JsonBuilder italic(@NotNull TextDecoration.State state) {
		return decorate(state, TextDecoration.ITALIC);
	}


	/**
	 * Enables strikethrough on the internal builder
	 * @return this builder
	 */
	@NotNull @Contract("-> this")
	public JsonBuilder strikethrough() {
		return strikethrough(true);
	}

	/**
	 * Sets the state of strikethrough on the internal builder
	 * @return this builder
	 */
	@NotNull @Contract("_ -> this")
	public JsonBuilder strikethrough(boolean state) {
		return strikethrough(TextDecoration.State.byBoolean(state));
	}

	/**
	 * Sets the state of strikethrough on the internal builder
	 * @return this builder
	 */
	@NotNull @Contract("_ -> this")
	public JsonBuilder strikethrough(@NotNull TextDecoration.State state) {
		return decorate(state, TextDecoration.STRIKETHROUGH);
	}

	/**
	 * Enables underlines on the internal builder
	 * @return this builder
	 */
	@NotNull @Contract("-> this")
	public JsonBuilder underline() {
		return underline(true);
	}

	/**
	 * Sets the state of underlines on the internal builder
	 * @return this builder
	 */
	@NotNull @Contract("_ -> this")
	public JsonBuilder underline(boolean state) {
		return underline(TextDecoration.State.byBoolean(state));
	}

	/**
	 * Sets the state of underlines on the internal builder
	 * @return this builder
	 */
	@NotNull @Contract("_ -> this")
	public JsonBuilder underline(@NotNull TextDecoration.State state) {
		return decorate(state, TextDecoration.UNDERLINED);
	}

	/**
	 * Enables obfuscation (random gibberish text) on the internal builder
	 * @return this builder
	 */
	@NotNull @Contract("-> this")
	public JsonBuilder obfuscate() {
		return obfuscate(true);
	}

	/**
	 * Sets the state of obfuscation (random gibberish text) on the internal builder
	 * @return this builder
	 */
	@NotNull @Contract("_ -> this")
	public JsonBuilder obfuscate(boolean state) {
		return obfuscate(TextDecoration.State.byBoolean(state));
	}

	/**
	 * Sets the state of obfuscation (random gibberish text) on the internal builder
	 * @return this builder
	 */
	@NotNull @Contract("_ -> this")
	public JsonBuilder obfuscate(@NotNull TextDecoration.State state) {
		return decorate(state, TextDecoration.OBFUSCATED);
	}

	/**
	 * Sets the style of the internal builder, replacing existing colors and decorations
	 * @return this builder
	 */
	@NotNull @Contract("_ -> this")
	public JsonBuilder style(@NotNull Style style) {
		builder.style(style);
		return this;
	}

	/**
	 * Sets the style of the internal builder, replacing existing colors and decorations
	 * @return this builder
	 */
	@NotNull @Contract("_ -> this")
	public JsonBuilder style(@NotNull Consumer<Style.Builder> style) {
		builder.style(style);
		return this;
	}

	/**
	 * Adds an action to run on click
	 * @return this builder
	 */
	@NotNull @Contract("_ -> this")
	public JsonBuilder clickEvent(@Nullable ClickEvent clickEvent) {
		builder.clickEvent(clickEvent);
		return this;
	}

	/**
	 * Prompts the player to open a URL when clicked
	 * @return this builder
	 */
	@NotNull @Contract("_ -> this")
	public JsonBuilder url(@NotNull String url) {
		return clickEvent(ClickEvent.openUrl(url));
	}

	/**
	 * Prompts the player to open a URL when clicked
	 * @return this builder
	 */
	@NotNull @Contract("_ -> this")
	public JsonBuilder url(@NotNull URL url) {
		return clickEvent(ClickEvent.openUrl(url));
	}

	/**
	 * Makes the player run a command when clicked
	 * @param command a command, forward slash not required
	 * @return this builder
	 */
	@NotNull @Contract("_ -> this")
	public JsonBuilder command(@NotNull String command) {
		if (!command.startsWith("/"))
			command = "/" + command;
		return clickEvent(ClickEvent.runCommand(command));
	}

	/**
	 * Suggests a command to a player on click by typing it into their chat window
	 * @param text some text, usually a command
	 * @return this builder
	 */
	@NotNull @Contract("_ -> this")
	public JsonBuilder suggest(@NotNull String text) {
		return clickEvent(ClickEvent.suggestCommand(text));
	}

	/**
	 * Copies text to the user's clipboard on click
	 * @param text text to copy
	 * @return this builder
	 */
	@NotNull @Contract("_ -> this")
	public JsonBuilder copy(@NotNull String text) {
		return clickEvent(ClickEvent.copyToClipboard(text));
	}

	/**
	 * Sets the page of a book when clicked
	 * @param page page number
	 * @return this builder
	 */
	@NotNull @Contract("_ -> this")
	public JsonBuilder bookPage(int page) {
		return bookPage(String.valueOf(page));
	}

	/**
	 * Sets the page of a book when clicked
	 * @param page page number
	 * @return this builder
	 */
	@NotNull @Contract("_ -> this")
	public JsonBuilder bookPage(@NotNull String page) {
		return clickEvent(ClickEvent.changePage(page));
	}

	/**
	 * Toggles loreize, which automatically splits text and replaces || with newlines using {@link StringUtils#loreize(String)}
	 * @return this builder
	 */
	@NotNull @Contract("_ -> this")
	public JsonBuilder loreize(boolean loreize) {
		this.loreize = loreize;
		return this;
	}

	/**
	 * Clears this builder's hover text
	 * @return this builder
	 */
	@NotNull @Contract("-> this")
	public JsonBuilder hover() {
		lore.clear();
		builder.hoverEvent(null);
		return this;
	}

	/**
	 * Adds lines of text to this builder's hover text
	 * <br>Note: this is not computed until {@link #build()} which will override any other hover set
	 * @param lines lines of text
	 * @return this builder
	 */
	@NotNull @Contract("_ -> this")
	public JsonBuilder hover(@NonNull String... lines) {
		return hover(Arrays.asList(lines));
	}

	/**
	 * Adds lines of text to this builder's hover text
	 * <br>Note: this is not computed until {@link #build()} which will override any other hover set
	 * @param lines lines of text
	 * @return this builder
	 */
	@NotNull @Contract("_ -> this")
	public JsonBuilder hover(@NonNull List<String> lines) {
		lore.addAll(lines);
		return this;
	}

	/**
	 * Gets a copy of the current pending lines of hover text
	 * @return list of not-yet-formatted strings
	 */
	@NotNull @Contract("-> this")
	public List<String> getHoverLines() {
		return new ArrayList<>(lore);
	}

	/**
	 * Sets hover text to render the metadata of the provided item stack, like when hovering over one in your inventory
	 * @return this builder
	 */
	@NotNull @Contract("_ -> this")
	public JsonBuilder hover(@NonNull ItemStack itemStack) {
		builder.hoverEvent(itemStack.asHoverEvent());
		return this;
	}

	/**
	 * Sets hover text to render the name, type, and UUID of an entity
	 * @return this builder
	 */
	@NotNull @Contract("_ -> this")
	public JsonBuilder hover(@NonNull Entity entity) {
		builder.hoverEvent(entity.asHoverEvent());
		return this;
	}

	/**
	 * Sets the text shown on hover
	 * @return this builder
	 */
	@NotNull @Contract("_ -> this")
	public <V> JsonBuilder hover(@Nullable HoverEventSource<V> hoverEvent) {
		builder.hoverEvent(hoverEvent);
		return this;
	}

	/**
	 * Sets a component to display on hover
	 * @return this builder
	 */
	@NotNull @Contract("_ -> this")
	public JsonBuilder hover(@Nullable ComponentLike component) {
		if (component != null)
			builder.hoverEvent(component.asComponent().asHoverEvent());
		return this;
	}

	/**
	 * Sets a string that will be inserted in chat when this component is shift-clicked
	 * @return this builder
	 */
	@NotNull @Contract("_ -> this")
	public JsonBuilder insert(@Nullable String insertion) {
		builder.insertion(insertion);
		return this;
	}

	/**
	 * "Initializes" the builder. This does nothing on its own. See {@link #isInitialized()} for more information.
	 * @return this builder
	 */
	@NotNull @Contract("-> this")
	public JsonBuilder initialize() {
		this.initialized = true;
		return this;
	}

	/**
	 * Runs {@link #build()} and sends the resulting component to a recipient.
	 * @param recipient a player object (see {@link PlayerUtils#send(Object, Object)} for valid objects)
	 */
	public void send(@Nullable Object recipient) {
		PlayerUtils.send(recipient, this);
	}

	/**
	 * Executes {@link #group()} and returns the final result
	 * @return resultant component
	 */
	@NotNull
	public TextComponent build() {
		group();
		return result.build();
	}

	/**
	 * Alias of {@link #build()}. Executes {@link #group()} and returns the final result
	 * @return resultant component
	 */
	@Override
	@NonNull
	public Component asComponent() {
		return build();
	}

	/**
	 * Builds this component ({@link #build()}) and formats it as plain text using Paper's serializer
	 * @return plain human text (no color codes)
	 */
	@NotNull
	public String toString() {
		return AdventureUtils.asPlainText(build());
	}

	/**
	 * Builds this component ({@link #build()}) and converts it to json format using Paper's serializer
	 * @deprecated should not generally be working with json anymore
	 * @return Minecraft json string
	 */
	@NotNull
	@Deprecated
	public String serialize() {
		return GsonComponentSerializer.gson().serialize(build());
	}

}
