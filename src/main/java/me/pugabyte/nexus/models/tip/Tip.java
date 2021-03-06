package me.pugabyte.nexus.models.tip;

import dev.morphia.annotations.Converters;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import eden.mongodb.serializers.UUIDConverter;
import eden.utils.TimeUtils.Time;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import me.pugabyte.nexus.framework.exceptions.postconfigured.PlayerNotOnlineException;
import me.pugabyte.nexus.framework.persistence.serializer.mongodb.LocationConverter;
import me.pugabyte.nexus.models.PlayerOwnedObject;
import me.pugabyte.nexus.models.cooldown.CooldownService;
import me.pugabyte.nexus.models.nerd.Rank;
import me.pugabyte.nexus.utils.RandomUtils;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

@Data
@Builder
@Entity("tip")
@NoArgsConstructor
@AllArgsConstructor
@RequiredArgsConstructor
@Converters({UUIDConverter.class, LocationConverter.class})
public class Tip implements PlayerOwnedObject {
	@Id
	@NonNull
	private UUID uuid;
	private List<TipType> received = new ArrayList<>();

	public boolean show(TipType tipType) {
		if (!isOnline())
			throw new PlayerNotOnlineException(getOfflinePlayer());

		if (!received.contains(tipType)) {
			received.add(tipType);
			new TipService().save(this);
			return true;
		}

		if (!new CooldownService().check(uuid, "Tip-" + tipType.name(), tipType.getCooldown()))
			return false;

		if (tipType.getPredicate() != null)
			if (!tipType.getPredicate().test(getOnlinePlayer()))
				return false;

		if (tipType.getRetryChance() > 0)
			return RandomUtils.chanceOf(tipType.getRetryChance());

		return true;
	}

	public enum TipType {
		CONCRETE(1, Time.HOUR, player -> player.hasPermission("group.nonstaff")),
		LWC_CHEST(1, Time.MINUTE.x(15), player -> Rank.of(player) == Rank.GUEST),
		LWC_FURNACE(1, Time.MINUTE.x(15), player -> Rank.of(player) == Rank.GUEST),
		RESOURCE_WORLD_STORAGE(15),
		SPAM_ATTACK(50, Time.MINUTE.x(5)),
		AUTOSORT_SORT_INVENTORY(1, Time.WEEK, player -> player.hasPermission("store.autosort")),
		AUTOSORT_SORT_CHESTS(1, Time.WEEK, player -> player.hasPermission("store.autosort")),
		AUTOSORT_REFILL(1, Time.WEEK, player -> player.hasPermission("store.autosort")),
		AUTOSORT_DEPOSIT_ALL(1, Time.WEEK, player -> player.hasPermission("store.autosort")),
		AUTOSORT_DEPOSIT_QUICK(1, Time.WEEK, player -> player.hasPermission("store.autosort"));

		@Getter
		private final int retryChance;
		@Getter
		private int cooldown;
		@Getter
		private Predicate<Player> predicate;

		TipType(int retryChance) {
			this.retryChance = retryChance;
		}

		TipType(int retryChance, int cooldown) {
			this.retryChance = retryChance;
			this.cooldown = cooldown;
		}

		TipType(int retryChance, Time cooldown, Predicate<Player> predicate) {
			this(retryChance, cooldown.get(), predicate);
		}

		TipType(int retryChance, int cooldown, Predicate<Player> predicate) {
			this.retryChance = retryChance;
			this.cooldown = cooldown;
			this.predicate = predicate;
		}
	}

}
