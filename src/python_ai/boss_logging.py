AGGREGATE_BOSS_LEVEL = 20


def is_boss_level(level):
    return int(level) > 0 and int(level) % 10 == 0


def ensure_boss_stats(boss_stats, observation):
    level = int(observation["level"])
    if not is_boss_level(level):
        return None

    if level not in boss_stats:
        boss_stats[level] = {
            "level": level,
            "actions": 0,
            "entry_player_health": int(observation["playerHealth"]),
            "entry_enemy_health": int(observation["enemyHealth"]),
            "end_player_health": int(observation["playerHealth"]),
            "end_enemy_health": int(observation["enemyHealth"]),
            "result": "active",
            "actual_action_counts": {},
            "q_action_counts": {},
            "q_value_sum": 0.0,
            "q_margin_sum": 0.0,
            "q_samples": 0,
        }

    return boss_stats[level]


def count_boss_action(
    boss_stats,
    observation,
    actual_action=None,
    q_action=None,
    q_value=None,
    q_margin=None,
):
    if observation is None or observation["state"] != "BATTLE":
        return

    stats = ensure_boss_stats(boss_stats, observation)
    if stats is not None:
        stats["actions"] += 1
        increment_action_count(stats["actual_action_counts"], actual_action)
        increment_action_count(stats["q_action_counts"], q_action)
        if q_value is not None:
            stats["q_value_sum"] += float(q_value)
        if q_margin is not None:
            stats["q_margin_sum"] += float(q_margin)
        if q_action is not None:
            stats["q_samples"] += 1


def increment_action_count(action_counts, action):
    if action is None:
        return

    action = int(action)
    action_counts[action] = action_counts.get(action, 0) + 1


def update_boss_stats(boss_stats, observation):
    if not is_boss_level(observation["level"]):
        return

    stats = ensure_boss_stats(boss_stats, observation)
    stats["end_player_health"] = int(observation["playerHealth"])
    stats["end_enemy_health"] = int(observation["enemyHealth"])

    if observation["state"] == "VICTORY":
        stats["result"] = "victory"
    elif observation["state"] == "REWARD":
        stats["result"] = "cleared"
    elif observation["state"] == "BATTLE" and int(observation["playerHealth"]) <= 0:
        stats["result"] = "dead"


def format_boss_log(boss_stats):
    if not boss_stats:
        return "none"

    parts = []
    for level in sorted(boss_stats):
        stats = boss_stats[level]
        q_samples = stats["q_samples"]
        q_value = stats["q_value_sum"] / q_samples if q_samples else 0.0
        q_margin = stats["q_margin_sum"] / q_samples if q_samples else 0.0
        parts.append(
            f"L{level:02d}:{stats['result']} "
            f"actions={stats['actions']} "
            f"actual={format_action_counts(stats['actual_action_counts'])} "
            f"q={format_action_counts(stats['q_action_counts'])} "
            f"qAvg={q_value:.2f} "
            f"qGap={q_margin:.2f} "
            f"hp={stats['entry_player_health']}->{stats['end_player_health']} "
            f"enemy={stats['entry_enemy_health']}->{stats['end_enemy_health']}"
        )
    return "; ".join(parts)


def format_boss_q_summary(boss_stats):
    stats = latest_sampled_boss_stats(boss_stats)
    if stats is None:
        return "qAvg=n/a qGap=n/a"

    q_samples = stats["q_samples"]
    q_value = stats["q_value_sum"] / q_samples
    q_margin = stats["q_margin_sum"] / q_samples
    return f"qAvg={q_value:.2f} qGap={q_margin:.2f}"


def latest_sampled_boss_stats(boss_stats):
    sampled_levels = [
        level for level, stats in boss_stats.items()
        if stats["q_samples"] > 0
    ]
    if not sampled_levels:
        return None

    return boss_stats[max(sampled_levels)]


def format_boss_aggregate(start_episode, end_episode, boss_stats_window):
    aggregate = aggregate_boss_stats(boss_stats_window)
    return (
        f"BossAgg {start_episode:04d}-{end_episode:04d} | "
        f"clear={aggregate['clear']} | "
        f"dmgAvg={aggregate['damage_avg']:.1f} | "
        f"actualTop={format_action_counts(aggregate['actual_action_counts'], limit=10)} | "
        f"qTop={format_action_counts(aggregate['q_action_counts'], limit=10)}"
    )


def aggregate_boss_stats(boss_stats_window):
    aggregate = {
        "seen": 0,
        "clear": 0,
        "damage_sum": 0.0,
        "actual_action_counts": {},
        "q_action_counts": {},
    }

    for boss_stats in boss_stats_window:
        for stats in boss_stats.values():
            if int(stats["level"]) != AGGREGATE_BOSS_LEVEL:
                continue

            aggregate["seen"] += 1
            if stats["result"] in ["cleared", "victory"]:
                aggregate["clear"] += 1

            entry_enemy_health = max(0, int(stats["entry_enemy_health"]))
            end_enemy_health = max(0, int(stats["end_enemy_health"]))
            damage = min(entry_enemy_health, max(0, entry_enemy_health - end_enemy_health))
            aggregate["damage_sum"] += damage
            merge_action_counts(aggregate["actual_action_counts"], stats["actual_action_counts"])
            merge_action_counts(aggregate["q_action_counts"], stats["q_action_counts"])

    if aggregate["seen"] > 0:
        aggregate["damage_avg"] = aggregate["damage_sum"] / aggregate["seen"]
    else:
        aggregate["damage_avg"] = 0.0

    return aggregate


def merge_action_counts(target_counts, source_counts):
    for action, count in source_counts.items():
        target_counts[action] = target_counts.get(action, 0) + count


def format_action_counts(action_counts, limit=3):
    if not action_counts:
        return "none"

    ranked_actions = sorted(action_counts.items(), key=lambda item: item[1], reverse=True)
    return ",".join(
        f"{describe_action(action)}x{count}"
        for action, count in ranked_actions[:limit]
    )


def describe_action(action):
    action = int(action)
    if 0 <= action <= 9:
        return f"{action}:play{action}"
    if 10 <= action <= 19:
        return f"{action}:atk{action - 10}"
    if 20 <= action <= 29:
        return f"{action}:def{action - 20}"
    if action == 30:
        return "30:end"
    if action == 31:
        return "31:unused"
    if 32 <= action <= 36:
        return f"{action}:reward{action - 32}"
    return f"{action}:unknown"
