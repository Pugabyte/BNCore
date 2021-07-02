package me.pugabyte.nexus.features.minigames.models.mechanics;

import me.pugabyte.nexus.features.minigames.mechanics.*;
import org.jetbrains.annotations.NotNull;

public enum MechanicType {
	ANVIL_DROP(new AnvilDrop()),
	ARCHERY(new Archery()),
	BATTLESHIP(new Battleship()),
	BINGO(new Bingo()),
	CAPTURE_THE_FLAG(new CaptureTheFlag()),
	DEATH_SWAP(new DeathSwap()),
	DOGFIGHTING(new Dogfighting()),
	FOUR_TEAM_DEATHMATCH(new FourTeamDeathmatch()),
	FREE_FOR_ALL(new FreeForAll()),
	GOLD_RUSH(new GoldRush()),
	GRAB_A_JUMBUCK(new GrabAJumbuck()),
	HIDE_AND_SEEK(new HideAndSeek()),
	HOLE_IN_THE_WALL(new HoleInTheWall()),
	HOLI_SPLEGG(new HoliSplegg()),
	INFECTION(new Infection()),
	INVERTO_INFERNO(new InvertoInferno()),
	JUGGERNAUT(new Juggernaut()),
	KANGAROO_JUMPING(new KangarooJumping()),
	MASTERMIND(new Mastermind()),
	MAZE(new Maze()),
	MONSTER_MAZE(new MonsterMaze()),
	MURDER(new Murder()),
	ONE_FLAG_CAPTURE_THE_FLAG(new OneFlagCaptureTheFlag()),
	ONE_IN_THE_QUIVER(new OneInTheQuiver()),
	PAINTBALL(new Paintball()),
	PARKOUR(new Parkour()),
	PIXEL_DROP(new PixelDrop()),
	PIXEL_PAINTERS(new PixelPainters()),
	QUAKE(new Quake()),
	SABOTAGE(new Sabotage()),
	SIEGE(new Siege()),
	SPLEEF(new Spleef()),
	SPLEGG(new Splegg()),
	TEAM_DEATHMATCH(new TeamDeathmatch()),
	THIMBLE(new Thimble()),
	TNT_RUN(new TNTRun()),
	UHC(new UHC()),
	UNCIVIL_ENGINEERS(new UncivilEngineers()),
	XRUN(new XRun());

	private final @NotNull Mechanic mechanic;

	MechanicType(@NotNull Mechanic mechanic) {
		this.mechanic = mechanic;
	}

	public @NotNull Mechanic get() {
		return mechanic;
	}

}
