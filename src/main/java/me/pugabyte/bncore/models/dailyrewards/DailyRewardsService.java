package me.pugabyte.bncore.models.dailyrewards;

import me.pugabyte.bncore.models.BaseService;

import java.util.List;

public class DailyRewardsService extends BaseService {
	@Override
	public DailyRewards get(String uuid) {
		return database.where("uuid = ?", uuid).first(DailyRewards.class);
	}

	public List<DailyRewards> getPage(int page) {
		return database.orderBy("streak desc").limit(10).offset((page - 1) * 10).results(DailyRewards.class);
	}

	public List<DailyRewards> getAll() {
		return database.results(DailyRewards.class);
	}
}
