package me.pugabyte.nexus.features.particles;

import lombok.NoArgsConstructor;
import lombok.NonNull;
import me.pugabyte.nexus.features.minigames.models.events.matches.MatchJoinEvent;
import me.pugabyte.nexus.features.minigames.models.events.matches.MinigamerQuitEvent;
import me.pugabyte.nexus.features.particles.effects.DotEffect;
import me.pugabyte.nexus.features.particles.effects.LineEffect;
import me.pugabyte.nexus.features.particles.providers.ParticleMenuProvider;
import me.pugabyte.nexus.framework.commands.models.CustomCommand;
import me.pugabyte.nexus.framework.commands.models.annotations.Arg;
import me.pugabyte.nexus.framework.commands.models.annotations.Path;
import me.pugabyte.nexus.framework.commands.models.annotations.Permission;
import me.pugabyte.nexus.framework.commands.models.events.CommandEvent;
import me.pugabyte.nexus.models.particle.ParticleOwner;
import me.pugabyte.nexus.models.particle.ParticleService;
import me.pugabyte.nexus.models.particle.ParticleType;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import static me.pugabyte.nexus.features.particles.Particles.startParticles;
import static me.pugabyte.nexus.features.particles.Particles.stopParticles;
import static me.pugabyte.nexus.utils.LocationUtils.getCenteredLocation;

@NoArgsConstructor
public class ParticlesCommand extends CustomCommand implements Listener {
	ParticleService service = new ParticleService();
	ParticleOwner particleOwner;

	public ParticlesCommand(@NonNull CommandEvent event) {
		super(event);
		particleOwner = service.get(player());
	}

	@Path
	void menu() {
		new ParticleMenuProvider().open(player());
	}

	@Path("<effectType>")
	@Permission("group.admin")
	void run(ParticleType particleType) {
		particleOwner.start(particleType);
	}

	@Path("stop <effectType>")
	void stop(ParticleType particleType) {
		particleOwner.cancel(particleType);
	}

	@Path("stopall")
	void stopAll() {
		particleOwner.cancel();
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		startParticles(event.getPlayer());
	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		stopParticles(event.getPlayer());
	}

	@EventHandler
	public void onMatchJoin(MatchJoinEvent event) {
		stopParticles(event.getMinigamer().getPlayer());
	}

	@EventHandler
	public void onMatchQuit(MinigamerQuitEvent event) {
		startParticles(event.getMinigamer().getPlayer());
	}

	@Path("line [distance] [density]")
	@Permission("group.admin")
	void line(@Arg("10") int distance, @Arg("0.1") double density) {
		LineEffect.builder().player(player()).distance(distance).density(density).rainbow(true).start();
	}

	@Path("dot")
	@Permission("group.admin")
	void dot() {
		Location loc = getCenteredLocation(location()).add(0, 1, 0);
		DotEffect.builder().player(player()).location(loc).ticks(10 * 20).rainbow(true).start();
	}
}
