package me.pugabyte.nexus.features.events.y2021.bearfair21.fairgrounds;

import eden.utils.TimeUtils.Time;
import lombok.NoArgsConstructor;
import me.pugabyte.nexus.features.events.y2021.bearfair21.BearFair21;
import me.pugabyte.nexus.utils.RandomUtils;
import me.pugabyte.nexus.utils.SoundBuilder;
import me.pugabyte.nexus.utils.Tasks;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@NoArgsConstructor
public class Interactables {
	private static boolean strengthTest_active = false;

	public static void strengthTest() {
		if (strengthTest_active) return;
		strengthTest_active = true;

		Location one = new Location(BearFair21.getWorld(), 66, 136, -16);
		Location two = new Location(BearFair21.getWorld(), 67, 136, -16);
		Location three = new Location(BearFair21.getWorld(), 69, 138, -16);
		Location four = new Location(BearFair21.getWorld(), 69, 139, -16);
		Location five = new Location(BearFair21.getWorld(), 69, 140, -16);
		Location six = new Location(BearFair21.getWorld(), 69, 141, -16);
		Location seven = new Location(BearFair21.getWorld(), 69, 142, -16);
		List<Location> lights = Arrays.asList(one, two, three, four, five, six, seven);

		int max = 7;
		int limit = RandomUtils.randomInt(1, 7);
		AtomicInteger count = new AtomicInteger();
		AtomicInteger wait = new AtomicInteger(0);
		AtomicBoolean breakBool = new AtomicBoolean(false);
		for (Location location : lights) {
			Tasks.wait(wait.get(), () -> {
				if (!breakBool.get()) {
					location.getBlock().setType(Material.REDSTONE_BLOCK);
					count.getAndIncrement();
					if (count.get() == limit) {
						if (count.get() == max)
							new SoundBuilder(Sound.BLOCK_BELL_USE).location(one).pitch(2).play();
						else
							new SoundBuilder(Sound.BLOCK_BELL_USE).location(one).pitch(0.1).play();
						breakBool.set(true);
					}
				}
			});
			if (breakBool.get())
				break;
			wait.getAndAdd(4);
		}

		wait.getAndAdd(Time.SECOND.x(1));

		Tasks.wait(wait.get(), () -> {
			for (Location light : lights) {
				light.getBlock().setType(Material.AIR);
			}
			strengthTest_active = false;
		});
	}
}
