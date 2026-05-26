import argparse
import json
from datetime import datetime, timezone
from pathlib import Path

import numpy as np

from mathcard_vector_env import MathCardVectorEnv
from observation_encoder import ENCODER_VERSION, VECTOR_SIZE
from replay_buffer import ReplayBuffer


ACTION_COUNT = 37
REWARD_VERSION = "boss_reward_v3_deck_quality"


def parse_args():
    parser = argparse.ArgumentParser(description="Record human MathCard play as replay transitions.")
    parser.add_argument("--episodes", type=int, default=1)
    parser.add_argument("--max-steps", type=int, default=3000)
    parser.add_argument("--capacity", type=int, default=100_000)
    parser.add_argument("--output", type=str, default="models/human_demo_replay_buffer.npz")
    parser.add_argument("--jsonl", type=str, default="logs/human_demo_steps.jsonl")
    parser.add_argument("--overwrite", action="store_true")
    parser.add_argument("--timeout-penalty", type=float, default=-50.0)
    parser.add_argument("--demo-sampling-priority", type=float, default=10.0)
    return parser.parse_args()


def project_root():
    return Path(__file__).resolve().parents[2]


def project_path(path_text):
    path = Path(path_text).expanduser()
    if path.is_absolute():
        return path
    return project_root() / path


def replay_priority_for_level(level):
    level = max(1, int(level))
    return (level - 1) // 10


def replay_metadata():
    return {
        "source": "human_demo",
        "encoder_version": ENCODER_VERSION,
        "reward_version": REWARD_VERSION,
        "vector_size": VECTOR_SIZE,
        "action_count": ACTION_COUNT,
        "has_action_masks": True,
    }


def describe_action(action, observation=None):
    action = int(action)
    hand_cards = observation.get("handCards", []) if observation else []
    reward_options = observation.get("rewardOptions", []) if observation else []
    if 0 <= action <= 9:
        card = hand_cards[action] if action < len(hand_cards) else "?"
        return f"play{action} {card}"
    if 10 <= action <= 19:
        index = action - 10
        card = hand_cards[index] if index < len(hand_cards) else "?"
        return f"atk{index} {card}"
    if 20 <= action <= 29:
        index = action - 20
        card = hand_cards[index] if index < len(hand_cards) else "?"
        return f"def{index} {card}"
    if action == 30:
        return "end"
    if action == 31:
        return "unused"
    if 32 <= action <= 36:
        index = action - 32
        option = reward_options[index] if index < len(reward_options) else "?"
        return f"reward{index} {option}"
    return "unknown"


def display_operation(operation):
    if not operation or operation == "\u0000":
        return "."
    return operation


def format_observation(observation):
    return (
        f"L{int(observation['level']):02d} "
        f"{observation['state']} "
        f"round={observation['round']} "
        f"hp={observation['playerHealth']} "
        f"shield={observation['shield']} "
        f"enemy={observation['enemyType']} "
        f"{observation['enemyHealth']}/{observation['enemyMaxHealth']} "
        f"dmg={observation['enemyDamage']} "
        f"calc={observation['calcValue']}{display_operation(observation['operation'])} "
        f"deck={observation['deckSize']} "
        f"draw={observation['drawingPileSize']} "
        f"discard={observation['discardPileSize']}"
    )


def print_state(observation, action_mask, total_reward):
    print("")
    print(format_observation(observation))
    print(f"total_reward={total_reward:.2f}")
    print("hand:")
    for index, card in enumerate(observation.get("handCards", [])):
        print(f"  {index}: {card}")
    if observation.get("rewardOptions"):
        print("rewards:")
        for index, option in enumerate(observation["rewardOptions"]):
            print(f"  {index}: {option}")
    print("legal actions:")
    for action in legal_actions(action_mask):
        shortcut = shortcut_for_action(action)
        print(f"  {shortcut:<5} action={action:<2} {describe_action(action, observation)}")


def legal_actions(action_mask):
    return [index for index, allowed in enumerate(action_mask) if allowed]


def shortcut_for_action(action):
    action = int(action)
    if 0 <= action <= 9:
        return f"p{action}"
    if 10 <= action <= 19:
        return f"a{action - 10}"
    if 20 <= action <= 29:
        return f"d{action - 20}"
    if action == 30:
        return "end"
    if 32 <= action <= 36:
        return f"r{action - 32}"
    return str(action)


def parse_action(text):
    text = text.strip().lower()
    if text in ["q", "quit", "exit"]:
        return "quit"
    if text in ["h", "help", "?"]:
        return "help"
    if text in ["e", "end"]:
        return 30
    if text.isdigit():
        return int(text)

    if len(text) >= 2 and text[0] in ["p", "a", "d", "r"] and text[1:].isdigit():
        index = int(text[1:])
        if text[0] == "p":
            return index
        if text[0] == "a":
            return 10 + index
        if text[0] == "d":
            return 20 + index
        if text[0] == "r":
            return 32 + index

    return None


def choose_human_action(observation, action_mask, total_reward):
    allowed = set(legal_actions(action_mask))
    while True:
        print_state(observation, action_mask, total_reward)
        choice = parse_action(input("action (p0/a0/d0/end/r0, h for help, q to quit): "))
        if choice == "quit":
            return None
        if choice == "help":
            print("Use pN to play hand card N, aN to attack with hand card N, dN to defend, rN for rewards.")
            print("You can also type the raw action id shown in the legal action list.")
            continue
        if choice in allowed:
            return int(choice)
        print(f"Invalid or illegal action: {choice}")


def append_jsonl(path, row):
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("a", encoding="utf-8") as file:
        file.write(json.dumps(row, ensure_ascii=True) + "\n")


def load_or_create_buffer(path, capacity, overwrite):
    if path.exists() and not overwrite:
        replay_buffer, metadata = ReplayBuffer.load(path, capacity=capacity)
        print(f"Loaded existing demo buffer: {path} ({len(replay_buffer)} transitions)")
        return replay_buffer, metadata
    if overwrite and path.exists():
        print(f"Overwriting demo buffer: {path}")
    return ReplayBuffer(capacity=capacity), replay_metadata()


def record_transition(
    replay_buffer,
    jsonl_path,
    episode,
    step,
    state,
    action_mask,
    action,
    reward,
    next_state,
    done,
    next_action_mask,
    info,
    previous_observation,
    timed_out,
    sampling_priority,
):
    next_observation = info["observation"]
    priority = replay_priority_for_level(next_observation["level"])
    replay_buffer.add(
        state=state,
        action=action,
        reward=reward,
        next_state=next_state,
        done=done,
        next_action_mask=next_action_mask,
        action_mask=action_mask,
        priority=priority,
        sampling_priority=sampling_priority,
    )
    append_jsonl(
        jsonl_path,
        {
            "recorded_at": datetime.now(timezone.utc).isoformat(),
            "episode": int(episode),
            "step": int(step),
            "action": int(action),
            "action_name": describe_action(action, previous_observation),
            "reward": float(reward),
            "done": bool(done),
            "timed_out": bool(timed_out),
            "action_result": info["actionResult"],
            "observation": previous_observation,
            "next_observation": next_observation,
            "action_mask": [bool(value) for value in action_mask],
            "next_action_mask": [bool(value) for value in next_action_mask],
        },
    )


def save_buffer(replay_buffer, path, metadata):
    replay_buffer.save(path, metadata=metadata)
    print(f"Saved demo replay buffer: {path} ({len(replay_buffer)} transitions)")


def main():
    args = parse_args()
    output_path = project_path(args.output)
    jsonl_path = project_path(args.jsonl)
    replay_buffer, metadata = load_or_create_buffer(output_path, args.capacity, args.overwrite)
    metadata = {**metadata, **replay_metadata()}

    env = MathCardVectorEnv()
    new_transitions = 0
    try:
        for episode in range(1, args.episodes + 1):
            state, action_mask, reset_info = env.reset(return_info=True)
            observation = reset_info["observation"]
            total_reward = 0.0
            done = False

            for step in range(1, args.max_steps + 1):
                action = choose_human_action(observation, action_mask, total_reward)
                if action is None:
                    print("Recording stopped by user.")
                    return

                previous_state = np.array(state, dtype=np.float32).tolist()
                previous_mask = [bool(value) for value in action_mask]
                previous_observation = observation
                next_state, reward, done, next_action_mask, info = env.step(action)
                timed_out = step == args.max_steps and not done
                transition_done = done or timed_out
                transition_reward = reward + args.timeout_penalty if timed_out else reward
                total_reward += transition_reward

                record_transition(
                    replay_buffer,
                    jsonl_path,
                    episode,
                    step,
                    previous_state,
                    previous_mask,
                    action,
                    transition_reward,
                    np.array(next_state, dtype=np.float32).tolist(),
                    transition_done,
                    [bool(value) for value in next_action_mask],
                    info,
                    previous_observation,
                    timed_out,
                    args.demo_sampling_priority,
                )
                new_transitions += 1

                action_result = info["actionResult"]
                print(
                    f"result: reward={transition_reward:.2f} "
                    f"legal={action_result['legal']} "
                    f"done={transition_done} "
                    f"message={action_result['message']}"
                )

                state = next_state
                action_mask = next_action_mask
                observation = info["observation"]
                done = transition_done
                if done:
                    break

            print(
                f"Episode {episode:04d} finished: "
                f"level={observation['level']} "
                f"state={observation['state']} "
                f"steps={step} "
                f"reward={total_reward:.2f}"
            )
    except KeyboardInterrupt:
        print("")
        print("Recording interrupted.")
    finally:
        env.close()
        if new_transitions > 0:
            save_buffer(replay_buffer, output_path, metadata)
            print(f"Appended raw transition log: {jsonl_path}")
        else:
            print("No new transitions recorded.")


if __name__ == "__main__":
    main()
