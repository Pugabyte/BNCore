package me.pugabyte.nexus.features.minigames.managers;

import com.google.common.base.Strings;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import lombok.Getter;
import lombok.SneakyThrows;
import me.pugabyte.nexus.Nexus;
import me.pugabyte.nexus.features.minigames.models.Arena;
import me.pugabyte.nexus.framework.exceptions.postconfigured.InvalidInputException;
import me.pugabyte.nexus.utils.WorldGuardUtils;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ArenaManager {
	private static List<Arena> arenas = new ArrayList<>();
	@Getter
	private static String folder = "plugins/Nexus/minigames/arenas/";

	public static List<Arena> getAll() {
		return arenas;
	}

	public static List<Arena> getAll(String filter) {
		List<Arena> filtered = new ArrayList<>();
		for (Arena arena : arenas) {
			if (filter != null)
				if (!arena.getName().toLowerCase().startsWith(filter.toLowerCase()))
					continue;
			filtered.add(arena);
		}

		return filtered;
	}

	public static List<String> getNames() {
		return getNames(null);
	}

	public static List<String> getNames(String filter) {
		return getAll(filter).stream().map(Arena::getName).collect(Collectors.toList());
	}

	public static Arena getFromLocation(Location location) {
		Set<ProtectedRegion> regionsAt = new WorldGuardUtils(location).getRegionsAt(location);
		for (ProtectedRegion region : regionsAt) {
			Arena fromRegion = getFromRegion(region.getId());
			if (fromRegion != null)
				return fromRegion;
		}

		return null;
	}

	public static Arena getFromRegion(String regionName) {
		try {
			String mechanicName, arenaName;
			if (!regionName.contains("_")) {
				mechanicName = regionName;
				arenaName = regionName;
			} else {
				mechanicName = regionName.split("_")[0];
				arenaName = regionName.split("_")[1];
			}

			Arena arena = get(arenaName);
			if (arena.getMechanic().getClass().getSimpleName().equalsIgnoreCase(mechanicName))
				return arena;
		} catch (ArrayIndexOutOfBoundsException | InvalidInputException ignore) {}

		return null;
	}

	public static Arena find(String name) {
		if (!Strings.isNullOrEmpty(name)) {
			for (Arena arena : arenas)
				if (arena.getName().equalsIgnoreCase(name))
					return arena;
			for (Arena arena : arenas)
				if (arena.getName().toLowerCase().startsWith(name.toLowerCase()))
					return arena;
		}
		throw new InvalidInputException("Arena not found");
	}

	public static  boolean exists(String name) {
		try {
			get(name);
			return true;
		} catch (InvalidInputException ex) {
			return false;
		}
	}

	public static Arena get(String name) {
		for (Arena arena : arenas)
			if (arena.getName().equalsIgnoreCase(name))
				return arena;
		throw new InvalidInputException("Arena not found");
	}

	public static Arena get(int id) {
		for (Arena arena : arenas)
			if (arena.getId() == id)
				return arena;
		throw new InvalidInputException("Arena not found");
	}

	public static void add(Arena arena) {
		try { arenas.remove(get(arena.getId()));   } catch (NullPointerException | InvalidInputException ignore) {}
		try { arenas.remove(get(arena.getName())); } catch (NullPointerException | InvalidInputException ignore) {}
		arenas.add(arena);
	}

	public static void remove(Arena arena) {
		arenas.remove(arena);
	}

	private static String getFile(String name) {
		return folder + name + ".yml";
	}

	private static FileConfiguration getConfig(String name) {
		File file = new File(getFile(name));
		if (!file.exists()) {
			try {
				if (!file.createNewFile())
					Nexus.warn("File " + file.getName() + " already exists");
			} catch (IOException ex) {
				Nexus.severe("An error occurred while trying to create a configuration file: " + ex.getMessage());
			}
		}

		return YamlConfiguration.loadConfiguration(file);
	}

	@SneakyThrows
	public static void read() {
		File file = Paths.get(folder).toFile();
		if (!file.exists()) file.createNewFile();
		arenas.clear();
		try (Stream<Path> paths = Files.walk(Paths.get(folder))) {
			paths.forEach(filePath -> {
				try {
					if (!Files.isRegularFile(filePath)) return;

					String name = filePath.getFileName().toString();
					if (name.startsWith(".")) return;
					if (!name.endsWith(".yml")) return;

					read(name.replace(".yml", ""));
				} catch (Exception ex) {
					Nexus.severe("An error occurred while trying to read arena configuration file " + filePath.getFileName().toFile() + ": " + ex.getMessage());
				}
			});
		} catch (Exception ex) {
			Nexus.severe("An error occurred while trying to read arena configuration files: " + ex.getMessage());
		}
	}

	public static void read(String name) {
		add((Arena) getConfig(name).get("arena"));
	}

	public static void write() {
		arenas.forEach(ArenaManager::write);
	}

	public static void write(Arena arena) {
		FileConfiguration arenaConfig = getConfig(arena.getName());
		arenaConfig.set("arena", arena);

		try {
			arenaConfig.save(getFile(arena.getName()));
			if (!arenas.contains(arena))
				add(arena);
		} catch (Exception ex) {
			Nexus.severe("An error occurred while trying to write arena configuration file " + arena.getName() + ": " + ex.getMessage());
		}
	}

	public static void delete(String arena) {
		delete(get(arena));
	}

	public static void delete(Arena arena) {
		File file = new File(getFile(arena.getName()));
		if (!file.exists()) {
			return;
		}

		file.delete();
		remove(arena);
	}

	public static int getNextId() {
		int id = 1;
		for (Arena arena : getAll())
			if (arena.getId() >= id)
				id = (arena.getId() + 1);

		return id;
	}

	@SneakyThrows
	public static <T> T convert(Arena arena, Class<?> clazz) {
		return (T) clazz.getDeclaredConstructor(Map.class).newInstance(arena.serialize());
	}

}
