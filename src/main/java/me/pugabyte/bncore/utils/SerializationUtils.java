package me.pugabyte.bncore.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class SerializationUtils {
	public static Map<String, ItemStack> serializeItems(ItemStack[] itemStacks) {
		Map<String, ItemStack> items = new HashMap<>();
		int slot = 0;
		for (ItemStack item : itemStacks) {
			if (item != null)
				items.put(String.valueOf(slot), item);
			slot++;
		}

		return items;
	}

	public static ItemStack[] deserializeItems(Map<String, Object> items) {
		ItemStack[] inventory = new ItemStack[41];
		if (items == null) return inventory;

		for (Map.Entry<String, Object> item : items.entrySet())
			inventory[Integer.parseInt(item.getKey())] = (ItemStack) item.getValue();

		return inventory;
	}

	public static List<String> serializeMaterialSet(Set<Material> materials) {
		if (materials == null) return null;
		return new ArrayList<String>(){{ addAll(materials.stream().map(Material::name).collect(Collectors.toList())); }};
	}

	public static Set<Material> deserializeMaterialSet(List<String> materials) {
		if (materials == null) return null;
		return materials.stream().map(block -> Material.valueOf(block.toUpperCase())).collect(Collectors.toSet());
	}

	public static String serializeLocation(Location location) {
		DecimalFormat nf = new DecimalFormat("#.000");
		return location.getWorld().getName() + "," +
				nf.format(location.getX()) + "," +
				nf.format(location.getY()) + "," +
				nf.format(location.getZ()) + "," +
				nf.format(location.getYaw()) + "," +
				nf.format(location.getPitch());
	}

	public static Location deserializeLocation(String in) {
		List<String> parts = Arrays.asList(in.split(","));
		return new Location(Bukkit.getWorld(parts.get(0)),
				Double.parseDouble(parts.get(1)),
				Double.parseDouble(parts.get(2)),
				Double.parseDouble(parts.get(3)),
				Float.parseFloat(parts.get(4)),
				Float.parseFloat(parts.get(5))
		);
	}
}
