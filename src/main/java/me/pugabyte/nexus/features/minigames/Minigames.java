package me.pugabyte.nexus.features.minigames;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import lombok.Getter;
import me.lucko.helper.Services;
import me.lucko.helper.scoreboard.PacketScoreboard;
import me.lucko.helper.scoreboard.PacketScoreboardProvider;
import me.pugabyte.nexus.Nexus;
import me.pugabyte.nexus.features.minigames.lobby.ActionBar;
import me.pugabyte.nexus.features.minigames.lobby.Basketball;
import me.pugabyte.nexus.features.minigames.lobby.Parkour;
import me.pugabyte.nexus.features.minigames.lobby.TickPerks;
import me.pugabyte.nexus.features.minigames.managers.ArenaManager;
import me.pugabyte.nexus.features.minigames.managers.MatchManager;
import me.pugabyte.nexus.features.minigames.managers.PlayerManager;
import me.pugabyte.nexus.features.minigames.menus.MinigamesMenus;
import me.pugabyte.nexus.features.minigames.models.Match;
import me.pugabyte.nexus.features.minigames.models.MatchData;
import me.pugabyte.nexus.features.minigames.models.Minigamer;
import me.pugabyte.nexus.features.minigames.models.annotations.MatchDataFor;
import me.pugabyte.nexus.features.minigames.models.mechanics.Mechanic;
import me.pugabyte.nexus.features.minigames.models.mechanics.MechanicType;
import me.pugabyte.nexus.features.minigames.models.modifiers.MinigameModifier;
import me.pugabyte.nexus.framework.features.Feature;
import me.pugabyte.nexus.models.minigamessetting.MinigamesSetting;
import me.pugabyte.nexus.models.minigamessetting.MinigamesSettingService;
import me.pugabyte.nexus.utils.AdventureUtils;
import me.pugabyte.nexus.utils.PlayerUtils;
import me.pugabyte.nexus.utils.StringUtils;
import me.pugabyte.nexus.utils.Utils;
import me.pugabyte.nexus.utils.WorldEditUtils;
import me.pugabyte.nexus.utils.WorldGroup;
import me.pugabyte.nexus.utils.WorldGuardUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import org.reflections.Reflections;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class Minigames extends Feature {
	public static final String PREFIX = StringUtils.getPrefix("Minigames");
	public static final Component COMPONENT_PREFIX = AdventureUtils.getPrefix("Minigames");
	public static final int PERK_TICK_DELAY = 4;
	@Getter
	public static final MinigamesMenus menus = new MinigamesMenus();
	@Getter
	public static final PacketScoreboard scoreboard = Services.load(PacketScoreboardProvider.class).getScoreboard();
	@Getter
	private static final Set<UUID> testModePlayers = new HashSet<>();

	@Override
	public void onStart() {
		registerSerializables();
		registerMatchDatas();
		ArenaManager.read();
		registerListeners();

		new ActionBar();
		new Basketball();
		new Parkour();
		new TickPerks();
	}

	public static World getWorld() {
		return Bukkit.getWorld("gameworld");
	}

	@Deprecated // Use Match#getWGUtils or Arena#getWGUtils
	public static WorldGuardUtils getWorldGuardUtils() {
		return new WorldGuardUtils(getWorld());
	}

	@Deprecated // Use Match#getWEUtils or Arena#getWEUtils
	public static WorldEditUtils getWorldEditUtils() {
		return new WorldEditUtils(getWorld());
	}

	public static Location getLobby() {
		return new Location(getWorld(), 1861.5, 38.1, 247.5, 0, 0);
	}

	public static ProtectedRegion getLobbyRegion() {
		return getWorldGuardUtils().getProtectedRegion("minigamelobby");
	}

	@Override
	public void onStop() {
		new ArrayList<>(MatchManager.getAll()).forEach(Match::end);
		ArenaManager.write();
	}

	public static boolean isMinigameWorld(World world) {
		return WorldGroup.of(world) == WorldGroup.MINIGAMES;
	}

	public static List<Player> getPlayers() {
		return PlayerUtils.getOnlinePlayers().stream()
			.filter(player -> isMinigameWorld(player.getWorld()))
			.collect(Collectors.toList());
	}

	public static List<Minigamer> getMinigamers() {
		return getPlayers().stream().map(PlayerManager::get).collect(Collectors.toList());
	}

	public static List<Minigamer> getActiveMinigamers() {
		return getPlayers().stream().map(PlayerManager::get).filter(minigamer -> minigamer.getMatch() != null).collect(Collectors.toList());
	}

	public static void broadcast(String announcement) {
		getPlayers().forEach(player -> PlayerUtils.send(player, Minigames.PREFIX + announcement));

		// TODO: If arena is public, announce to discord and whole server
	}

	public static void broadcast(ComponentLike component) {
		if (component == null) return;
		final Component message = Minigames.COMPONENT_PREFIX.append(component);
		getPlayers().forEach(player -> player.sendMessage(message));
		System.out.println(AdventureUtils.asPlainText(message));
	}

	// Registration

	private String getPath() {
		return this.getClass().getPackage().getName();
	}

	private void registerListeners() {
		for (Class<? extends Listener> clazz : new Reflections(getPath() + ".listeners").getSubTypesOf(Listener.class))
			Utils.tryRegisterListener(clazz);
	}

	private void registerSerializables() {
		new Reflections(getPath()).getTypesAnnotatedWith(SerializableAs.class).forEach(clazz -> {
			String alias = clazz.getAnnotation(SerializableAs.class).value();
			ConfigurationSerialization.registerClass((Class<? extends ConfigurationSerializable>) clazz, alias);
		});
	}

	@Getter
	private static final Map<Mechanic, Constructor<?>> matchDataMap = new HashMap<>();

	public static void registerMatchDatas() {
		try {
			String path = Minigames.class.getPackage().getName();
			Set<Class<? extends MatchData>> matchDataTypes = new Reflections(path + ".models.matchdata")
					.getSubTypesOf(MatchData.class);

			for (Class<?> matchDataType : matchDataTypes)
				if (matchDataType.getAnnotation(MatchDataFor.class) != null)
					for (MechanicType mechanicType : MechanicType.values())
						for (Class<? extends Mechanic> superclass : mechanicType.get().getSuperclasses())
							for (Class<? extends Mechanic> applicableMechanic : matchDataType.getAnnotation(MatchDataFor.class).value())
							if (applicableMechanic.equals(superclass))
								try {
									Constructor<?> constructor = matchDataType.getConstructor(Match.class);
									constructor.setAccessible(true);
									matchDataMap.put(mechanicType.get(), constructor);
								} catch (NoSuchMethodException ex) {
									Nexus.warn("MatchData " + matchDataType.getSimpleName() + " has no Match constructor");
								}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public static @NotNull MinigameModifier getModifier() {
		return new MinigamesSettingService().get0().getModifier();
	}

	public static void setModifier(@NotNull MinigameModifier modifier) {
		MinigamesSettingService service = new MinigamesSettingService();
		MinigamesSetting setting = service.get0();
		setting.setModifier(modifier);
		service.save(setting);
	}

}
