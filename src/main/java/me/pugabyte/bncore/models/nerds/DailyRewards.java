package me.pugabyte.bncore.models.nerds;

import java.util.ArrayList;
import java.util.List;

public class DailyRewards {
	private int streak = 0;
	private List<Integer> claimedRewards = new ArrayList<>();

	public static DailyRewards read(Nerd nerd) {
		return new DailyRewards();
	}
}
