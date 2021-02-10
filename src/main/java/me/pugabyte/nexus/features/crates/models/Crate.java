package me.pugabyte.nexus.features.crates.models;

import com.destroystokyo.paper.ParticleBuilder;
import com.gmail.filoghost.holographicdisplays.api.Hologram;
import com.gmail.filoghost.holographicdisplays.api.HologramsAPI;
import com.google.common.util.concurrent.AtomicDouble;
import lombok.Data;
import me.pugabyte.nexus.Nexus;
import me.pugabyte.nexus.features.crates.Crates;
import me.pugabyte.nexus.features.crates.models.events.CrateSpawnItemEvent;
import me.pugabyte.nexus.features.menus.MenuUtils;
import me.pugabyte.nexus.utils.*;
import org.bukkit.*;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static me.pugabyte.nexus.utils.SoundUtils.getPitch;

@Data
public abstract class Crate implements Listener {

	public Player player;
	public boolean inUse = false;
	public CrateLoot loot;
	public List<Hologram> crateHologram;
	public Item spawnedItem;

	public Crate() {
		Nexus.registerListener(this);
	}

	public abstract CrateType getCrateType();

	public abstract List<String> getCrateHologramLines();

	public Color[] getBandColors() {
		return new Color[]{Color.WHITE, Color.WHITE};
	}

	public void spawnHologram() {
		List<Hologram> holograms = new ArrayList<>();
		for (int i = 0; i < 2; i++) {
			Hologram hologram = HologramsAPI.createHologram(Nexus.getInstance(), getCrateType().getCenteredLocation().clone().add(0, 2, 0));
			for (String line : getCrateHologramLines())
				hologram.appendTextLine(StringUtils.colorize(line));
			hologram.getVisibilityManager().setVisibleByDefault(true);
			holograms.add(hologram);
		}
		crateHologram = holograms;
	}

	public void deleteHologram() {
		for (Hologram hologram : crateHologram)
			hologram.delete();
	}

	public void showHologram() {
		for (Hologram hologram : crateHologram)
			hologram.getVisibilityManager().setVisibleByDefault(true);
	}

	public void hideHologram() {
		for (Hologram hologram : crateHologram)
			hologram.getVisibilityManager().setVisibleByDefault(false);
	}

	public void openCrate(Location location, Player player) {
		if (inUse) return;
		this.player = player;
		inUse = true;
		pickCrateLoot();
		if (!canHoldItems(player)) return;
		takeKey();
		hideHologram();
		playAnimationSound(location);
		playAnimation(location).thenAccept(finalLocation -> {
			playFinalSound(location);
			playFinalParticle(finalLocation);
			spawnItem(finalLocation, loot.getDisplayItem());
		});
		Tasks.wait(Time.SECOND.x(7), () -> {
			giveItems();
			reset();
		});
	}

	public void openMultiple(Location location, Player player, int amount) {
		if (inUse) return;
		this.player = player;
		MenuUtils.ConfirmationMenu.builder()
				.title("Open " + amount + " Crates?")
				.onConfirm(e -> {
					player.closeInventory();
					try {
						pickCrateLoot();
						if (!canHoldItems(player)) return;
						takeKey();
						hideHologram();
						playAnimationSound(location);
						playAnimation(location).thenAccept(finalLocation -> {
							AtomicInteger wait = new AtomicInteger(0);
							Tasks.wait(Time.SECOND.x(wait.getAndAdd(1)), () -> {
								playFinalSound(location);
								playFinalParticle(finalLocation);
								spawnItem(finalLocation, loot.getDisplayItem());
							});
							List<Integer> tasks = new ArrayList<>();
							for (int i = 0; i < amount - 1; i++) {
								int j = i;
								tasks.add(Tasks.wait(Time.SECOND.x(wait.getAndAdd(1)), () -> {
									giveItems();
									removeItem();
									pickCrateLoot();
									if (!canHoldItems(player)) {
										tasks.forEach(Tasks::cancel);
										return;
									}
									takeKey();
									playFinalSound(location);
									playFinalParticle(finalLocation);
									spawnItem(finalLocation, loot.getDisplayItem());
									if (j == amount - 2)
										Tasks.wait(Time.SECOND.x(3), () -> {
											giveItems();
											reset();
										});
								}));
							}
						});
					} catch (CrateOpeningException ex) {
						if (ex.getMessage() != null)
							PlayerUtils.send(player, Crates.PREFIX + ex.getMessage());
						reset();
					}
				})
				.open(player);
	}

	public boolean canHoldItems(Player player) {
		if (!PlayerUtils.hasRoomFor(player, loot.getItems().toArray(new ItemStack[0]))) {
			PlayerUtils.send(player, Crates.PREFIX + "You must clear room in your inventory before you can open crates");
			reset();
			return false;
		}
		return true;
	}

	public void pickCrateLoot() {
		Map<CrateLoot, Double> original = new HashMap<>();
		Crates.getLootByType(getCrateType()).stream().filter(CrateLoot::isActive)
				.forEach(crateLoot -> original.put(crateLoot, crateLoot.getWeight()));

		if (original.size() == 0)
			throw new CrateOpeningException("&3Coming soon...");

		LinkedHashMap<CrateLoot, Double> sorted = new LinkedHashMap<>();
		original.entrySet().stream().sorted(Map.Entry.comparingByValue())
				.forEachOrdered(e -> sorted.put(e.getKey(), e.getValue()));

		LinkedHashMap<CrateLoot, Double> percentages = new LinkedHashMap<>();
		double max = 0;
		for (double i : sorted.values())
			max += i;
		for (Map.Entry<CrateLoot, Double> entry : sorted.entrySet())
			percentages.put(entry.getKey(), ((entry.getValue() / max) * 100));

		LinkedHashMap<CrateLoot, Integer> normalized = new LinkedHashMap<>();
		LinkedHashMap<CrateLoot, Double> temp = new LinkedHashMap<>();
		while (percentages.values().toArray(new Double[0])[0] < 1) {
			for (Map.Entry<CrateLoot, Double> entry : percentages.entrySet()) {
				temp.put(entry.getKey(), percentages.get(entry.getKey()) * 10);
			}
			percentages = temp;
		}
		percentages.forEach((key, value) -> normalized.put(key, value.intValue()));

		LinkedHashMap<Integer, List<CrateLoot>> combined = new LinkedHashMap<>();
		normalized.forEach((key, value) -> {
			if (!combined.containsKey(value))
				combined.put(value, new ArrayList<>());
			combined.get(value).add(key);
		});

		int rarity = 0;
		Integer[] percents = normalized.values().toArray(new Integer[0]);
		int random = (int) (Math.random() * percents[percents.length - 1]) + 1;
		for (int i : percents)
			if (random <= i) {
				rarity = i;
				break;
			}

		List<CrateLoot> list = combined.get(rarity);
		int random2 = (int) (Math.random() * list.size());
		loot = list.get(random2);
	}

	public CompletableFuture<Location> playAnimation(Location location) {
		final AtomicDouble radius = new AtomicDouble(.2);
		final AtomicReference<Double> y = new AtomicReference<>(0d);
		final AtomicDouble t = new AtomicDouble(0);
		final AtomicReference<Location> locationReference = new AtomicReference<>(location);
		final CompletableFuture<Location> finalLocation = new CompletableFuture<>();

		int taskId = Tasks.repeat(0, 1, () -> {
			y.updateAndGet(v -> v - Math.PI / 16);
			for (int i = 0; i < 50; i++) {
				for (int band = 0; band < 2; band++) {
					double x = radius.get() * (2 * Math.PI - t.get()) * Math.cos(t.get() + y.get() + band * Math.PI);
					double yPos = 0.5 * t.get();
					double z = radius.get() * (2 * Math.PI - t.get()) * Math.sin(t.get() + y.get() + band * Math.PI);
					locationReference.set(location.clone().add(x, yPos, z));
					new ParticleBuilder(org.bukkit.Particle.REDSTONE)
							.color(getBandColors()[band])
							.count(1)
							.location(locationReference.get())
							.spawn();
				}
				t.set(t.get() + .002);
			}
		});

		Tasks.wait(Time.SECOND.x(3), () -> {
			finalLocation.complete(locationReference.get());
			Tasks.cancel(taskId);
		});

		return finalLocation;
	}

	public void playAnimationSound(Location location) {
		int wait = 3;
		float volume = .6F;
		World w = location.getWorld();
		Tasks.wait(wait += 0, () -> {
			w.playSound(location, Sound.BLOCK_NOTE_BLOCK_HARP, SoundCategory.RECORDS, .6F, getPitch(3));
			w.playSound(location, Sound.BLOCK_NOTE_BLOCK_HARP, .6F, getPitch(3));
			w.playSound(location, Sound.BLOCK_NOTE_BLOCK_HARP, .6F, getPitch(7));
			w.playSound(location, Sound.BLOCK_NOTE_BLOCK_HARP, .6F, getPitch(10));
			w.playSound(location, Sound.BLOCK_NOTE_BLOCK_SNARE, .5F, getPitch(24));
		});
		Tasks.wait(wait += 3, () -> w.playSound(location, Sound.BLOCK_NOTE_BLOCK_HARP, volume, getPitch(3)));
		Tasks.wait(wait += 3, () -> w.playSound(location, Sound.BLOCK_NOTE_BLOCK_HARP, volume, getPitch(5)));
		Tasks.wait(wait += 3, () -> w.playSound(location, Sound.BLOCK_NOTE_BLOCK_HARP, volume, getPitch(6)));
		Tasks.wait(wait += 3, () -> w.playSound(location, Sound.BLOCK_NOTE_BLOCK_HARP, volume, getPitch(7)));
		Tasks.wait(wait += 3, () -> {
			w.playSound(location, Sound.BLOCK_NOTE_BLOCK_HARP, .6F, getPitch(5));
			w.playSound(location, Sound.BLOCK_NOTE_BLOCK_HARP, .6F, getPitch(9));
			w.playSound(location, Sound.BLOCK_NOTE_BLOCK_HARP, .6F, getPitch(12));
			w.playSound(location, Sound.BLOCK_NOTE_BLOCK_SNARE, .5F, getPitch(24));
		});
		Tasks.wait(wait += 3, () -> w.playSound(location, Sound.BLOCK_NOTE_BLOCK_HARP, volume, getPitch(5)));
		Tasks.wait(wait += 3, () -> w.playSound(location, Sound.BLOCK_NOTE_BLOCK_HARP, volume, getPitch(7)));
		Tasks.wait(wait += 3, () -> w.playSound(location, Sound.BLOCK_NOTE_BLOCK_HARP, volume, getPitch(8)));
		Tasks.wait(wait += 3, () -> w.playSound(location, Sound.BLOCK_NOTE_BLOCK_HARP, volume, getPitch(9)));
		Tasks.wait(wait += 3, () -> {
			w.playSound(location, Sound.BLOCK_NOTE_BLOCK_HARP, .6F, getPitch(7));
			w.playSound(location, Sound.BLOCK_NOTE_BLOCK_HARP, .6F, getPitch(10));
			w.playSound(location, Sound.BLOCK_NOTE_BLOCK_HARP, .6F, getPitch(14));
			w.playSound(location, Sound.BLOCK_NOTE_BLOCK_SNARE, .5F, getPitch(24));
		});
		Tasks.wait(wait += 3, () -> w.playSound(location, Sound.BLOCK_NOTE_BLOCK_HARP, volume, getPitch(7)));
		Tasks.wait(wait += 3, () -> w.playSound(location, Sound.BLOCK_NOTE_BLOCK_HARP, volume, getPitch(9)));
		Tasks.wait(wait += 3, () -> w.playSound(location, Sound.BLOCK_NOTE_BLOCK_HARP, volume, getPitch(10)));
		Tasks.wait(wait += 3, () -> w.playSound(location, Sound.BLOCK_NOTE_BLOCK_HARP, volume, getPitch(11)));
		Tasks.wait(wait += 3, () -> {
			w.playSound(location, Sound.BLOCK_NOTE_BLOCK_HARP, .6F, getPitch(9));
			w.playSound(location, Sound.BLOCK_NOTE_BLOCK_HARP, .6F, getPitch(13));
			w.playSound(location, Sound.BLOCK_NOTE_BLOCK_HARP, .6F, getPitch(16));
			w.playSound(location, Sound.BLOCK_NOTE_BLOCK_SNARE, .5F, getPitch(24));
		});
		Tasks.wait(wait += 3, () -> w.playSound(location, Sound.BLOCK_NOTE_BLOCK_HARP, volume, getPitch(9)));
		Tasks.wait(wait += 3, () -> w.playSound(location, Sound.BLOCK_NOTE_BLOCK_HARP, volume, getPitch(11)));
		Tasks.wait(wait += 3, () -> w.playSound(location, Sound.BLOCK_NOTE_BLOCK_HARP, volume, getPitch(12)));
		Tasks.wait(wait + 3, () -> w.playSound(location, Sound.BLOCK_NOTE_BLOCK_HARP, volume, getPitch(13)));
	}

	public void playFinalSound(Location location) {
		int wait = 0;
		float volume = .6F;
		World w = location.getWorld();
		Tasks.wait(wait += 3, () -> {
			w.playSound(location, Sound.BLOCK_NOTE_BLOCK_HARP, volume, getPitch(13));
			w.playSound(location, Sound.BLOCK_NOTE_BLOCK_HARP, volume, getPitch(17));
			w.playSound(location, Sound.BLOCK_NOTE_BLOCK_HARP, volume, getPitch(8));
			w.playSound(location, Sound.BLOCK_NOTE_BLOCK_SNARE, volume, getPitch(24));
		});
		Tasks.wait(wait += 3, () -> w.playSound(location, Sound.BLOCK_NOTE_BLOCK_HARP, volume, getPitch(18)));
		Tasks.wait(wait += 2, () -> w.playSound(location, Sound.BLOCK_NOTE_BLOCK_HARP, volume, getPitch(20)));
		Tasks.wait(wait + 2, () -> w.playSound(location, Sound.BLOCK_NOTE_BLOCK_HARP, volume, getPitch(25)));
	}

	public void playFinalParticle(Location location) {
		List<Material> dyes = Arrays.stream(Material.values()).filter(material -> material.name().contains("DYE"))
				.collect(Collectors.toList());

		for (int i = 0; i < 50; i++) {
			new ParticleBuilder(org.bukkit.Particle.ITEM_CRACK)
					.location(location)
					.count(5)
					.offset(.1, .1, .1)
					.extra(1)
					.data(new ItemStack(dyes.get((int) (Math.random() * dyes.size()))))
					.spawn();
		}
	}

	public Item spawnItem(Location location, ItemStack itemStack) {
		Item item = location.getWorld().dropItem(location, itemStack);
		item.setVelocity(new Vector(0, 0, 0));
		item.setCanPlayerPickup(false);
		item.setCustomNameVisible(true);
		item.setCustomName(StringUtils.colorize(loot.getTitle()));
		spawnedItem = item;
		new CrateSpawnItemEvent(player, loot).callEvent();
		return item;
	}

	public void removeItem() {
		if (spawnedItem != null)
			spawnedItem.remove();
	}

	public void giveItems() {
		ItemUtils.giveItems(player, loot.getItems());
	}

	public void takeKey() {
		ItemStack key = getCrateType().getKey();
		for (ItemStack item : player.getInventory().getContents())
			if (ItemUtils.isFuzzyMatch(key, item)) {
				item.setAmount(item.getAmount() - 1);
				break;
			}
	}

	public void reset() {
		inUse = false;
		showHologram();
		removeItem();
	}
}
