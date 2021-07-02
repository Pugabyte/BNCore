package me.pugabyte.nexus.features.radio;

import com.xxmicloxx.NoteBlockAPI.model.Song;
import com.xxmicloxx.NoteBlockAPI.songplayer.SongPlayer;
import me.pugabyte.nexus.framework.commands.models.CustomCommand;
import me.pugabyte.nexus.framework.commands.models.annotations.Arg;
import me.pugabyte.nexus.framework.commands.models.annotations.Confirm;
import me.pugabyte.nexus.framework.commands.models.annotations.ConverterFor;
import me.pugabyte.nexus.framework.commands.models.annotations.Description;
import me.pugabyte.nexus.framework.commands.models.annotations.Path;
import me.pugabyte.nexus.framework.commands.models.annotations.Permission;
import me.pugabyte.nexus.framework.commands.models.annotations.TabCompleterFor;
import me.pugabyte.nexus.framework.commands.models.events.CommandEvent;
import me.pugabyte.nexus.framework.exceptions.postconfigured.InvalidInputException;
import me.pugabyte.nexus.framework.features.Features;
import me.pugabyte.nexus.models.radio.RadioConfig;
import me.pugabyte.nexus.models.radio.RadioConfig.Radio;
import me.pugabyte.nexus.models.radio.RadioConfig.RadioSong;
import me.pugabyte.nexus.models.radio.RadioConfig.RadioType;
import me.pugabyte.nexus.models.radio.RadioConfigService;
import me.pugabyte.nexus.models.radio.RadioUser;
import me.pugabyte.nexus.models.radio.RadioUserService;
import me.pugabyte.nexus.utils.Name;
import me.pugabyte.nexus.utils.StringUtils;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static me.pugabyte.nexus.features.radio.RadioUtils.addPlayer;
import static me.pugabyte.nexus.features.radio.RadioUtils.getListenedRadio;
import static me.pugabyte.nexus.features.radio.RadioUtils.isInRangeOfRadiusRadio;
import static me.pugabyte.nexus.features.radio.RadioUtils.removePlayer;

public class RadioCommand extends CustomCommand {
	RadioConfigService configService = new RadioConfigService();
	RadioConfig config = configService.get0();
	RadioUserService userService = new RadioUserService();
	RadioUser user;

	public RadioCommand(CommandEvent event) {
		super(event);
		PREFIX = RadioFeature.PREFIX;

		if (isPlayer())
			user = userService.get(player());
	}

	@Path("join [radio]")
	@Description("Join a radio")
	void joinRadio(Radio radio) {
		if (user.isMute())
			error("You have muted all radios!");

		if (radio == null) {
			radio = RadioUtils.getRadiusRadio(player());
			if (radio == null)
				error("You are not near a radio!\nChoices: &e"
						+ RadioUtils.getServerRadios().stream().map(Radio::getId).collect(Collectors.joining("&3, &e")));
		}

		if (radio.getType().equals(RadioType.RADIUS) && !isInRangeOfRadiusRadio(player(), radio))
			error("You are not near that radio!");

		if (!radio.isEnabled())
			error("That radio is not enabled!");

		Radio listenedRadio = getListenedRadio(player(), true);
		if (listenedRadio != null)
			removePlayer(player(), listenedRadio);

		if (radio.getType().equals(RadioType.RADIUS))
			user.getLeftRadiusRadios().remove(radio.getId());

		addPlayer(player(), radio);
	}

	@Path("leave")
	@Description("Leave the listened radio")
	void leaveRadio() {
		Radio radio = getListenedRadio(player(), true);
		if (radio == null)
			return;

		removePlayer(player(), radio);
		if (radio.getType().equals(RadioType.RADIUS))
			user.getLeftRadiusRadios().add(radio.getId());
		else
			user.setLastServerRadioId(null);

		if (user.getLastServerRadio() != null)
			addPlayer(player(), user.getLastServerRadio());

		userService.save(user);
	}

	@Path("toggle")
	@Description("Toggles in between joining and leaving the server radio")
	void toggleRadio() {
		if (isNullOrEmpty(user.getServerRadioId()))
			joinRadio(user.getLastServerRadio());
		else
			leaveRadio();
	}

	@Path("volume <number>")
	@Description("Sets the volume for all radios")
	void volume(@Arg(value = "0", min = 0, max = 100) byte volume) {
		user.setVolume(volume);
		userService.save(user);

		send(PREFIX + "Volume set to: &e" + user.getVolume());
	}

	@Path("info")
	@Description("Shows info about the radio you are listening to")
	void songInfo() {
		Radio radio = RadioUtils.getListenedRadio(player(), true);

		if (radio == null)
			error("You are not listening to a radio!");

		SongPlayer songPlayer = radio.getSongPlayer();
		Song song = songPlayer.getSong();

		send(PREFIX + "Radio Info:");
		send("&3Radio: &e" + StringUtils.camelCase(radio.getId()));

		List<String> list = RadioUtils.getPlaylistHover(radio);
		send(json("&3Songs: &e[" + list.size() + "]").hover(list).loreize(false));

		send("&3Playing: &e" + song.getTitle() + " &3by &e" + song.getAuthor() + " &3(" + getSongPercent(songPlayer) + "%)");
		line();
	}

	@Path("mute [enable]")
	@Description("Mute all radios")
	void mute(Boolean enable) {
		if (enable) {
			user.setMute(true);
			leaveRadio();
			send(PREFIX + "Muted all radios.");
		} else {
			user.setMute(false);
			send(PREFIX + "Unmuted all radios.");
		}

		userService.save(user);
	}

	// Staff Commands

	@Path("players <radio>")
	@Description("Lists all players listening to the server radio")
	@Permission("group.staff")
	void listListeners(Radio radio) {
		Set<UUID> uuids = radio.getSongPlayer().getPlayerUUIDs();
		if (uuids.size() == 0)
			error("No players are listening to " + radio.getId());

		send(PREFIX + "Players listening to " + radio.getId() + ":");
		int ndx = 1;
		for (UUID uuid : uuids) {
			send("&3" + ndx++ + " &e" + Name.of(uuid));
		}
	}

	@Path("teleport <radio>")
	@Permission("group.staff")
	void teleport(Radio radio) {
		if (!radio.getType().equals(RadioType.RADIUS))
			error("You can only teleport to a radius radio");
		player().teleportAsync(radio.getLocation(), TeleportCause.COMMAND);
	}

	@Path("debugUser <player>")
	@Permission("group.admin")
	void debugUser(OfflinePlayer player) {
		RadioUser user = userService.get(player);

		send(PREFIX + "&3User Debug: ");
		send("&3Player: &e" + user.getNickname());
		send("&3Is Mute: &e" + user.isMute());
		send("&3ServerRadioId: &e" + user.getServerRadioId());
		send("&3LastServerRadioId: &e" + user.getLastServerRadioId());
		send("&3LeftRadiusRadios: &e" + user.getLeftRadiusRadios());
		line();
	}

	@Path("debugRadio <radio>")
	@Permission("group.admin")
	void debugRadio(Radio radio) {
		send(PREFIX + "&3Radio Debug: ");
		send("&3Id: &e" + radio.getId());
		send("&3Enabled: &e" + radio.isEnabled());
		send("&3Type: &e" + StringUtils.camelCase(radio.getType()));

		if (radio.getType().equals(RadioType.RADIUS)) {
			send("&3Location: &e" + StringUtils.getShortLocationString(radio.getLocation()));
			send("&3Radius: &e" + radio.getRadius());
			send("&3Particles: &e" + radio.isParticles());
		}

		List<String> list = RadioUtils.getPlaylistHover(radio);
		send(json("&3Songs: &e[" + list.size() + "]").hover(list));

		line();
	}

	@Path("reload")
	@Description("Reloads the config, and remakes every radio")
	@Permission("group.admin")
	void reloadConfig() {
		Features.get(RadioFeature.class).reload();
		send(PREFIX + "Config reloaded!");
	}

	// Config Commands

	@Path("config reload <radio>")
	@Description("Recreate a radio")
	@Permission("group.admin")
	void configReload(Radio radio) {
		if (!radio.isEnabled())
			error("Radio is not enabled.");
		radio.reload();
		send(PREFIX + StringUtils.camelCase(radio.getType()) + " Radio &e" + radio.getId() + " &3reloaded");
	}

	@Path("config create <type> <id> [radius]")
	@Description("Create a radio")
	@Permission("group.admin")
	void configCreate(RadioType type, String id, @Arg("0") int radius) {
		if (type.equals(RadioType.RADIUS)) {
			config.add(Radio.builder()
					.id(id)
					.type(RadioType.RADIUS)
					.radius(radius)
					.location(location())
					.build());

		} else if (type.equals(RadioType.SERVER)) {
			config.add(Radio.builder()
					.id(id)
					.type(RadioType.SERVER)
					.build());

		}
		configService.save(config);

		send(PREFIX + StringUtils.camelCase(type) + " Radio &e" + id + " &3created");
	}

	@Path("config setParticles <radio> <enable>")
	@Permission("group.admin")
	void configSetParticles(Radio radio, boolean enable) {
		if (!radio.getType().equals(RadioType.RADIUS))
			error("You can only set particles of a radius radio");

		radio.setParticles(enable);
		configService.save(config);

		send(PREFIX + "Particles set to " + radio.isParticles() + " for " + radio.getId());
	}

	@Path("config setType <radio> <type>")
	@Permission("group.admin")
	void configSetType(Radio radio, RadioType type) {
		radio.setType(type);
		configService.save(config);

		send(PREFIX + "Type set to " + radio.getType() + " for " + radio.getId());
	}

	@Path("config setRadius <radio> <radius>")
	@Permission("group.admin")
	void configSetRadius(Radio radio, int radius) {
		if (!radio.getType().equals(RadioType.RADIUS))
			error("You can only set radius of a radius radio");

		radio.setRadius(radius);
		if (radio.isEnabled())
			radio.reload();

		configService.save(config);

		send(PREFIX + "Radius set to " + radio.getRadius() + " for " + radio.getId());
	}

	@Path("config setLocation <radio>")
	@Permission("group.admin")
	void configSetLocation(Radio radio) {
		if (!radio.getType().equals(RadioType.RADIUS))
			error("You can only set location of a radius radio");

		radio.setLocation(location());
		if (radio.isEnabled())
			radio.reload();

		configService.save(config);

		send(PREFIX + "Location set to " + StringUtils.getShortLocationString(radio.getLocation()) + " for " + radio.getId());
	}

	@Path("config addSong <radio> <song>")
	@Description("Add a song to a radio")
	@Permission("group.admin")
	void configAddSong(Radio radio, @Arg(type = RadioSong.class) List<RadioSong> radioSongs) {
		for (RadioSong radioSong : radioSongs)
			config.addSong(radio, radioSong);

		configService.save(config);

		send(PREFIX + "Added " + radioSongs.stream().map(RadioSong::getName).collect(Collectors.joining(", ")) + " to " + radio.getId());
	}

	@Path("config removeSong <radio> <song>")
	@Description("Remove a song from a radio")
	@Permission("group.admin")
	void configRemoveSong(Radio radio, @Arg(context = 1) RadioSong radioSong) {
		config.removeSong(radio, radioSong);

		configService.save(config);
		send(PREFIX + "Removed " + radioSong.getName() + " from " + radio.getId());
	}

	@Path("config setId <radio> <id>")
	@Permission("group.admin")
	void configSetType(Radio radio, String id) {
		String oldId = radio.getId();
		radio.setId(id);
		configService.save(config);

		String newId = radio.getId();
		userService.clearCache();

		List<RadioUser> radioUsers = userService.getAll();
		for (RadioUser user : radioUsers) {
			if (user.getServerRadioId() != null && user.getServerRadioId().equalsIgnoreCase(oldId))
				user.setServerRadioId(newId);

			if (user.getLastServerRadioId() != null && user.getLastServerRadioId().equalsIgnoreCase(oldId))
				user.setLastServerRadioId(newId);

			if (user.getLeftRadiusRadios() != null && user.getLeftRadiusRadios().contains(oldId)) {
				user.getLeftRadiusRadios().remove(oldId);
				user.getLeftRadiusRadios().add(newId);
			}

			userService.save(user);
		}


		send(PREFIX + "Id set to " + radio.getId());
	}

	@Path("config enable <radio>")
	@Description("Enable the radio")
	@Permission("group.admin")
	void configEnable(Radio radio) {
		RadioFeature.verify(radio);

		radio.setEnabled(true);
		configService.save(config);

		send(PREFIX + "Enabled " + radio.getId());
	}

	@Path("config disable <radio>")
	@Description("Disable the radio")
	@Permission("group.admin")
	void configDisable(Radio radio) {
		radio.setEnabled(false);
		configService.save(config);

		send(PREFIX + "Disabled " + radio.getId());
	}

	@Confirm
	@Path("config delete <id>")
	@Description("Delete the radio")
	@Permission("group.admin")
	void configDelete(Radio radio) {
		RadioUtils.removeRadio(radio);

		send(PREFIX + "Radio &e" + radio.getId() + " &3deleted");
	}

	@ConverterFor(Radio.class)
	Radio convertToRadio(String value) {
		Radio radio = config.getById(value);
		if (radio == null)
			throw new InvalidInputException("Radio &e" + value + " &cnot found");
		return radio;
	}

	@TabCompleterFor(Radio.class)
	List<String> tabCompleteRadio(String filter) {
		return config.getRadios().stream()
				.filter(radio -> radio.getId().toLowerCase().startsWith(filter.toLowerCase()))
				.map(Radio::getId)
				.collect(Collectors.toList());
	}

	@ConverterFor(RadioSong.class)
	RadioSong convertToRadioSong(String value) {
		return RadioFeature.getRadioSongByName(value)
				.orElseThrow(() -> new InvalidInputException("Song &e" + value + " &cnot found"));
	}

	@TabCompleterFor(RadioSong.class)
	List<String> tabCompleteRadioSong(String filter, Radio context) {
		return RadioFeature.getAllSongs().stream()
				.filter(song -> song.getName().toLowerCase().startsWith(filter.toLowerCase()))
				.filter(song -> {
					if (context != null)
						return context.getSongs().contains(song.getName());
					return true;
				})
				.map(RadioSong::getName)
				.collect(Collectors.toList());
	}

	private int getSongPercent(SongPlayer songPlayer) {
		double songLen = songPlayer.getSong().getLength();
		double current = songPlayer.getTick();
		return (int) ((current / songLen) * 100.0);
	}
}
