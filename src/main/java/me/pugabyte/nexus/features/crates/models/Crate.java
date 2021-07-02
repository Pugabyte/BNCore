package me.pugabyte.nexus.features.crates.models;

import com.destroystokyo.paper.ParticleBuilder;
import com.gmail.filoghost.holographicdisplays.api.Hologram;
import com.gmail.filoghost.holographicdisplays.api.HologramsAPI;
import com.google.common.util.concurrent.AtomicDouble;
import eden.utils.TimeUtils.Time;
import lombok.Data;
import me.pugabyte.nexus.Nexus;
import me.pugabyte.nexus.features.crates.Crates;
import me.pugabyte.nexus.features.crates.models.events.CrateSpawnItemEvent;
import me.pugabyte.nexus.features.crates.models.exceptions.CrateOpeningException;
import me.pugabyte.nexus.features.menus.MenuUtils;
import me.pugabyte.nexus.utils.ItemUtils;
import me.pugabyte.nexus.utils.PlayerUtils;
import me.pugabyte.nexus.utils.RandomUtils;
import me.pugabyte.nexus.utils.SoundUtils.Jingle;
import me.pugabyte.nexus.utils.StringUtils;
import me.pugabyte.nexus.utils.Tasks;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

	public Location getHologramLocation() {
		Location loc = getCrateType().getCenteredLocation().clone().add(0, 1, 0);
		for (String string : getCrateHologramLines())
			loc.add(0, 5 / 16d, 0);
		return loc;
	}

	public void spawnHologram() {
		List<Hologram> holograms = new ArrayList<>();
		for (int i = 0; i < 2; i++) {
			Hologram hologram = HologramsAPI.createHologram(Nexus.getInstance(), getHologramLocation());
			for (String line : getCrateHologramLines())
				hologram.appendTextLine(StringUtils.colorize(line));
			hologram.getVisibilityManager().setVisibleByDefault(true);
			holograms.add(hologram);
		}
		crateHologram = holograms;
	}

	public void deleteHologram() {
		if (crateHologram != null)
			for (Hologram hologram : crateHologram)
				hologram.delete();
	}

	public void showHologram() {
		if (crateHologram != null)
			for (Hologram hologram : crateHologram)
				hologram.getVisibilityManager().setVisibleByDefault(true);
	}

	public void hideHologram() {
		if (crateHologram != null)
			for (Hologram hologram : crateHologram)
				hologram.getVisibilityManager().setVisibleByDefault(false);
	}

	public void openCrate(Location location, Player player) {
		if (inUse) return;
		this.player = player;
		inUse = true;
		pickCrateLoot();
		if (!canHoldItems(player))
			return;

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
						if (inUse) return;
						inUse = true;
						pickCrateLoot();
						if (!canHoldItems(player)) return;
						takeKey();
						hideHologram();
						playAnimationSound(location);
						playAnimation(location).thenAccept(finalLocation -> {
							try {
								AtomicInteger wait = new AtomicInteger(0);
								Tasks.wait(Time.SECOND.x(wait.getAndAdd(1)), () -> {
									try {
										playFinalSound(location);
										playFinalParticle(finalLocation);
										spawnItem(finalLocation, loot.getDisplayItem());
									} catch (CrateOpeningException ex) {
										if (ex.getMessage() != null)
											PlayerUtils.send(player, Crates.PREFIX + ex.getMessage());
										reset();
									}
								});
								List<Integer> tasks = new ArrayList<>();
								for (int i = 0; i < amount - 1; i++) {
									int j = i;
									tasks.add(Tasks.wait(Time.SECOND.x(wait.getAndAdd(1)), () -> {
										try {
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
										} catch (CrateOpeningException ex) {
											if (ex.getMessage() != null)
												PlayerUtils.send(player, Crates.PREFIX + ex.getMessage());
											tasks.forEach(Tasks::cancel);
											reset();
										}
									}));
								}
							} catch (CrateOpeningException ex) {
								if (ex.getMessage() != null)
									PlayerUtils.send(player, Crates.PREFIX + ex.getMessage());
								reset();
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
		if (!PlayerUtils.hasRoomFor(player, loot.getItems().toArray(ItemStack[]::new))) {
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

		loot = RandomUtils.getWeightedRandom(original);
	}

	public Particle getParticleType() {
		return Particle.REDSTONE;
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
					ParticleBuilder builder = new ParticleBuilder(getParticleType())
							.count(1)
							.extra(0.01)
							.location(locationReference.get());
					if (getParticleType() == Particle.REDSTONE)
						builder.color(getBandColors()[band]);
					builder.spawn();
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
		Jingle.CRATE_OPEN.play(location);
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
		try {
			Item item = location.getWorld().dropItem(location, itemStack);
			item.setVelocity(new Vector(0, 0, 0));
			item.setCanPlayerPickup(false);
			item.setCustomNameVisible(true);
			item.setCustomName(StringUtils.colorize(loot.getTitle()));
			spawnedItem = item;
			new CrateSpawnItemEvent(player, loot, getCrateType()).callEvent();
			return item;
		} catch (Exception ex) {
			player.getInventory().addItem(getCrateType().getKey());
			throw new CrateOpeningException("There was an error while trying to play the crate animation");
		}
	}

	public void removeItem() {
		if (spawnedItem != null)
			spawnedItem.remove();
	}

	public void giveItems() {
		PlayerUtils.giveItems(player, loot.getItems());
	}

	public void takeKey() {
		try {
			boolean took = false;
			ItemStack key = getCrateType().getKey();
			for (ItemStack item : player.getInventory().getContents()) {
				if (ItemUtils.isNullOrAir(item)) continue;
				if (ItemUtils.isFuzzyMatch(key, item)) {
					item.setAmount(item.getAmount() - 1);
					took = true;
					break;
				}
			}
			if (!took) throw new CrateOpeningException("no key present");
		} catch (Exception ex) {
			throw new CrateOpeningException("You must have a key in your inventory");
		}
	}

	public void reset() {
		inUse = false;
		showHologram();
		removeItem();
	}
}
