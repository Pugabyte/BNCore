package me.pugabyte.nexus.utils;

import me.lexikiq.HasPlayer;
import me.pugabyte.nexus.features.commands.MuteMenuCommand.MuteMenuProvider.MuteMenuItem;
import me.pugabyte.nexus.models.mutemenu.MuteMenuUser;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

import java.util.Collection;

@SuppressWarnings({"ConstantConditions", "UnusedAssignment"})
public class SoundUtils {
	private static final float defaultVolume = 0.5F;

	@Deprecated
	public static void playSound(HasPlayer player, Sound sound) {
		playSound(player, sound, SoundCategory.MASTER);
	}

	@Deprecated
	public static void playSound(HasPlayer player, Sound sound, SoundCategory category) {
		playSound(player, sound, category, defaultVolume, 1);
	}

	@Deprecated
	public static void playSound(HasPlayer player, Sound sound, float volume, float pitch) {
		playSound(player, sound, SoundCategory.MASTER, volume, pitch);
	}

	@Deprecated
	public static void playSound(HasPlayer player, Sound sound, SoundCategory category, float volume, float pitch) {
		Player _player = player.getPlayer();
		_player.playSound(_player.getLocation(), sound, category, volume, pitch);
	}

	@Deprecated
	public static void playSound(Location location, Sound sound) {
		playSound(location, sound, SoundCategory.MASTER);
	}

	@Deprecated
	public static void playSound(Location location, Sound sound, SoundCategory category) {
		playSound(location, sound, category, defaultVolume, 1);
	}

	@Deprecated
	public static void playSound(Location location, Sound sound, float volume, float pitch) {
		playSound(location, sound, SoundCategory.MASTER, volume, pitch);
	}

	@Deprecated
	public static void playSound(Location location, Sound sound, SoundCategory category, float volume, float pitch) {
		location.getWorld().playSound(location, sound, category, volume, pitch);
	}

	public static void stopSound(HasPlayer player, Sound sound) {
		stopSound(player, sound, null);
	}

	public static void stopSound(HasPlayer player, Sound sound, SoundCategory category) {
		player.getPlayer().stopSound(sound, category);
	}

	public enum Jingle {
		PING {
			@Override
			public void play(HasPlayer player) {
				if (MuteMenuUser.hasMuted(player.getPlayer(), MuteMenuItem.ALERTS))
					return;

				float volume = getMuteMenuVolume(player, MuteMenuItem.ALERTS);

				new SoundBuilder(Sound.ENTITY_ARROW_HIT_PLAYER).reciever(player).volume(volume).play();
			}

			@Override
			public void play(Location location) {
			}
		},

		RANKUP {
			@Override
			public void play(HasPlayer player) {
				if (MuteMenuUser.hasMuted(player.getPlayer(), MuteMenuItem.RANK_UP))
					return;

				float volume = getMuteMenuVolume(player, MuteMenuItem.RANK_UP);

				SoundBuilder harp = new SoundBuilder(Sound.BLOCK_NOTE_BLOCK_HARP).reciever(player).category(SoundCategory.RECORDS).volume(volume);
				SoundBuilder bell = new SoundBuilder(Sound.BLOCK_NOTE_BLOCK_BELL).reciever(player).category(SoundCategory.RECORDS).volume(volume);
				SoundBuilder flute = new SoundBuilder(Sound.BLOCK_NOTE_BLOCK_FLUTE).reciever(player).category(SoundCategory.RECORDS).volume(volume);

				int wait = 0;
				Tasks.wait(wait += 0, () -> {
					harp.pitch(0.749154F).play();
					bell.pitch(0.749154F).play();
				});
				Tasks.wait(wait += 4, () -> {
					harp.pitch(0.561231F).play();
					bell.pitch(0.561231F).play();
				});
				Tasks.wait(wait += 4, () -> {
					harp.pitch(0.629961F).play();
					bell.pitch(0.629961F).play();
				});
				Tasks.wait(wait += 2, () -> {
					harp.pitch(0.707107F).play();
					bell.pitch(0.707107F).play();
				});
				Tasks.wait(wait += 2, () -> {
					harp.pitch(0.840896F).play();
					bell.pitch(0.840896F).play();
				});
				Tasks.wait(wait += 2, () -> {
					flute.pitch(1.122462F).play();
					bell.pitch(1.122462F).play();
				});
			}

			@Override
			public void play(Location location) {
			}
		},

		FIRST_JOIN {
			@Override
			public void play(HasPlayer player) {
				if (MuteMenuUser.hasMuted(player.getPlayer(), MuteMenuItem.FIRST_JOIN_SOUND))
					return;

				float volume = getMuteMenuVolume(player, MuteMenuItem.FIRST_JOIN_SOUND);

				SoundBuilder chime = new SoundBuilder(Sound.BLOCK_NOTE_BLOCK_CHIME).reciever(player).category(SoundCategory.RECORDS).volume(volume);
				SoundBuilder bell = new SoundBuilder(Sound.BLOCK_NOTE_BLOCK_BELL).reciever(player).category(SoundCategory.RECORDS).volume(volume);

				int wait = 0;
				Tasks.wait(wait += 0, () -> {
					chime.pitch(0.561231F).play();
					bell.pitch(0.561231F).play();
				});
				Tasks.wait(wait += 2, () -> {
					chime.pitch(0.629961F).play();
					bell.pitch(0.629961F).play();
				});
				Tasks.wait(wait += 2, () -> {
					chime.pitch(0.561231F).play();
					bell.pitch(0.561231F).play();
				});
				Tasks.wait(wait += 2, () -> {
					chime.pitch(0.840896F).play();
					bell.pitch(0.840896F).play();
				});
			}

			@Override
			public void play(Location location) {
			}
		},

		JOIN {
			@Override
			public void play(HasPlayer player) {
				if (MuteMenuUser.hasMuted(player.getPlayer(), MuteMenuItem.JOIN_QUIT))
					return;

				float volume = getMuteMenuVolume(player, MuteMenuItem.JOIN_QUIT_SOUNDS);

				SoundBuilder harp = new SoundBuilder(Sound.BLOCK_NOTE_BLOCK_HARP).reciever(player).category(SoundCategory.RECORDS).volume(volume);

				int wait = 0;
				Tasks.wait(wait += 0, () -> harp.pitch(0.5F).play());
				Tasks.wait(wait += 2, () -> harp.pitch(0.667420F).play());
				Tasks.wait(wait += 2, () -> harp.pitch(0.749154F).play());
				Tasks.wait(wait += 2, () -> harp.pitch(1.0F).play());
			}

			@Override
			public void play(Location location) {
			}
		},

		QUIT {
			@Override
			public void play(HasPlayer player) {
				if (MuteMenuUser.hasMuted(player.getPlayer(), MuteMenuItem.JOIN_QUIT))
					return;

				float volume = getMuteMenuVolume(player, MuteMenuItem.JOIN_QUIT_SOUNDS);

				SoundBuilder harp = new SoundBuilder(Sound.BLOCK_NOTE_BLOCK_HARP).reciever(player).category(SoundCategory.RECORDS).volume(volume);

				int wait = 0;
				Tasks.wait(wait += 0, () -> harp.pitch(0.707107F).play());
				Tasks.wait(wait += 4, () -> harp.pitch(0.629961F).play());
				Tasks.wait(wait += 4, () -> harp.pitch(0.707107F).play());
				Tasks.wait(wait += 4, () -> harp.pitch(0.529732F).play());
			}

			@Override
			public void play(Location location) {
			}
		},

		BATTLESHIP_MISS {
			@Override
			public void play(HasPlayer player) {
				int wait = 0;


				Tasks.wait(wait += 0, () -> new SoundBuilder(Sound.UI_TOAST_IN).reciever(player).volume(defaultVolume).play());
				Tasks.wait(wait += 9, () -> new SoundBuilder(Sound.ENTITY_GENERIC_SPLASH).reciever(player).volume(defaultVolume).play());
			}

			@Override
			public void play(Location location) {
			}
		},

		BATTLESHIP_HIT {
			@Override
			public void play(HasPlayer player) {
				int wait = 0;
				Tasks.wait(wait += 0, () -> new SoundBuilder(Sound.UI_TOAST_IN).reciever(player).volume(defaultVolume).play());
				Tasks.wait(wait += 9, () -> new SoundBuilder(Sound.ENTITY_GENERIC_EXPLODE).reciever(player).volume(defaultVolume).play());
				Tasks.wait(wait += 8, () -> new SoundBuilder(Sound.BLOCK_FIRE_AMBIENT).reciever(player).volume(defaultVolume).play());
			}

			@Override
			public void play(Location location) {
			}
		},

		BATTLESHIP_SINK {
			@Override
			public void play(HasPlayer player) {
				SoundBuilder explode = new SoundBuilder(Sound.ENTITY_GENERIC_EXPLODE).reciever(player).volume(defaultVolume);
				SoundBuilder splash = new SoundBuilder(Sound.ENTITY_GENERIC_SPLASH).reciever(player).volume(defaultVolume);
				SoundBuilder fire = new SoundBuilder(Sound.BLOCK_FIRE_AMBIENT).reciever(player).volume(defaultVolume).pitch(0.1);

				int wait = 0;
				Tasks.wait(wait, () -> {
					explode.play();
					splash.play();
				});
				Tasks.wait(wait += RandomUtils.randomInt(2, 5), () -> {
					explode.play();
					fire.play();
				});
				Tasks.wait(wait += RandomUtils.randomInt(2, 5), () -> {
					explode.play();
					splash.play();
				});
				Tasks.wait(wait += RandomUtils.randomInt(2, 5), explode::play);
				Tasks.wait(wait += RandomUtils.randomInt(1, 3), explode::play);
				Tasks.wait(wait += RandomUtils.randomInt(1, 4), splash::play);
			}

			@Override
			public void play(Location location) {
			}
		},

		TREE_FELLER {
			@Override
			public void play(HasPlayer player) {
				SoundBuilder armorStandBreak = new SoundBuilder(Sound.ENTITY_ARMOR_STAND_BREAK).reciever(player).volume(defaultVolume);

				Tasks.wait(0, () -> armorStandBreak.pitch(randomPitch()).play());
				Tasks.wait(1, () -> armorStandBreak.pitch(randomPitch()).play());
				Tasks.wait(2, () -> armorStandBreak.pitch(randomPitch()).play());
				Tasks.wait(3, () -> {
					armorStandBreak.pitch(randomPitch()).play();
					new SoundBuilder(Sound.BLOCK_CROP_BREAK).reciever(player).volume(defaultVolume).pitch(0.1).play();
				});
				Tasks.wait(4, () -> {
					armorStandBreak.pitch(randomPitch()).play();
					new SoundBuilder(Sound.BLOCK_SHROOMLIGHT_STEP).reciever(player).volume(defaultVolume).pitch(0.1).play();
				});
				Tasks.wait(5, () -> {
					armorStandBreak.pitch(randomPitch()).play();
					new SoundBuilder(Sound.ENTITY_HORSE_SADDLE).reciever(player).volume(defaultVolume).pitch(0.1).play();
				});
				Tasks.wait(6, () -> armorStandBreak.pitch(2).play());
			}

			@Override
			public void play(Location location) {
			}
		},
		CRATE_OPEN {
			@Override
			public void play(Location location) {
				SoundBuilder harp = new SoundBuilder(Sound.BLOCK_NOTE_BLOCK_HARP).volume(0.6).location(location);
				SoundBuilder snare = new SoundBuilder(Sound.BLOCK_NOTE_BLOCK_SNARE).volume(0.5).location(location);

				int wait = 3;
				Tasks.wait(wait += 0, () -> {
					harp.pitchStep(3).play();
					harp.pitchStep(7).play();
					harp.pitchStep(10).play();
					snare.pitchStep(24).play();
				});
				Tasks.wait(wait += 3, () -> harp.pitchStep(3).play());
				Tasks.wait(wait += 3, () -> harp.pitchStep(5).play());
				Tasks.wait(wait += 3, () -> harp.pitchStep(6).play());
				Tasks.wait(wait += 3, () -> harp.pitchStep(7).play());
				Tasks.wait(wait += 3, () -> {
					harp.pitchStep(5).play();
					harp.pitchStep(9).play();
					harp.pitchStep(12).play();
					snare.pitchStep(24).play();
				});
				Tasks.wait(wait += 3, () -> harp.pitchStep(5).play());
				Tasks.wait(wait += 3, () -> harp.pitchStep(7).play());
				Tasks.wait(wait += 3, () -> harp.pitchStep(8).play());
				Tasks.wait(wait += 3, () -> harp.pitchStep(9).play());
				Tasks.wait(wait += 3, () -> {
					harp.pitchStep(7).play();
					harp.pitchStep(10).play();
					harp.pitchStep(14).play();
					snare.pitchStep(24).play();
				});
				Tasks.wait(wait += 3, () -> harp.pitchStep(7).play());
				Tasks.wait(wait += 3, () -> harp.pitchStep(9).play());
				Tasks.wait(wait += 3, () -> harp.pitchStep(10).play());
				Tasks.wait(wait += 3, () -> harp.pitchStep(11).play());
				Tasks.wait(wait += 3, () -> {
					harp.pitchStep(9).play();
					harp.pitchStep(13).play();
					harp.pitchStep(16).play();
					snare.pitchStep(24).play();
				});
				Tasks.wait(wait += 3, () -> harp.pitchStep(9).play());
				Tasks.wait(wait += 3, () -> harp.pitchStep(11).play());
				Tasks.wait(wait += 3, () -> harp.pitchStep(12).play());
				Tasks.wait(wait += 3, () -> harp.pitchStep(13).play());
				Tasks.wait(wait += 3, () -> {
					harp.pitchStep(13).play();
					harp.pitchStep(17).play();
					harp.pitchStep(8).play();
					snare.pitchStep(24).play();
				});
				Tasks.wait(wait += 3, () -> {
					harp.pitchStep(20).play();
					harp.pitchStep(12).play();
					harp.pitchStep(15).play();
				});
			}

			@Override
			public void play(HasPlayer player) {
				play(player.getPlayer().getLocation());
			}
		},
		SABOTAGE_VOTE {
			@Override
			public void play(HasPlayer player) {
				new SoundBuilder(Sound.BLOCK_NOTE_BLOCK_CHIME).reciever(player).volume(0.8).pitch(1.7).play();
				Tasks.wait(3, () -> new SoundBuilder(Sound.BLOCK_NOTE_BLOCK_CHIME).reciever(player).volume(0.8).pitch(2.0).play());
			}

			@Override
			public void play(Location location) {
			}
		},
		SABOTAGE_MEETING {
			@Override
			public void play(HasPlayer player) {
				new SoundBuilder(Sound.BLOCK_BELL_USE).reciever(player).pitch(0.8).play();
				new SoundBuilder(Sound.BLOCK_BELL_RESONATE).reciever(player).volume(0.25).play();

				SoundBuilder bell = new SoundBuilder(Sound.BLOCK_NOTE_BLOCK_BELL).reciever(player).volume(0.6);
				Tasks.wait(7, () -> bell.pitch(0.2F).play());
				Tasks.wait(12, () -> bell.pitch(0.6F).play());
				Tasks.wait(17, () -> bell.pitch(0.8F).play());
			}

			@Override
			public void play(Location location) {
			}
		};

		private static Location getFinalLocation(HasPlayer player, Location location) {
			if (location == null)
				return player.getPlayer().getLocation();
			return location;
		}

		public abstract void play(Location location);

		public abstract void play(HasPlayer player);

		public void play(Collection<? extends HasPlayer> players) {
			players.stream().map(HasPlayer::getPlayer).forEach(this::play);
		}

		public void playAll() {
			play(PlayerUtils.getOnlinePlayers());
		}
	}

	private static float getMuteMenuVolume(HasPlayer player, MuteMenuItem item) {
		float volume = SoundUtils.defaultVolume;
		Integer customVolume = MuteMenuUser.getVolume(player.getPlayer(), item);
		if (customVolume != null)
			volume = customVolume / 50.0F;
		return volume;
	}

	public static float randomPitch() {
		return (float) RandomUtils.randomDouble(0.1, 2);
	}

	public static float getPitch(int step) {
		return (float) Math.pow(2, ((-12 + step) / 12.0));
	}

}
