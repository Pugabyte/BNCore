package me.pugabyte.nexus.features.events.y2021.bearfair21.quests.npcs;

import lombok.AllArgsConstructor;
import lombok.Getter;
import me.pugabyte.nexus.utils.CitizensUtils;
import me.pugabyte.nexus.utils.PacketUtils;
import net.citizensnpcs.api.npc.NPC;
import net.minecraft.world.entity.decoration.EntityArmorStand;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static eden.utils.StringUtils.camelCase;

@AllArgsConstructor
public enum BearFair21NPC {
	// MAIN
	//   MERCHANTS
	ARCHITECT("Zach", 4109),
	ARTIST("Sage", 2657),
	BAKER("Rye", 2659),
	BARTENDER("Cosmo", 2655),
	BLACKSMITH("Alvor", 2656),
	BOTANIST("Fern", 2661),
	CARPENTER("Ron", 4110),
	COLLECTOR("Mercury", 2750),
	FISHERMAN1("Gage", 2653),
	INVENTOR("Joshua", 2660),
	PASTRY_CHEF("Maple", 2654),
	SORCERER("Lucian", 2658),
	TRADER("Joe", 4206),
	//   QUEST GIVERS
	MAYOR("John", 3838) {
		@Override
		public @NotNull String getNpcNameAndJob() {
			return camelCase(this) + " " + getNpcName();
		}
	},
	LUMBERJACK("Flint", 3845),
	BEEKEEPER("Harold", 3844),
	FISHERMAN2("Nate", 3841),
	AERONAUT("Skye", 4111),
	CURATOR("Curator", 4207),
	QUEEN_BEE("Queen Bee", 4208),
	//   MISC
	ADMIRAL("Phoenix", 3839),
	ORGANIZER("Wakka", 3798),

	// MGN
	AXEL("Axel", 4126),
	XAVIER("Xavier", 4127),
	RYAN("Ryan", 4128),
	HEATHER("Heather", 4134),
	MGN_CUSTOMER_1("Trent", 4135),
	MGN_CUSTOMER_2("Mr. Fredrickson", 4136),
	JAMES("James", 4167),

	// PUGMAS
	PUGMAS_MAYOR("Mayor", 4130),
	GRINCH("Grinch", 4129),
	GRINCH_1("Grinch", 4168),
	PUGMAS_VILLAGER_1("Villager", 4142),
	PUGMAS_VILLAGER_2("Villager", 4143),
	PUGMAS_VILLAGER_3("Villager", 4144),
	PUGMAS_VILLAGER_4("Villager", 4145),
	PUGMAS_VILLAGER_5("Villager", 4146),
	PUGMAS_VILLAGER_6("Villager", 4147),
	PUGMAS_VILLAGER_7("Villager", 4148),
	PUGMAS_VILLAGER_8("Villager", 4149),
	PUGMAS_VILLAGER_9("Villager", 4150),
	PUGMAS_VILLAGER_10("Villager", 4151),
	PUGMAS_VILLAGER_11("Villager", 4152),
	PUGMAS_VILLAGER_12("Villager", 4153),
	PUGMAS_VILLAGER_13("Villager", 4154),
	PUGMAS_VILLAGER_14("Villager", 4155),

	// HALLOWEEN
	JOSE("Jose", 4131),
	SANTIAGO("Santiago", 4132),
	ANA("Ana", 4133),
	FRANCISCO("Francisco", 4171),
	ADRIAN("Adrian", 4174),
	MAXIM("Maxim", 4173),
	ISABELLA("Isabella", 4175),
	JUAN("Juan", 4179),
	LOLA("Lola", 4178),
	JENNA("Jenna", 4177),
	RICARDO("Ricardo", 4182),
	LUIS("Luis", 4180),
	MARIANA("Mariana", 4181),
	HALLOWEEN_MAYOR("Mayor", 4172),
	RODRIGO("Rodrigo", 4185),
	DANIEL("Daniel", 4187),
	SANDRA("Sandra", 4188),
	MARTHA("Martha", 4189),
	PATRICIA("Patricia", 4190),
	NINA("Nina", 4191),
	RUBEN("Ruben", 4192),
	CLARENCE("Clarence", 4193),
	CARLA("Carla", 4194),
	ANTONIO("Antonio", 4195),

	// SDU
	BRUCE("Bruce", 4209),
	KYLIE("Kylie", 4210),
	MEL_GIBSON("Mel Gibson", 4211),
	MILO("Milo", 4213)
	;

	@Getter
	private final String npcName;
	@Getter
	private final int id;

	public static BearFair21NPC from(int id) {
		for (BearFair21NPC bearFair21NPC : values()) {
			if (bearFair21NPC.getId() == id)
				return bearFair21NPC;
		}
		return null;
	}

	public static BearFair21NPC of(Integer id) {
		return Arrays.stream(values()).filter(bearFair21NPC -> bearFair21NPC.getId() == id).findFirst().orElse(null);
	}

	public @Nullable NPC getNPC() {
		return CitizensUtils.getNPC(this.id);
	}

	public List<EntityArmorStand> showName(Player player) {
		NPC npc = getNPC();
		if (npc == null) return null;

		String npcJob = getNpcJob();

		if (npcJob.equalsIgnoreCase(npcName))
			return Collections.singletonList(PacketUtils.entityNameFake(player, npc.getEntity(), npcName));
		else
			return PacketUtils.entityNameFake(player, npc.getEntity(), npcJob, npcName);
	}

	@NotNull
	public String getNpcNameAndJob() {
		final String job = getNpcJob();
		if (npcName.equalsIgnoreCase(job))
			return npcName;
		return npcName + " the " + job;
	}

	@NotNull
	public String getNpcJob() {
		return camelCase(this.name().toLowerCase()
			.replaceAll("(pugmas_)|(mgn_)|(sdu_)|(halloween_)|(main_)", "")
			.replaceAll("[0-9]+", "")).trim();
	}
}
