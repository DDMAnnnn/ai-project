import random

from mathcard_vector_env import MathCardVectorEnv
from observation_encoder import VECTOR_SIZE


ACTION_COUNT = 37


def choose_random_legal_action(action_mask):
    legal_actions = []
    for index, allowed in enumerate(action_mask):
        if allowed:
            legal_actions.append(index)

    if not legal_actions:
        raise RuntimeError("No legal actions available.")

    return random.choice(legal_actions)


def assert_vector_env_shapes(state, action_mask):
    if len(state) != VECTOR_SIZE:
        raise RuntimeError(f"Expected state length {VECTOR_SIZE}, got {len(state)}.")

    if len(action_mask) != ACTION_COUNT:
        raise RuntimeError(f"Expected action mask length {ACTION_COUNT}, got {len(action_mask)}.")


def main():
    env = MathCardVectorEnv()
    try:
        state, action_mask = env.reset()
        assert_vector_env_shapes(state, action_mask)

        total_reward = 0.0
        final_info = None

        for step_index in range(500):
            action = choose_random_legal_action(action_mask)
            state, reward, done, action_mask, info = env.step(action)
            assert_vector_env_shapes(state, action_mask)

            if not info["actionResult"]["legal"]:
                message = info["actionResult"]["message"]
                raise RuntimeError(f"Masked action {action} returned illegal: {message}")

            total_reward += reward
            final_info = info

            if done:
                break

        observation = final_info["observation"]
        print("Vector env smoke test finished.")
        print(f"Final level: {observation['level']}")
        print(f"Final state: {observation['state']}")
        print(f"Done: {final_info['done']}")
        print(f"Total reward: {total_reward:.2f}")
        print(f"State vector length: {len(state)}")
        print(f"Action mask length: {len(action_mask)}")
    finally:
        env.close()


if __name__ == "__main__":
    main()
