package me.pugabyte.nexus.features.minigames.models.perks;

import dev.morphia.annotations.Converters;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import me.pugabyte.nexus.features.minigames.Minigames;
import me.pugabyte.nexus.features.minigames.models.Arena;
import me.pugabyte.nexus.framework.persistence.serializer.mongodb.UUIDConverter;
import me.pugabyte.nexus.models.PlayerOwnedObject;
import me.pugabyte.nexus.utils.PlayerUtils;
import me.pugabyte.nexus.utils.RandomUtils;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static me.pugabyte.nexus.utils.StringUtils.plural;

@Data
@Builder
@Entity("perk_owner")
@NoArgsConstructor
@AllArgsConstructor
@RequiredArgsConstructor
@Converters(UUIDConverter.class)
public class PerkOwner extends PlayerOwnedObject {
	public static final PerkOwnerService service = new PerkOwnerService();
	private static final int MAX_DAILY_TOKENS = 20;

	@Id
	@NonNull
	private UUID uuid;
	private Map<PerkType, Boolean> purchasedPerks = new HashMap<>();
	private int tokens = 0;
	private int dailyTokens = 0;
	private LocalDate tokenDate = LocalDate.now();
	private HideParticle hideParticle = HideParticle.NONE;

	public Set<PerkType> getEnabledPerks() {
		Set<PerkType> perks = new HashSet<>();
		purchasedPerks.forEach((perkType, enabled) -> {
			if (enabled)
				perks.add(perkType);
		});
		return perks;
	}

	public <T extends Perk> Set<T> getEnabledPerksByClass(Class<T> tClass) {
		return getEnabledPerks().stream().filter(perkType -> tClass.isInstance(perkType.getPerk())).map(perkType -> tClass.cast(perkType.getPerk())).collect(Collectors.toSet());
	}

	public boolean equals(PerkOwner other) {
		return uuid.equals(other.getUuid());
	}

	public boolean purchase(PerkType perk) {
		if (purchasedPerks.containsKey(perk))
			return false;
		if (perk.getPerk().getPrice() > tokens)
			return false;
		tokens -= perk.getPerk().getPrice();
		purchasedPerks.put(perk, false);
		service.save(this);
		return true;
	}

	/**
	 * Rewards the user for winning a minigame
	 */
	public void reward(Arena arena) {
		LocalDate date = LocalDate.now();
		if (date.isAfter(tokenDate)) {
			tokenDate = date;
			dailyTokens = 0;
		}
		int amount = Math.min(MAX_DAILY_TOKENS-dailyTokens, RandomUtils.randomInt(5, 10));
		if (amount > 0) {
			tokens += amount;
			dailyTokens += amount;
			PlayerUtils.send(uuid, Minigames.PREFIX + "You won &e" + amount + plural(" token", amount) + "&3 for winning &e" + arena.getName());
			if (dailyTokens == MAX_DAILY_TOKENS)
				PlayerUtils.send(uuid, Minigames.PREFIX + "You've earned the maximum tokens for today");
		}
		service.save(this);
	}

	/**
	 * Enables or disables a perk
	 * @return whether or not the user had the perk
	 */
	public boolean toggle(PerkType perkType) {
		if (!purchasedPerks.containsKey(perkType))
			return false;

		boolean setTo = !purchasedPerks.get(perkType);
		// disable other perk types if this is being enabled and this is part of an exclusive perk category
		if (setTo && perkType.getPerk().getPerkCategory().isExclusive())
			(new HashSet<>(purchasedPerks.keySet())).stream().filter(perkType::excludes).forEach(otherType -> purchasedPerks.put(otherType, false));

		purchasedPerks.put(perkType, setTo);

		service.save(this);
		return true;
	}
}