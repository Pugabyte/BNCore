package me.pugabyte.bearnation.server.features.commands;

import lombok.NonNull;
import me.pugabyte.bearnation.api.framework.commands.models.CustomCommand;
import me.pugabyte.bearnation.api.framework.commands.models.annotations.Aliases;
import me.pugabyte.bearnation.api.framework.commands.models.annotations.ConverterFor;
import me.pugabyte.bearnation.api.framework.commands.models.annotations.Path;
import me.pugabyte.bearnation.api.framework.commands.models.annotations.Redirects.Redirect;
import me.pugabyte.bearnation.api.framework.commands.models.events.CommandEvent;
import org.bukkit.WeatherType;

import java.util.Arrays;

@Aliases({"rain", "snow", "pweather"})
@Redirect(from = { "/rainoff", "/snowoff" }, to = "/rain off")
public class PlayerWeatherCommand extends CustomCommand {

	public PlayerWeatherCommand(@NonNull CommandEvent event) {
		super(event);
	}

	@Path("<weather>")
	void run(WeatherType type) {
		player().setPlayerWeather(type);
		send(PREFIX + "Weather set to " + camelCase(type));
	}

	@Path("off")
	void off() {
		run(WeatherType.CLEAR);
	}

	@Path("reset")
	void reset() {
		player().resetPlayerWeather();
		send(PREFIX + "Weather synced with server");
	}

	@ConverterFor(WeatherType.class)
	WeatherType convertToWeatherType(String value) {
		if (Arrays.asList("storm", "rain").contains(value))
			value = WeatherType.DOWNFALL.name();
		if (Arrays.asList("sun", "none").contains(value))
			value = WeatherType.CLEAR.name();
		return (WeatherType) convertToEnum(WeatherType.class, value);
	}

}
