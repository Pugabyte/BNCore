package me.pugabyte.nexus.utils;

import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.Predicate;

public class BiomeTag {
	public static final BiomeTag OCEAN = new BiomeTag(Material.WATER_BUCKET).append("OCEAN", MatchMode.CONTAINS);
	public static final BiomeTag TAIGA = new BiomeTag(Material.SPRUCE_LOG).append("TAIGA", MatchMode.CONTAINS);
	public static final BiomeTag ICE_SPIKES = new BiomeTag(Material.PACKED_ICE).append(Biome.ICE_SPIKES);
	public static final BiomeTag ALL_NETHER = new BiomeTag(Material.NETHERRACK).append(Biome.NETHER_WASTES, Biome.BASALT_DELTAS,
		Biome.CRIMSON_FOREST, Biome.WARPED_FOREST, Biome.SOUL_SAND_VALLEY);

	private final EnumSet<Biome> biomes;
	@Getter
	private final Material material;

	public BiomeTag(Material material) {
		this.biomes = EnumSet.noneOf(Biome.class);
		this.material = material;
	}

	public BiomeTag append(Biome... biomes) {
		this.biomes.addAll(Arrays.asList(biomes));
		return this;
	}

	@SafeVarargs
	public final BiomeTag append(Tag<Biome>... biomeTags) {
		for (Tag<Biome> biomeTag : biomeTags)
			this.biomes.addAll(biomeTag.getValues());

		return this;
	}

	public BiomeTag append(Predicate<Biome> predicate) {
		for (Biome m : Biome.values())
			if (predicate.test(m))
				this.biomes.add(m);
		return this;
	}

	public BiomeTag append(String segment, BiomeTag.MatchMode mode) {
		append(segment, mode, Biome.values());
		return this;
	}

	public BiomeTag append(String segment, BiomeTag.MatchMode mode, Biome[] biomes) {
		segment = segment.toUpperCase();

		switch (mode) {
			case PREFIX:
				for (Biome m : biomes)
					if (m.name().startsWith(segment))
						this.biomes.add(m);
				break;

			case SUFFIX:
				for (Biome m : biomes)
					if (m.name().endsWith(segment))
						this.biomes.add(m);
				break;

			case CONTAINS:
				for (Biome m : biomes)
					if (m.name().contains(segment))
						this.biomes.add(m);
				break;
		}

		return this;
	}

	public BiomeTag exclude(Biome... biomes) {
		for (Biome m : biomes)
			this.biomes.remove(m);

		return this;
	}

	@SafeVarargs
	public final BiomeTag exclude(Tag<Biome>... biomeTags) {
		for (Tag<Biome> biomeTag : biomeTags)
			this.biomes.removeAll(biomeTag.getValues());

		return this;
	}

	public BiomeTag exclude(Predicate<Biome> predicate) {
		biomes.removeIf(predicate);
		return this;
	}

	public BiomeTag exclude(String segment, BiomeTag.MatchMode mode) {
		exclude(segment, mode, Biome.values());
		return this;
	}

	public BiomeTag exclude(String segment, BiomeTag.MatchMode mode, Biome[] biomes) {
		segment = segment.toUpperCase();

		switch (mode) {
			case PREFIX:
				for (Biome m : biomes)
					if (m.name().startsWith(segment))
						this.biomes.remove(m);
				break;

			case SUFFIX:
				for (Biome m : biomes)
					if (m.name().endsWith(segment))
						this.biomes.remove(m);
				break;

			case CONTAINS:
				for (Biome m : biomes)
					if (m.name().contains(segment))
						this.biomes.remove(m);
				break;
		}

		return this;
	}

	public Set<Biome> getValues() {
		return biomes;
	}

	public Biome[] toArray() {
		return new ArrayList<>(biomes).toArray(Biome[]::new);
	}

	public boolean isTagged(@NotNull Biome biome) {
		return biomes.contains(biome);
	}

	public boolean isTagged(@NotNull Block block) {
		return isTagged(block.getBiome());
	}

	@Override
	public String toString() {
		return biomes.toString();
	}

	public enum MatchMode {
		PREFIX,
		SUFFIX,
		CONTAINS
	}
}
