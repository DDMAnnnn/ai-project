import argparse
import time
from pathlib import Path

import numpy as np
import tensorflow as tf

from boss_logging import (
    count_boss_action,
    format_boss_aggregate,
    format_boss_q_summary,
    update_boss_stats,
)
from dqn_agent import DuelingQValues  # noqa: F401 - registers the saved custom layer.
from mathcard_vector_env import MathCardVectorEnv
from observation_encoder import VECTOR_SIZE


ACTION_COUNT = 37


def parse_args():
    parser = argparse.ArgumentParser(description="Evaluate a trained MathCard DQN model.")
    parser.add_argument("--episodes", type=int, default=10)
    parser.add_argument("--max-steps", type=int, default=3000)
    parser.add_argument("--timeout-penalty", type=float, default=-50.0)
    parser.add_argument("--model", type=str, default=None)
    parser.add_argument("--argmax-tie-epsilon", type=float, default=0.05)
    parser.add_argument("--watch", action="store_true")
    parser.add_argument("--watch-delay", type=float, default=0.0)
    parser.add_argument("--watch-file", type=str, default=None)
    parser.add_argument("--watch-level-min", type=int, default=0)
    parser.add_argument("--watch-pause", action="store_true")
    parser.add_argument("--watch-top-actions", type=int, default=5)
    parser.add_argument("--reward-spike-threshold", type=float, default=1000.0)
    return parser.parse_args()


def default_model_path():
    project_root = Path(__file__).resolve().parents[2]
    return project_root / "models" / "mathcard_dqn.keras"


def greedy_action_info(model, state, action_mask, argmax_tie_epsilon):
    q_values = q_values_for_state(model, state)
    action, q_value, q_margin = select_greedy_action(
        q_values,
        action_mask,
        argmax_tie_epsilon,
    )
    return action, q_value, q_margin


def select_greedy_action(q_values, action_mask, argmax_tie_epsilon):
    legal_actions = np.flatnonzero(np.array(action_mask, dtype=np.bool_))
    if legal_actions.size == 0:
        raise RuntimeError("No legal actions available.")

    legal_q_values = q_values[legal_actions]
    best_q_value = float(np.max(legal_q_values))
    if legal_q_values.size > 1:
        second_q_value = float(np.partition(legal_q_values, -2)[-2])
    else:
        second_q_value = best_q_value

    candidate_actions = legal_actions[legal_q_values >= best_q_value - argmax_tie_epsilon]
    action = int(np.random.choice(candidate_actions))
    return action, float(q_values[action]), best_q_value - second_q_value


def q_values_for_state(model, state):
    return model(np.array([state], dtype=np.float32), training=False).numpy()[0]


def describe_action(action):
    if 0 <= action <= 9:
        return f"play card {action}"
    if 10 <= action <= 19:
        return f"attack with card {action - 10}"
    if 20 <= action <= 29:
        return f"defend with card {action - 20}"
    if action == 30:
        return "end round"
    if action == 31:
        return "unused/illegal"
    if 32 <= action <= 36:
        return f"choose reward {action - 32}"
    return "unknown"


def format_cards(cards):
    if not cards:
        return "none"
    return " | ".join(f"{index}:{card}" for index, card in enumerate(cards))


def format_legal_actions(action_mask):
    legal_actions = [
        f"{action}:{describe_action(action)}"
        for action, is_legal in enumerate(action_mask)
        if is_legal
    ]
    return ", ".join(legal_actions) if legal_actions else "none"


def format_top_actions(q_values, action_mask, limit):
    legal_actions = [
        action for action, is_legal in enumerate(action_mask)
        if is_legal
    ]
    ranked_actions = sorted(
        legal_actions,
        key=lambda action: float(q_values[action]),
        reverse=True,
    )
    return ", ".join(
        f"{action}:{describe_action(action)}={float(q_values[action]):.2f}"
        for action in ranked_actions[:limit]
    ) if ranked_actions else "none"


def format_observation_brief(observation):
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


def should_watch(args, observation):
    return args.watch and int(observation["level"]) >= args.watch_level_min


def open_watch_file(path):
    if path is None:
        return None

    watch_path = Path(path).expanduser()
    if not watch_path.is_absolute():
        watch_path = Path.cwd() / watch_path
    watch_path.parent.mkdir(parents=True, exist_ok=True)
    print(f"Writing watch log to: {watch_path}")
    return watch_path.open("w", encoding="utf-8")


def emit_watch_lines(lines, watch_file):
    for line in lines:
        print(line)
        if watch_file is not None:
            watch_file.write(line + "\n")
    if watch_file is not None:
        watch_file.flush()


def print_watch_step(
    args,
    watch_file,
    episode,
    step,
    observation,
    action_mask,
    action,
    q_value,
    q_margin,
    q_values,
    reward,
    info,
):
    if not should_watch(args, observation):
        return

    action_result = info["actionResult"]
    next_observation = info["observation"]
    lines = [
        "",
        f"[Watch Eval {episode:04d} Step {step:04d}]",
        f"before: {format_observation_brief(observation)}",
        f"hand: {format_cards(observation['handCards'])}",
    ]
    if observation["rewardOptions"]:
        lines.append(f"rewards: {format_cards(observation['rewardOptions'])}")
    lines.extend([
        f"legal: {format_legal_actions(action_mask)}",
        f"topQ: {format_top_actions(q_values, action_mask, args.watch_top_actions)}",
        f"AI: {action}:{describe_action(action)} "
        f"q={q_value:.2f} gap={q_margin:.2f}",
        f"result: reward={reward:.2f} "
        f"legal={action_result['legal']} "
        f"done={info['done']} "
        f"message={action_result['message']}",
        f"after: {format_observation_brief(next_observation)}",
    ])
    emit_watch_lines(lines, watch_file)
    if args.watch_pause:
        input("Press Enter for next watched step...")
    if args.watch_delay > 0:
        time.sleep(args.watch_delay)


def maybe_log_reward_spike(
    args,
    watch_file,
    episode,
    step,
    observation,
    action,
    reward,
    total_reward_before,
    info,
):
    if args.reward_spike_threshold <= 0 or abs(float(reward)) < args.reward_spike_threshold:
        return

    action_result = info.get("actionResult", {})
    lines = [
        "",
        f"[RewardSpike Eval {episode:04d} Step {step:04d}]",
        f"reward={float(reward):.2f} totalBefore={float(total_reward_before):.2f} "
        f"action={action}:{describe_action(action)} "
        f"legal={action_result.get('legal')} done={info.get('done')} "
        f"message={action_result.get('message')}",
        f"before: {format_observation_brief(observation)}",
        f"hand: {format_cards(observation['handCards'])}",
    ]
    if observation["rewardOptions"]:
        lines.append(f"rewards: {format_cards(observation['rewardOptions'])}")
    lines.append(f"after: {format_observation_brief(info['observation'])}")
    emit_watch_lines(lines, watch_file)


def summarize(values):
    return {
        "avg": float(np.mean(values)),
        "min": float(np.min(values)),
        "max": float(np.max(values)),
    }


def main():
    args = parse_args()
    model_path = Path(args.model) if args.model else default_model_path()

    if not model_path.exists():
        raise FileNotFoundError(f"Model file not found: {model_path}")

    print(f"TensorFlow version: {tf.__version__}")
    print(f"GPU devices: {tf.config.list_physical_devices('GPU')}")
    print(f"Loading model: {model_path}")
    print(f"State size: {VECTOR_SIZE}")
    print(f"Action count: {ACTION_COUNT}")

    model = tf.keras.models.load_model(model_path, compile=False)
    env = MathCardVectorEnv()
    watch_file = open_watch_file(args.watch_file)

    episode_rewards = []
    episode_levels = []
    episode_steps = []
    action_counts = np.zeros(ACTION_COUNT, dtype=np.int32)
    victories = 0
    timeouts = 0
    boss_stats_window = []

    try:
        for episode in range(1, args.episodes + 1):
            state, action_mask, reset_info = env.reset(return_info=True)
            total_reward = 0.0
            final_info = None
            current_observation = reset_info["observation"]
            boss_stats = {}
            done = False

            for step in range(1, args.max_steps + 1):
                q_values = q_values_for_state(model, state)
                action, q_value, q_margin = select_greedy_action(
                    q_values,
                    action_mask,
                    args.argmax_tie_epsilon,
                )
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
                previous_action_mask = action_mask
                state, reward, done, action_mask, info = env.step(action)
                maybe_log_reward_spike(
                    args,
                    watch_file,
                    episode,
                    step,
                    previous_observation,
                    action,
                    reward,
                    total_reward,
                    info,
                )
                print_watch_step(
                    args,
                    watch_file,
                    episode,
                    step,
                    previous_observation,
                    previous_action_mask,
                    action,
                    q_value,
                    q_margin,
                    q_values,
                    reward,
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
                total_reward += args.timeout_penalty

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
        if watch_file is not None:
            watch_file.close()
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
    print(f"Victories: {victories}/{args.episodes}")
    print(f"Timeouts: {timeouts}/{args.episodes}")
    top_actions = np.argsort(action_counts)[-10:][::-1]
    print("Top actions:")
    for action in top_actions:
        if action_counts[action] > 0:
            print(f"  action {action} ({describe_action(action)}): {int(action_counts[action])}")


if __name__ == "__main__":
    main()
