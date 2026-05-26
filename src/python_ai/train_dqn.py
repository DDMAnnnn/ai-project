import argparse
import json
from pathlib import Path

import numpy as np
import tensorflow as tf

from boss_logging import (
    count_boss_action,
    describe_action,
    format_boss_aggregate,
    format_boss_q_summary,
    is_boss_level,
    update_boss_stats,
)
from dqn_agent import DQNAgent
from mathcard_vector_env import MathCardVectorEnv
from observation_encoder import ENCODER_VERSION, VECTOR_SIZE
from replay_buffer import ReplayBuffer


ACTION_COUNT = 37
WIN_LEVEL = 50
TRAINING_STATE_FILENAME = "mathcard_dqn_state.json"
REPLAY_BUFFER_FILENAME = "mathcard_replay_buffer.npz"
REWARD_VERSION = "boss_reward_v3_deck_quality"
MODEL_VERSION = f"{ENCODER_VERSION}_{REWARD_VERSION}_dueling_dqn_v1"


def parse_args():
    parser = argparse.ArgumentParser(description="Train a DQN agent for MathCard.")
    parser.add_argument("--episodes", type=int, default=20)
    parser.add_argument("--max-steps", type=int, default=3000)
    parser.add_argument("--batch-size", type=int, default=64)
    parser.add_argument("--buffer-size", type=int, default=100_000)
    parser.add_argument("--warmup-steps", type=int, default=500)
    parser.add_argument("--train-every", type=int, default=1)
    parser.add_argument("--target-update-episodes", type=int, default=5)
    parser.add_argument("--target-update-mode", choices=["hard", "soft"], default="soft")
    parser.add_argument("--target-update-tau", type=float, default=0.005)
    parser.add_argument("--save-every", type=int, default=10)
    parser.add_argument("--eval-every", type=int, default=0)
    parser.add_argument("--eval-episodes", type=int, default=100)
    parser.add_argument("--eval-max-steps", type=int, default=0)
    parser.add_argument("--deck-data-log", type=str, default="logs/deck_predictor_data.jsonl")
    parser.add_argument("--load-model", dest="load_model", action="store_true", default=True)
    parser.add_argument("--no-load-model", dest="load_model", action="store_false")
    parser.add_argument("--load-replay-buffer", dest="load_replay_buffer", action="store_true", default=True)
    parser.add_argument("--no-load-replay-buffer", dest="load_replay_buffer", action="store_false")
    parser.add_argument("--learning-rate", type=float, default=0.00025)
    parser.add_argument("--gradient-clip-norm", type=float, default=10.0)
    parser.add_argument("--argmax-tie-epsilon", type=float, default=0.05)
    parser.add_argument("--rank-sampling-decay", type=float, default=1.25)
    parser.add_argument("--boss-rank-sampling", dest="boss_rank_sampling", action="store_true", default=True)
    parser.add_argument("--no-boss-rank-sampling", dest="boss_rank_sampling", action="store_false")
    parser.add_argument("--boss-rank-sampling-prob", type=float, default=0.20)
    parser.add_argument("--boss-rank-top-k", type=int, default=3)
    parser.add_argument("--boss-rank-gap-threshold", type=float, default=0.50)
    parser.add_argument("--prioritized-replay", dest="prioritized_replay", action="store_true", default=True)
    parser.add_argument("--no-prioritized-replay", dest="prioritized_replay", action="store_false")
    parser.add_argument("--per-alpha", type=float, default=0.30)
    parser.add_argument("--per-beta-start", type=float, default=0.60)
    parser.add_argument("--per-beta-end", type=float, default=1.00)
    parser.add_argument("--per-epsilon", type=float, default=0.001)
    parser.add_argument("--per-candidate-size", type=int, default=4096)
    parser.add_argument("--reset-per-priorities", action="store_true")
    parser.add_argument("--timeout-penalty", type=float, default=-50.0)
    parser.add_argument("--reward-spike-threshold", type=float, default=1000.0)
    parser.add_argument("--epsilon", type=float, default=None)
    parser.add_argument("--epsilon-min", type=float, default=0.10)
    parser.add_argument("--epsilon-decay", type=float, default=0.999)
    parser.add_argument("--epsilon-start", type=float, default=1.0)
    parser.add_argument("--epsilon-episode-150", type=float, default=0.30)
    parser.add_argument("--epsilon-episode-200", type=float, default=0.30)
    parser.add_argument("--epsilon-episode-250", type=float, default=0.10)
    parser.add_argument("--epsilon-episode-350", type=float, default=0.05)
    parser.add_argument("--adaptive-epsilon", dest="adaptive_epsilon", action="store_true", default=False)
    parser.add_argument("--no-adaptive-epsilon", dest="adaptive_epsilon", action="store_false")
    parser.add_argument("--progress-window", type=int, default=20)
    parser.add_argument("--epsilon-stall-patience", type=int, default=20)
    parser.add_argument("--epsilon-improvement-delta", type=float, default=0.25)
    parser.add_argument("--epsilon-boost", type=float, default=0.15)
    parser.add_argument("--epsilon-boost-max", type=float, default=0.90)
    return parser.parse_args()


def format_metric(metrics, key, default="n/a", precision=2, reducer=np.mean):
    values = [metric[key] for metric in metrics if key in metric]
    if not values:
        return default
    return f"{float(reducer(values)):.{precision}f}"


def format_loss(metrics):
    return format_metric(metrics, "loss", precision=4)


def model_path():
    project_root = Path(__file__).resolve().parents[2]
    models_dir = project_root / "models"
    models_dir.mkdir(exist_ok=True)
    return models_dir / "mathcard_dqn.keras"


def training_state_path():
    return model_path().parent / TRAINING_STATE_FILENAME


def replay_buffer_path():
    return model_path().parent / REPLAY_BUFFER_FILENAME


def project_root():
    return Path(__file__).resolve().parents[2]


def deck_data_log_path(path_text):
    if not path_text:
        return None

    path = Path(path_text).expanduser()
    if not path.is_absolute():
        path = project_root() / path
    path.parent.mkdir(parents=True, exist_ok=True)
    return path


def replay_metadata():
    return {
        "state_size": VECTOR_SIZE,
        "action_count": ACTION_COUNT,
        "encoder_version": ENCODER_VERSION,
        "reward_version": REWARD_VERSION,
    }


def load_training_state(path):
    if not path.exists():
        return {}

    with path.open("r", encoding="utf-8") as file:
        return json.load(file)


def save_training_state(
    path,
    agent,
    completed_episodes,
    total_steps,
    recent_rewards,
    recent_levels,
    best_progress_score,
    stalled_episodes,
):
    state = {
        "model_version": MODEL_VERSION,
        "epsilon": float(agent.epsilon),
        "completed_episodes": int(completed_episodes),
        "total_steps": int(total_steps),
        "recent_rewards": [float(reward) for reward in recent_rewards],
        "recent_levels": [int(level) for level in recent_levels],
        "best_progress_score": None if best_progress_score is None else float(best_progress_score),
        "stalled_episodes": int(stalled_episodes),
    }

    with path.open("w", encoding="utf-8") as file:
        json.dump(state, file, indent=2)


def progress_score(average_reward, average_level):
    clipped_reward = float(np.clip(average_reward, -50.0, 100.0))
    return float(average_level + clipped_reward / 20.0)


def adaptive_epsilon_floor(args, average_level):
    if average_level < 12:
        return max(args.epsilon_min, 0.60)
    if average_level < 20:
        return max(args.epsilon_min, 0.40)
    if average_level < 35:
        return max(args.epsilon_min, 0.25)
    if average_level < 45:
        return max(args.epsilon_min, 0.15)
    return args.epsilon_min


def scheduled_epsilon(args, global_episode):
    if args.epsilon is not None:
        return float(args.epsilon)

    schedule_points = [
        (1, args.epsilon_start),
        (150, args.epsilon_episode_150),
        (200, args.epsilon_episode_200),
        (250, args.epsilon_episode_250),
        (350, args.epsilon_episode_350),
    ]

    if global_episode <= schedule_points[0][0]:
        return float(schedule_points[0][1])

    for (start_episode, start_epsilon), (end_episode, end_epsilon) in zip(
        schedule_points,
        schedule_points[1:],
    ):
        if global_episode <= end_episode:
            progress = (
                float(global_episode - start_episode)
                / float(max(1, end_episode - start_episode))
            )
            return float(start_epsilon + progress * (end_epsilon - start_epsilon))

    return float(schedule_points[-1][1])


def replay_priority_for_level(level):
    level = max(1, int(level))
    return (level - 1) // 10


def prioritized_replay_beta(args, episode):
    if args.episodes <= 1:
        return float(args.per_beta_end)

    progress = float(episode - 1) / float(max(1, args.episodes - 1))
    progress = float(np.clip(progress, 0.0, 1.0))
    return float(args.per_beta_start + progress * (args.per_beta_end - args.per_beta_start))


def reward_option_from_observation(observation, action):
    reward_index = action - 32
    reward_options = observation.get("rewardOptions", [])
    if 0 <= reward_index < len(reward_options):
        return reward_options[reward_index]
    return None


def capture_deck_decision(observation, action, step, reward, info):
    action_result = info["actionResult"]
    return {
        "step": int(step),
        "reward_level": int(observation["level"]),
        "next_level": int(info["observation"]["level"]),
        "player_health": int(observation["playerHealth"]),
        "deck_size": int(observation["deckSize"]),
        "deck_cards": list(observation.get("deckCards", [])),
        "reward_options": list(observation.get("rewardOptions", [])),
        "action": int(action),
        "reward_index": int(action - 32),
        "chosen_option": reward_option_from_observation(observation, action),
        "action_reward": float(reward),
        "action_message": action_result["message"],
        "action_legal": bool(action_result["legal"]),
    }


def append_deck_decision_rows(path, decisions, global_episode, episode_reward, steps, final_observation):
    if path is None or not decisions:
        return

    outcome = {
        "episode": int(global_episode),
        "episode_reward": float(episode_reward),
        "episode_steps": int(steps),
        "final_level": int(final_observation["level"]),
        "final_state": final_observation["state"],
        "victory": final_observation["state"] == "VICTORY",
        "model_version": MODEL_VERSION,
        "reward_version": REWARD_VERSION,
        "encoder_version": ENCODER_VERSION,
    }

    with path.open("a", encoding="utf-8") as file:
        for decision in decisions:
            row = dict(decision)
            row.update(outcome)
            file.write(json.dumps(row, ensure_ascii=False) + "\n")


def update_epsilon(
    agent,
    args,
    recent_rewards,
    recent_levels,
    replay_ready,
    best_progress_score,
    stalled_episodes,
):
    if not args.adaptive_epsilon:
        return best_progress_score, stalled_episodes

    if not recent_levels:
        return best_progress_score, stalled_episodes

    average_reward = float(np.mean(recent_rewards[-args.progress_window:]))
    average_level = float(np.mean(recent_levels[-args.progress_window:]))
    current_progress_score = progress_score(average_reward, average_level)

    if (
        best_progress_score is None
        or current_progress_score > best_progress_score + args.epsilon_improvement_delta
    ):
        best_progress_score = current_progress_score
        stalled_episodes = 0
    else:
        stalled_episodes += 1

    if not replay_ready:
        agent.epsilon = max(agent.epsilon, args.epsilon_boost_max)
        return best_progress_score, stalled_episodes

    agent.decay_epsilon()
    agent.epsilon = max(agent.epsilon, adaptive_epsilon_floor(args, average_level))

    if stalled_episodes >= args.epsilon_stall_patience:
        agent.epsilon = min(args.epsilon_boost_max, agent.epsilon + args.epsilon_boost)
        stalled_episodes = 0

    return best_progress_score, stalled_episodes


def summarize(values):
    return {
        "avg": float(np.mean(values)),
        "min": float(np.min(values)),
        "max": float(np.max(values)),
    }


def format_observation_brief(observation):
    if observation is None:
        return "none"

    return (
        f"L{int(observation['level']):02d} "
        f"{observation['state']} "
        f"round={observation['round']} "
        f"hp={observation['playerHealth']} "
        f"shield={observation['shield']} "
        f"enemy={observation['enemyType']} "
        f"{observation['enemyHealth']}/{observation['enemyMaxHealth']} "
        f"dmg={observation['enemyDamage']} "
        f"calc={observation['calcValue']}{observation['operation']} "
        f"deck={observation['deckSize']} "
        f"draw={observation['drawingPileSize']} "
        f"discard={observation['discardPileSize']}"
    )


def maybe_log_reward_spike(
    threshold,
    phase,
    episode,
    step,
    observation,
    action,
    reward,
    total_reward_before,
    info,
):
    if threshold <= 0 or abs(float(reward)) < threshold:
        return

    action_result = info.get("actionResult", {})
    next_observation = info.get("observation")
    print(
        "RewardSpike "
        f"{phase} ep={episode:04d} step={step:04d} "
        f"reward={float(reward):.2f} totalBefore={float(total_reward_before):.2f} "
        f"action={action}:{describe_action(action)} "
        f"legal={action_result.get('legal')} done={info.get('done')} "
        f"message={action_result.get('message')}",
        flush=True,
    )
    print(f"  before: {format_observation_brief(observation)}", flush=True)
    print(f"  hand: {observation.get('handCards', []) if observation else []}", flush=True)
    if observation and observation.get("rewardOptions"):
        print(f"  rewards: {observation.get('rewardOptions', [])}", flush=True)
    print(f"  after: {format_observation_brief(next_observation)}", flush=True)


def evaluate_agent(agent, episodes, max_steps, label, timeout_penalty, reward_spike_threshold):
    env = MathCardVectorEnv()
    episode_rewards = []
    episode_levels = []
    episode_steps = []
    action_counts = np.zeros(ACTION_COUNT, dtype=np.int32)
    victories = 0
    timeouts = 0
    boss_stats_window = []

    print(f"Evaluation after episode {label:04d}:")
    try:
        for episode in range(1, episodes + 1):
            state, action_mask, reset_info = env.reset(return_info=True)
            total_reward = 0.0
            final_info = None
            current_observation = reset_info["observation"]
            boss_stats = {}
            done = False

            for step in range(1, max_steps + 1):
                action, q_value, q_margin = agent.greedy_action_info(state, action_mask)
                count_boss_action(
                    boss_stats,
                    current_observation,
                    actual_action=action,
                    q_action=action,
                    q_value=q_value,
                    q_margin=q_margin,
                )
                action_counts[action] += 1
                previous_observation = current_observation
                state, reward, done, action_mask, info = env.step(action)
                maybe_log_reward_spike(
                    reward_spike_threshold,
                    "eval",
                    episode,
                    step,
                    previous_observation,
                    action,
                    reward,
                    total_reward,
                    info,
                )
                update_boss_stats(boss_stats, info["observation"])
                total_reward += reward
                final_info = info
                current_observation = info["observation"]

                if done:
                    break

            if not done:
                timeouts += 1
                total_reward += timeout_penalty

            observation = final_info["observation"]
            episode_rewards.append(total_reward)
            episode_levels.append(observation["level"])
            episode_steps.append(step)
            if observation["state"] == "VICTORY":
                victories += 1

            print(
                "Eval "
                f"{episode:04d} | "
                f"steps={step:04d} | "
                f"level={observation['level']:02d} | "
                f"reward={total_reward:8.2f} | "
                f"{format_boss_q_summary(boss_stats)}",
                flush=True,
            )
            boss_stats_window.append(boss_stats)
            boss_stats_window = boss_stats_window[-10:]
            if episode % 10 == 0:
                print(
                    format_boss_aggregate(
                        episode - len(boss_stats_window) + 1,
                        episode,
                        boss_stats_window,
                    ),
                    flush=True,
                )
    finally:
        env.close()

    reward_stats = summarize(episode_rewards)
    level_stats = summarize(episode_levels)
    step_stats = summarize(episode_steps)

    print("Evaluation summary:")
    print(
        f"Rewards avg/min/max: "
        f"{reward_stats['avg']:.2f} / {reward_stats['min']:.2f} / {reward_stats['max']:.2f}"
    )
    print(
        f"Levels avg/min/max: "
        f"{level_stats['avg']:.2f} / {level_stats['min']:.0f} / {level_stats['max']:.0f}"
    )
    print(
        f"Steps avg/min/max: "
        f"{step_stats['avg']:.2f} / {step_stats['min']:.0f} / {step_stats['max']:.0f}"
    )
    print(f"Victories: {victories}/{episodes}")
    print(f"Timeouts: {timeouts}/{episodes}")
    top_actions = np.argsort(action_counts)[-10:][::-1]
    print("Top actions:")
    for action in top_actions:
        if action_counts[action] > 0:
            print(f"  action {action} ({describe_action(action)}): {int(action_counts[action])}")


def main():
    args = parse_args()

    print(f"TensorFlow version: {tf.__version__}")
    print(f"GPU devices: {tf.config.list_physical_devices('GPU')}")
    print(f"State size: {VECTOR_SIZE}")
    print(f"Action count: {ACTION_COUNT}")
    print(f"Model version: {MODEL_VERSION}")
    print(
        "Boss rank sampling: "
        f"{args.boss_rank_sampling} "
        f"prob={args.boss_rank_sampling_prob:.2f} "
        f"topK={args.boss_rank_top_k} "
        f"gap<={args.boss_rank_gap_threshold:.2f}"
    )
    print(
        "Prioritized replay: "
        f"{args.prioritized_replay} "
        f"alpha={args.per_alpha:.2f} "
        f"beta={args.per_beta_start:.2f}->{args.per_beta_end:.2f} "
        f"eps={args.per_epsilon:g} "
        f"candidates={args.per_candidate_size}"
    )

    env = MathCardVectorEnv()
    replay_buffer = ReplayBuffer(capacity=args.buffer_size)
    agent = DQNAgent(
        state_size=VECTOR_SIZE,
        action_count=ACTION_COUNT,
        learning_rate=args.learning_rate,
        epsilon_min=args.epsilon_min,
        epsilon_decay=args.epsilon_decay,
        gradient_clip_norm=args.gradient_clip_norm,
        argmax_tie_epsilon=args.argmax_tie_epsilon,
        rank_sampling_decay=args.rank_sampling_decay,
    )

    total_steps = 0
    save_path = model_path()
    state_path = training_state_path()
    buffer_path = replay_buffer_path()
    deck_log_path = deck_data_log_path(args.deck_data_log)
    completed_episodes = 0
    recent_rewards = []
    recent_levels = []
    best_progress_score = None
    stalled_episodes = 0

    if deck_log_path is not None:
        print(f"Deck predictor data log: {deck_log_path}")

    if args.load_model:
        if save_path.exists():
            training_state = load_training_state(state_path)
            if training_state.get("model_version") != MODEL_VERSION:
                print(
                    "Skipped model load because saved training state is missing or "
                    "uses an older model version. Start a new model for this encoder."
                )
            else:
                agent.model = tf.keras.models.load_model(save_path, compile=False)
                agent.compile_model(agent.model)
                agent.update_target_model()
                print(f"Loaded model from: {save_path}")

                agent.epsilon = training_state.get("epsilon", agent.epsilon)
                completed_episodes = training_state.get("completed_episodes", completed_episodes)
                total_steps = training_state.get("total_steps", total_steps)
                recent_rewards = training_state.get("recent_rewards", recent_rewards)
                recent_levels = training_state.get("recent_levels", recent_levels)
                best_progress_score = training_state.get("best_progress_score", best_progress_score)
                stalled_episodes = training_state.get("stalled_episodes", stalled_episodes)
                print(f"Loaded training state from: {state_path}")

                if args.load_replay_buffer and buffer_path.exists():
                    try:
                        replay_buffer, buffer_metadata = ReplayBuffer.load(
                            buffer_path,
                            capacity=args.buffer_size,
                            expected_metadata=replay_metadata(),
                        )
                        print(
                            f"Loaded replay buffer from: {buffer_path} "
                            f"({len(replay_buffer)} transitions)"
                        )
                        if args.reset_per_priorities:
                            replay_buffer.reset_sampling_priorities()
                            print("Reset replay buffer PER sampling priorities to 1.0")
                    except ValueError as error:
                        print(f"Skipped replay buffer load: {error}")
        else:
            print(f"No saved model found at: {save_path}. Starting from a new model.")

    if args.epsilon is not None:
        agent.epsilon = args.epsilon

    try:
        boss_stats_window = []
        for episode in range(1, args.episodes + 1):
            global_episode = completed_episodes + episode
            if not args.adaptive_epsilon:
                agent.epsilon = scheduled_epsilon(args, global_episode)

            state, action_mask, reset_info = env.reset(return_info=True)
            episode_reward = 0.0
            training_metrics = []
            final_info = None
            current_observation = reset_info["observation"]
            boss_stats = {}
            episode_deck_decisions = []

            for step in range(1, args.max_steps + 1):
                q_action = None
                q_value = None
                q_margin = None
                is_training_boss = (
                    current_observation is not None
                    and current_observation["state"] == "BATTLE"
                    and is_boss_level(current_observation["level"])
                )

                action_info = agent.choose_action_info(
                    state,
                    action_mask,
                    use_rank_sampling=args.boss_rank_sampling and is_training_boss,
                    rank_sampling_probability=args.boss_rank_sampling_prob,
                    rank_top_k=args.boss_rank_top_k,
                    rank_gap_threshold=args.boss_rank_gap_threshold,
                    require_q_values=is_training_boss,
                )
                action = action_info["action"]
                if is_training_boss:
                    q_action = action_info["greedy_action"]
                    q_value = action_info["greedy_q_value"]
                    q_margin = action_info["q_margin"]

                count_boss_action(
                    boss_stats,
                    current_observation,
                    actual_action=action,
                    q_action=q_action,
                    q_value=q_value,
                    q_margin=q_margin,
                )
                previous_observation = current_observation
                next_state, reward, done, next_action_mask, info = env.step(action)
                timed_out = step == args.max_steps and not done
                transition_done = done or timed_out
                transition_reward = reward + args.timeout_penalty if timed_out else reward
                maybe_log_reward_spike(
                    args.reward_spike_threshold,
                    "train",
                    global_episode,
                    step,
                    previous_observation,
                    action,
                    transition_reward,
                    episode_reward,
                    info,
                )
                update_boss_stats(boss_stats, info["observation"])
                if (
                    previous_observation is not None
                    and previous_observation["state"] == "REWARD"
                    and 32 <= action < ACTION_COUNT
                ):
                    episode_deck_decisions.append(
                        capture_deck_decision(previous_observation, action, step, reward, info)
                    )

                replay_buffer.add(
                    state=state,
                    action=action,
                    reward=transition_reward,
                    next_state=next_state,
                    done=transition_done,
                    next_action_mask=next_action_mask,
                    priority=replay_priority_for_level(info["observation"]["level"]),
                )

                state = next_state
                action_mask = next_action_mask
                episode_reward += transition_reward
                total_steps += 1
                final_info = info
                current_observation = info["observation"]

                if (
                    len(replay_buffer) >= args.warmup_steps
                    and len(replay_buffer) >= args.batch_size
                    and total_steps % max(1, args.train_every) == 0
                ):
                    if args.prioritized_replay:
                        per_beta = prioritized_replay_beta(args, episode)
                        batch, batch_indices, sample_weights = replay_buffer.sample_prioritized(
                            args.batch_size,
                            alpha=args.per_alpha,
                            beta=per_beta,
                            candidate_size=args.per_candidate_size,
                        )
                        metrics = agent.train_on_batch(batch, sample_weights=sample_weights)
                        replay_buffer.update_sampling_priorities(
                            batch_indices,
                            np.abs(metrics["td_errors"]) + args.per_epsilon,
                        )
                        metrics["per_beta"] = per_beta
                        metrics["is_weight_mean"] = float(np.mean(sample_weights))
                    else:
                        batch = replay_buffer.sample(args.batch_size)
                        metrics = agent.train_on_batch(batch)
                    training_metrics.append(metrics)
                    if args.target_update_mode == "soft":
                        agent.soft_update_target_model(args.target_update_tau)

                if transition_done:
                    break

            observation = final_info["observation"]
            append_deck_decision_rows(
                deck_log_path,
                episode_deck_decisions,
                global_episode,
                episode_reward,
                step,
                observation,
            )
            recent_rewards.append(episode_reward)
            recent_levels.append(observation["level"])
            recent_rewards = recent_rewards[-args.progress_window:]
            recent_levels = recent_levels[-args.progress_window:]

            replay_ready = len(replay_buffer) >= args.warmup_steps and len(replay_buffer) >= args.batch_size
            best_progress_score, stalled_episodes = update_epsilon(
                agent,
                args,
                recent_rewards,
                recent_levels,
                replay_ready,
                best_progress_score,
                stalled_episodes,
            )

            if (
                args.target_update_mode == "hard"
                and episode % args.target_update_episodes == 0
            ):
                agent.update_target_model()

            if episode % args.save_every == 0:
                agent.save(save_path)
                replay_buffer.save(buffer_path, metadata=replay_metadata())
                save_training_state(
                    state_path,
                    agent,
                    global_episode,
                    total_steps,
                    recent_rewards,
                    recent_levels,
                    best_progress_score,
                    stalled_episodes,
                )
                print(f"Saved replay buffer to: {buffer_path}")

            print(
                "Episode "
                f"{global_episode:04d} | "
                f"steps={step:04d} | "
                f"level={observation['level']:02d} | "
                f"reward={episode_reward:8.2f} | "
                f"eps={agent.epsilon:.3f} | "
                f"loss={format_loss(training_metrics)} | "
                f"tdAbs={format_metric(training_metrics, 'td_abs_mean')} | "
                f"qMin={format_metric(training_metrics, 'q_min', reducer=np.min)} | "
                f"qMax={format_metric(training_metrics, 'q_max', reducer=np.max)} | "
                f"legal={format_metric(training_metrics, 'legal_q_max', reducer=np.max)} | "
                f"illegal={format_metric(training_metrics, 'illegal_q_max', reducer=np.max)} | "
                f"{format_boss_q_summary(boss_stats)}"
            )
            boss_stats_window.append(boss_stats)
            boss_stats_window = boss_stats_window[-10:]
            if episode % 10 == 0:
                print(
                    format_boss_aggregate(
                        global_episode - len(boss_stats_window) + 1,
                        global_episode,
                        boss_stats_window,
                    )
                    + f" | buffer={len(replay_buffer)}"
                )

            if args.eval_every > 0 and episode % args.eval_every == 0:
                evaluate_agent(
                    agent,
                    episodes=args.eval_episodes,
                    max_steps=args.eval_max_steps or args.max_steps,
                    label=global_episode,
                    timeout_penalty=args.timeout_penalty,
                    reward_spike_threshold=args.reward_spike_threshold,
                )

        agent.save(save_path)
        replay_buffer.save(buffer_path, metadata=replay_metadata())
        save_training_state(
            state_path,
            agent,
            completed_episodes + args.episodes,
            total_steps,
            recent_rewards,
            recent_levels,
            best_progress_score,
            stalled_episodes,
        )
        print(f"Saved model to: {save_path}")
        print(f"Saved replay buffer to: {buffer_path}")
        print(f"Saved training state to: {state_path}")
    finally:
        env.close()


if __name__ == "__main__":
    main()
