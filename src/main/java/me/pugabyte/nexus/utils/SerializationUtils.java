package me.pugabyte.nexus.utils;

import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static java.util.stream.Collectors.toList;

public class SerializationUtils {

	public static class YML {

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
			return materials.stream().map(Material::name).sorted().collect(toList());
		}

		public static Set<Material> deserializeMaterialSet(List<String> materials) {
			if (materials == null) return null;
			return new LinkedHashSet<>(materials.stream()
					.map(material -> Material.matchMaterial(material.toUpperCase()))
					.filter(Objects::nonNull)
					.sorted()
					.toList());
		}

	}

	public static class JSON {

		public static String toString(Map<String, Object> map) {
			Gson gson = new Gson();
			return gson.toJson(gson.toJsonTree(map, Map.class));
		}

		public static String toString(List<Map<String, Object>> list) {
			Gson gson = new Gson();
			return gson.toJson(gson.toJsonTree(list, List.class));
		}

		public static Map<String, Object> fromString(String value) {
			return new Gson().fromJson(value, Map.class);
		}

		public static List<Map<String, Object>> fromStringToList(String value) {
			return new Gson().fromJson(value, List.class);
		}

		/** Bukkit ConfigurationSerializable */

		public static List<Map<String, Object>> serialize(List<ConfigurationSerializable> values) {
			return new ArrayList<>() {{
				for (ConfigurationSerializable value : values)
					add(serialize(value));
			}};
		}

		public static Map<String, Object> serialize(ConfigurationSerializable value) {
			if (value == null) return null;
			Map<String, Object> serialized = serializeRecursive(value.serialize());
			serialized.put(ConfigurationSerialization.SERIALIZED_TYPE_KEY, ConfigurationSerialization.getAlias(value.getClass()));
			addExtraValues(serialized, value);
			return serialized;
		}

		public static List<Object> deserialize(List<Map<String, Object>> values) {
			return new ArrayList<>() {{
				for (Map<String, Object> value : values)
					add(deserializeRecursive(value));
			}};
		}

		public static List<ItemStack> deserializeItemStacks(String value) {
			return deserializeItemStacks(fromStringToList(value));
		}

		public static List<ItemStack> deserializeItemStacks(List<Map<String, Object>> values) {
			return new ArrayList<>() {{
				for (Map<String, Object> value : values)
					add(deserializeItemStack(value));
			}};
		}

		public static ItemStack deserializeItemStack(String value) {
			return deserializeItemStack(fromString(value));
		}

		public static ItemStack deserializeItemStack(Map<String, Object> value) {
			if (value == null) return null;
			value.computeIfPresent("meta", ($, meta) -> {
				Map<String, Object> metaMap = meta instanceof String ? fromString((String) meta) : (Map<String, Object>) meta;
				fixItemMetaClasses(metaMap);
				return deserializeRecursive(metaMap);
			});

			value.putIfAbsent("v", 2586);

			ItemStack deserialize = ItemStack.deserialize(value);
			if (deserialize.getAmount() == 0)
				deserialize.setAmount(1);
			return deserialize;
		}

		private static Map<String, Object> serializeRecursive(Map<String, Object> serialized) {
			Map<String, Object> fixed = new HashMap<>(serialized);
			serialized.forEach((key, value) -> fixed.put(key, serializeRecursive(value)));
			return fixed;
		}

		private static Object serializeRecursive(Object value) {
			if (value == null)
				return null;

			if (value instanceof ConfigurationSerializable configurationSerializable)
				return serialize(configurationSerializable);

			if (Collection.class.isAssignableFrom(value.getClass()))
				if (((Collection<?>) value).iterator().hasNext())
					return ImmutableList.copyOf(new ArrayList<>() {{
						for (Object object : ((Collection<?>) value))
							add(serializeRecursive(object));
					}});

			if (Map.class.isAssignableFrom(value.getClass()))
				return serializeRecursive((Map<String, Object>) value);

			return value;
		}

		private static Object deserializeRecursive(Map<String, Object> values) {
			Map<String, Object> fixed = new HashMap<>(values);
			values.forEach(((key, value) -> {
				if (Collection.class.isAssignableFrom(value.getClass())) {
					fixed.put(key, new ArrayList<>() {{
						for (Object next : (Collection<?>) value) {
							if (next == null || !Map.class.isAssignableFrom(next.getClass())) {
								add(next);
								continue;
							}

							add(deserializeRecursive((Map<String, Object>) next));
						}
					}});
				}

				if (Map.class.isAssignableFrom(value.getClass()))
					fixed.put(key, deserializeRecursive((Map<String, Object>) value));
			}));

			if (fixed.containsKey("==")) {
				try { fixItemMetaClasses(fixed); } catch (ClassCastException ignore) {}
				return ConfigurationSerialization.deserializeObject(fixed);
			}

			return fixed;
		}

		private static void addExtraValues(Map<String, Object> serialized, ConfigurationSerializable value) {
			if (value instanceof ItemStack itemStack)
				serialized.computeIfAbsent("amount", $ -> itemStack.getAmount());
		}

		private static final List<String> intKeys = Arrays.asList("power", "repair-cost", "Damage", "map-id", "generation", "custom-model-data",
				"effect", "duration", "amplifier", "fish-variant", "LodestonePosX", "LodestonePosY", "LodestonePosZ");
		// MongoDB deserializes some properties as the wrong class, do conversion
		private static void fixItemMetaClasses(Map<String, Object> deserialized) {
			intKeys.forEach(key ->
					deserialized.computeIfPresent(key, ($, metaValue) -> {
						if (metaValue instanceof Number number)
							return number.intValue();
						return metaValue;
					}));

			Arrays.asList("enchants", "stored-enchants", "display-map-color").forEach(key ->
					deserialized.computeIfPresent(key, ($, metaValue) ->
							toIntMap((Map<String, Object>) metaValue)));
		}

		private static Map<String, Object> toIntMap(Map<String, Object> map) {
			return new HashMap<>() {{
				map.forEach((key, value) -> {
					put(key, value);
					if (value instanceof Number number)
						put(key, number.intValue());
				});
			}};
		}

		/** Location */

		@NotNull
		public static String serializeLocation(Location location) {
			DecimalFormat nf = new DecimalFormat("#.000");
			return location.getWorld().getName() + "," +
					nf.format(location.getX()) + "," +
					nf.format(location.getY()) + "," +
					nf.format(location.getZ()) + "," +
					nf.format(location.getYaw()) + "," +
					nf.format(location.getPitch());
		}

		@NotNull
		public static Location deserializeLocation(String in) {
			List<String> parts = Arrays.asList(in.split(","));
			return new Location(
					Bukkit.getWorld(parts.get(0)),
					Double.parseDouble(parts.get(1)),
					Double.parseDouble(parts.get(2)),
					Double.parseDouble(parts.get(3)),
					Float.parseFloat(parts.get(4)),
					Float.parseFloat(parts.get(5))
			);
		}

		public static class LocationGsonSerializer implements JsonSerializer<Location> {

			@Override
			public JsonElement serialize(Location location, Type type, JsonSerializationContext context) {
				return new JsonPrimitive(StringUtils.getShortishLocationString(location));
			}

		}

	}
}
