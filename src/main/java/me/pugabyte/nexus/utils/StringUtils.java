package me.pugabyte.nexus.utils;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import me.lexikiq.HasPlayer;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtils extends eden.utils.StringUtils {
	@Getter
	private static final String colorChar = "§";
	@Getter
	private static final String altColorChar = "&";
	@Getter
	private static final String colorCharsRegex = "[" + colorChar + altColorChar + "]";
	@Getter
	private static final Pattern colorPattern = Pattern.compile(colorCharsRegex + "[0-9a-fA-F]");
	@Getter
	private static final Pattern formatPattern = Pattern.compile(colorCharsRegex + "[k-orK-OR]");
	@Getter
	private static final Pattern hexPattern = Pattern.compile(colorCharsRegex + "#[a-fA-F0-9]{6}");
	@Getter
	private static final Pattern hexColorizedPattern = Pattern.compile(colorCharsRegex + "x(" + colorCharsRegex + "[a-fA-F0-9]){6}");
	@Getter
	private static final Pattern colorGroupPattern = Pattern.compile("(" + colorPattern + "|(" + hexPattern + "|" + hexColorizedPattern + "))((" + formatPattern + ")+)?");
	@Getter
	public static final String CHECK = "&a✔";
	@Getter
	public static final String X = "&c✗";
	@Getter
	public static final String COMMA_SPLIT_REGEX = ",(?=[^}]*(?:\\{|$))";

	public static String getPrefix(Class<?> clazz) {
		return getPrefix(clazz.getSimpleName());
	}

	public static String getPrefix(String prefix) {
		return colorize("&8&l[&e" + prefix + "&8&l]&3 ");
	}

	public static String colorize(String input) {
		if (input == null)
			return null;

		while (true) {
			Matcher matcher = hexPattern.matcher(input);
			if (!matcher.find()) break;

			String color = matcher.group();
			input = input.replace(color, ChatColor.of(color.replaceFirst(colorCharsRegex, "")).toString());
		}

		return ChatColor.translateAlternateColorCodes(altColorChar.charAt(0), input);
	}

	// Replaces § with & and unformats hex, does NOT strip colors
	public static String decolorize(String input) {
		if (input == null)
			return null;

		input = colorize(input);

		while (true) {
			Matcher matcher = hexColorizedPattern.matcher(input);
			if (!matcher.find()) break;

			String color = matcher.group();
			input = input.replace(color, color.replace(colorChar + "x", "&#").replaceAll(colorChar, ""));
		}

		return input.replaceAll(colorChar, altColorChar);
	}

	public static String toHex(ChatColor color) {
		return "#" + Integer.toHexString(color.getColor().getRGB()).substring(2);
	}

	public static String stripColor(String input) {
		return ChatColor.stripColor(colorize(input));
	}

	public static String stripFormat(String input) {
		return formatPattern.matcher(colorize(input)).replaceAll("");
	}

	// TODO This will break with hex
	// TODO replace with https://canary.discord.com/channels/132680070480396288/421474915930079232/827394245522096158
	// 		- needs to strip color when measuring max length and carry colors over to next line
	public static String loreize(String string) {
		if (string == null) return null;

		int i = 0, lineLength = 0;
		boolean watchForNewLine = false, watchForColor = false;
		string = colorize(string);

		for (String character : string.split("")) {
			if (character.contains("\n")) {
				lineLength = 0;
				continue;
			}

			if (watchForNewLine) {
				if ("|".equalsIgnoreCase(character))
					lineLength = 0;
				watchForNewLine = false;
			} else if ("|".equalsIgnoreCase(character))
				watchForNewLine = true;

			if (watchForColor) {
				if (character.matches("[A-Fa-fK-Ok-oRr0-9]"))
					lineLength -= 2;
				watchForColor = false;
			} else if ("&".equalsIgnoreCase(character))
				watchForColor = true;

			++lineLength;

			if (lineLength > 28)
				if (" ".equalsIgnoreCase(character)) {
					String before = left(string, i);
					String excess = right(string, string.length() - i);
					if (excess.length() > 5) {
						excess = excess.trim();
						boolean doSplit = true;
						if (excess.contains("||") && excess.indexOf("||") <= 5)
							doSplit = false;
						if (excess.contains(" ") && excess.indexOf(" ") <= 5)
							doSplit = false;
						if (lineLength >= 38)
							doSplit = true;

						if (doSplit) {
							string = before + "||" + getLastColor(before) + excess.trim();
							lineLength = 0;
							i += 4;
						}
					}
				}

			++i;
		}

		return string;
	}

	public static List<String> splitLore(String lore) {
		return new ArrayList<>(Arrays.asList(lore.split("\\|\\|")));
	}

	public static String getLastColor(String text) {
		Matcher matcher = colorGroupPattern.matcher(text);
		String last = "";
		while (matcher.find())
			last = matcher.group();
		return last.toLowerCase();
	}

	public static String applyFormattingToAll(String input, boolean bold, boolean strikethrough, boolean underline, boolean italic, boolean magic) {
		if (input == null)
			return null;

		if (bold) input = applyFormattingToAll(input, ChatColor.BOLD);
		if (strikethrough) input = applyFormattingToAll(input, ChatColor.STRIKETHROUGH);
		if (underline) input = applyFormattingToAll(input, ChatColor.UNDERLINE);
		if (italic) input = applyFormattingToAll(input, ChatColor.ITALIC);
		if (magic) input = applyFormattingToAll(input, ChatColor.MAGIC);
		return input;
	}

	public static String applyFormattingToAll(String input, ChatColor formatting) {
		return StringUtils.getColorGroupPattern().matcher(input).replaceAll(result -> result.group() + formatting);
	}

	public static String pretty(ItemStack item) {
		return pretty(item, 1);
	}

	public static String pretty(ItemStack item, int amount) {
		return item.getAmount() * amount + " " + camelCase(item.getType().name());
	}

	public static String bool(boolean b) {
		if (b)
			return "&atrue";
		else
			return "&cfalse";
	}

	public enum ProgressBarStyle {
		NONE,
		COUNT,
		PERCENT
	}

	@NotNull
	public static String an(@NotNull String text) {
		return "a" + (text.matches("(?i)^[AEIOU].*") ? "n" : "");
	}

	public static String progressBar(int progress, int goal) {
		return progressBar(progress, goal, ProgressBarStyle.NONE, 25);
	}

	public static String progressBar(int progress, int goal, ProgressBarStyle style) {
		return progressBar(progress, goal, style, 25);
	}

	public static String progressBar(int progress, int goal, ProgressBarStyle style, int length) {
		double percent = Math.min((double) progress / goal, 1);
		ChatColor color = ChatColor.RED;
		if (percent == 1)
			color = ChatColor.GREEN;
		else if (percent >= 2/3)
			color = ChatColor.YELLOW;
		else if (percent >= 1/3)
			color = ChatColor.GOLD;

		int n = (int) Math.floor(percent * length);

		String bar = String.join("", Collections.nCopies(length, "|"));
		String first = left(bar, n);
		String last = right(bar, length - n);
		String result = color + first + "&8" + last;

		// TODO: Style
		if (style == ProgressBarStyle.COUNT)
			result += " &f" + progress + "/" + goal;
		if (style == ProgressBarStyle.PERCENT)
			result += " &f" + Math.floor(percent * 100) + "%";

		return result;
	}

	private static final String[] compassParts = {"[S]","SW","[W]","NW","[N]","NE","[E]","SE"};

	public static String compass(HasPlayer player) {
		return compass(player, 8);
	}

	public static String compass(HasPlayer player, int extra) {
		return compass(player, extra, 4);
	}

	public static String compass(HasPlayer player, int extra, int separators) {
		String compass = "";
		for (String compassPart : compassParts)
			compass += compassPart + " " + String.join("", Collections.nCopies(separators, "-")) + " ";

		float yaw = Location.normalizeYaw(player.getPlayer().getLocation().getYaw());
		if (yaw < 0) yaw = 360 + yaw;

		int center = (int) Math.round(yaw / (360D / compass.length())) + 1;

		String instance;
		if (center - extra < 0) {
			center += compass.length();
			instance = (compass + compass).substring(center - extra, center + extra + 1);
		} else if (center + extra + 1 > compass.length())
			instance = (compass + compass).substring(center - extra, center + extra + 1);
		else
			instance = compass.substring(center - extra, center + extra + 1);

		instance = instance.replaceAll("\\[", "&2[&f");
		instance = instance.replaceAll("]", "&2]&f");
		return colorize(instance);
	}

	public static String getWorldDisplayName(String world) {
		if (Arrays.asList("world", "world_nether", "world_the_end").contains(world))
			world = world.replace("world", "legacy");
		else if (world.contains("oneblock"))
			world = world.replace("oneblock_world", "one_block");
		else if (world.contains("bskyblock"))
			world = world.replace("bskyblock_world", "skyblock");
		else if (world.equals("bearfair21"))
			return "Bear Fair 21";
		else if (world.equals("uhc"))
			return "UHC";
		return camelCase(world);
	}

	public static String getWorldDisplayName(World world) {
		return getWorldDisplayName(world.getName());
	}

	public static String getLocationString(Location loc) {
		return "&3World: &e" + loc.getWorld().getName() + " &3x: &e" + df.format(loc.getX()) + " &3y: &e" +
				df.format(loc.getY()) + " &3z: &e" +  df.format(loc.getZ());
	}

	public static String getShortLocationString(Location loc) {
		return (int) loc.getX() + " " + (int) loc.getY() + " " +  (int) loc.getZ() + " " + loc.getWorld().getName();
	}

	public static String getShortishLocationString(Location loc) {
		return (int) loc.getX() + " " + (int) loc.getY() + " " +  (int) loc.getZ() + " " + df.format(loc.getYaw()) + " " + df.format(loc.getPitch()) + " " + loc.getWorld().getName();
	}

	public static String getCoordinateString(Location loc) {
		return (int) loc.getX() + " " + (int) loc.getY() + " " +  (int) loc.getZ();
	}

	public static String getFlooredCoordinateString(Location loc) {
		return (int) Math.floor(loc.getX()) + " " + (int) Math.floor(loc.getY()) + " " + (int) Math.floor(loc.getZ());
	}

	/**
	 * Sends a message to the player with the input message as a teleportation link.
	 * @param message message to display
	 * @param location location to set as a teleport destination on click
	 * @param player any player handled by {@link PlayerUtils#send(Object, Object, Object...)}
	 */
	public static void sendJsonLocation(String message, Location location, Object player) {
		new JsonBuilder().next(message).command(getTeleportCommand(location)).send(player);
	}

	public static String getTeleportCommand(Location location) {
		return "/tppos " + df.format(location.getX()) + " " + df.format(location.getY()) + " " + df.format(location.getZ()) + " " +
				df.format(location.getYaw()) + " " + df.format(location.getPitch()) + " " + location.getWorld().getName();
	}

	@RequiredArgsConstructor
	public static class Gradient {
		@NonNull
		private final List<ChatColor> colors;

		public static Gradient of(List<ChatColor> colors) {
			return new Gradient(colors);
		}

		public String apply(String text) {
			int count = colors.size();
			if (count == 0)
				return text;
			if (count == 1)
				return colors.get(0) + text;

			int groups = count - 1;
			int groupLength = text.length() / groups;
			List<String> parts = new ArrayList<>();
			for (int i = 0; i < groups; i++) {
				int start = i * groupLength;
				int end = start + groupLength;
				if (i == groups - 1)
					end = text.length();
				parts.add(text.substring(start, end));
			}

			Iterator<ChatColor> iterator = colors.iterator();
			ChatColor color1;
			ChatColor color2 = iterator.next();

			StringBuilder builder = new StringBuilder();

			for (int i = 0; i < groups; i++) {
				color1 = color2;
				color2 = iterator.next();

				String part = parts.get(i);

				int l = part.length();
				for (int j = 0; j < l; j++) {
					builder.append(ChatColor.of(new Color(
							(color1.getColor().getRed() + (j * (1F / l) * (color2.getColor().getRed() - color1.getColor().getRed()))) / 255,
							(color1.getColor().getGreen() + (j * (1F / l) * (color2.getColor().getGreen() - color1.getColor().getGreen()))) / 255,
							(color1.getColor().getBlue() + (j * (1F / l) * (color2.getColor().getBlue() - color1.getColor().getBlue()))) / 255
					)));
					builder.append(part.charAt(j));
				}
			}

			return builder.toString();
		}
	}

	public static class Rainbow {
		public static String apply(String text) {
			StringBuilder builder = new StringBuilder();
			int l = text.length();
			for (int i = 0; i < l; i++) {
				builder.append(ChatColor.of(Color.getHSBColor(((float) i / l) * .75F, .9F, .9F)));
				builder.append(text.charAt(i));
			}
			return builder.toString();
		}
	}

	public enum DefaultFontInfo {
		A('A', 5),
		a('a', 5),
		B('B', 5),
		b('b', 5),
		C('C', 5),
		c('c', 5),
		D('D', 5),
		d('d', 5),
		E('E', 5),
		e('e', 5),
		F('F', 5),
		f('f', 4),
		G('G', 5),
		g('g', 5),
		H('H', 5),
		h('h', 5),
		I('I', 3),
		i('i', 1),
		J('J', 5),
		j('j', 5),
		K('K', 5),
		k('k', 4),
		L('L', 5),
		l('l', 1),
		M('M', 5),
		m('m', 5),
		N('N', 5),
		n('n', 5),
		O('O', 5),
		o('o', 5),
		P('P', 5),
		p('p', 5),
		Q('Q', 5),
		q('q', 5),
		R('R', 5),
		r('r', 5),
		S('S', 5),
		s('s', 5),
		T('T', 5),
		t('t', 4),
		U('U', 5),
		u('u', 5),
		V('V', 5),
		v('v', 5),
		W('W', 5),
		w('w', 5),
		X('X', 5),
		x('x', 5),
		Y('Y', 5),
		y('y', 5),
		Z('Z', 5),
		z('z', 5),
		NUM_1('1', 5),
		NUM_2('2', 5),
		NUM_3('3', 5),
		NUM_4('4', 5),
		NUM_5('5', 5),
		NUM_6('6', 5),
		NUM_7('7', 5),
		NUM_8('8', 5),
		NUM_9('9', 5),
		NUM_0('0', 5),
		EXCLAMATION_POINT('!', 1),
		AT_SYMBOL('@', 6),
		NUM_SIGN('#', 5),
		DOLLAR_SIGN('$', 5),
		PERCENT('%', 5),
		UP_ARROW('^', 5),
		AMPERSAND('&', 5),
		ASTERISK('*', 5),
		LEFT_PARENTHESIS('(', 4),
		RIGHT_PERENTHESIS(')', 4),
		MINUS('-', 5),
		UNDERSCORE('_', 5),
		PLUS_SIGN('+', 5),
		EQUALS_SIGN('=', 5),
		LEFT_CURL_BRACE('{', 4),
		RIGHT_CURL_BRACE('}', 4),
		LEFT_BRACKET('[', 3),
		RIGHT_BRACKET(']', 3),
		COLON(':', 1),
		SEMI_COLON(';', 1),
		DOUBLE_QUOTE('"', 3),
		SINGLE_QUOTE('\'', 1),
		LEFT_ARROW('<', 4),
		RIGHT_ARROW('>', 4),
		QUESTION_MARK('?', 5),
		SLASH('/', 5),
		BACK_SLASH('\\', 5),
		LINE('|', 1),
		TILDE('~', 5),
		TICK('`', 2),
		PERIOD('.', 1),
		COMMA(',', 1),
		SPACE(' ', 3),
		DEFAULT('a', 4);

		private char character;
		private int length;

		DefaultFontInfo(char character, int length) {
			this.character = character;
			this.length = length;
		}

		public char getCharacter() {
			return this.character;
		}

		public int getLength() {
			return this.length;
		}

		public int getBoldLength() {
			if (this == DefaultFontInfo.SPACE)
				return this.getLength();
			return this.length + 1;
		}

		public static DefaultFontInfo getDefaultFontInfo(char c) {
			for (DefaultFontInfo dFI : DefaultFontInfo.values())
				if (dFI.getCharacter() == c)
					return dFI;
			return DefaultFontInfo.DEFAULT;
		}
	}

}
