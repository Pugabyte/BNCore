package me.pugabyte.nexus.features.minigames.models.arenas;

import lombok.Data;
import lombok.ToString;
import me.pugabyte.nexus.features.minigames.models.Arena;
import me.pugabyte.nexus.features.minigames.models.mechanics.custom.sabotage.Tasks;
import org.bukkit.configuration.serialization.SerializableAs;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@ToString(callSuper = true)
@SerializableAs("SabotageArena")
public class SabotageArena extends Arena {
	private int killCooldown = 25;
	private int meetingCooldown = 20;
	private Set<Tasks> tasks;
	private int shortTasks = 3;
	private int commonTasks = 2;
	private int longTasks = 2;

	public SabotageArena(Map<String, Object> map) {
		super(map);
		killCooldown = (int) map.getOrDefault("killCooldown", killCooldown);
		meetingCooldown = (int) map.getOrDefault("meetingCooldown", meetingCooldown);
		tasks = ((Set<String>) map.getOrDefault("tasks", new HashSet<String>())).stream().map(Tasks::valueOf).collect(Collectors.toSet());
		shortTasks = (int) map.getOrDefault("shortTasks", shortTasks);
		commonTasks = (int) map.getOrDefault("commonTasks", commonTasks);
		longTasks = (int) map.getOrDefault("longTasks", longTasks);
	}

	@Override
	public @NotNull Map<String, Object> serialize() {
		LinkedHashMap<String, Object> map = (LinkedHashMap<String, Object>) super.serialize();
		map.put("killCooldown", killCooldown);
		map.put("meetingCooldown", meetingCooldown);
		map.put("tasks", tasks.stream().map(Tasks::name).collect(Collectors.toSet()));
		map.put("shortTasks", shortTasks);
		map.put("commonTasks", commonTasks);
		map.put("longTasks", longTasks);
		return map;
	}

	public int maxTasksOf(Tasks.TaskType type) {
		return switch (type) {
			case COMMON -> commonTasks;
			case SHORT -> shortTasks;
			case LONG -> longTasks;
			case SABOTAGE -> 0;
		};
	}
}
