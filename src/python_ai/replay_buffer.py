import random
from dataclasses import dataclass
import json
from pathlib import Path

import numpy as np


@dataclass
class Transition:
    state: list
    action: int
    reward: float
    next_state: list
    done: bool
    next_action_mask: list
    priority: int = 0
    sampling_priority: float = 1.0


class ReplayBuffer:
    def __init__(self, capacity):
        self.capacity = int(capacity)
        self.memory = []
        self._max_sampling_priority = 1.0

    def add(
        self,
        state,
        action,
        reward,
        next_state,
        done,
        next_action_mask,
        priority=0,
        sampling_priority=None,
    ):
        if sampling_priority is None:
            sampling_priority = self.max_sampling_priority()

        transition = Transition(
            state=state,
            action=action,
            reward=reward,
            next_state=next_state,
            done=done,
            next_action_mask=next_action_mask,
            priority=int(priority),
            sampling_priority=float(sampling_priority),
        )
        self._max_sampling_priority = max(
            self._max_sampling_priority,
            transition.sampling_priority,
        )
        if len(self.memory) < self.capacity:
            self.memory.append(transition)
            return True

        replacement_index = self._replacement_index(transition.priority)
        if replacement_index is None:
            return False

        self.memory[replacement_index] = transition
        return True

    def sample(self, batch_size):
        if batch_size > len(self.memory):
            raise ValueError(
                f"Cannot sample {batch_size} transitions from buffer with {len(self.memory)} items."
            )

        return random.sample(self.memory, batch_size)

    def sample_prioritized(self, batch_size, alpha=0.6, beta=0.4):
        if batch_size > len(self.memory):
            raise ValueError(
                f"Cannot sample {batch_size} transitions from buffer with {len(self.memory)} items."
            )

        priorities = np.array(
            [transition.sampling_priority for transition in self.memory],
            dtype=np.float64,
        )
        priorities = np.maximum(priorities, 1.0e-12)
        if alpha <= 0.0:
            probabilities = np.full(len(self.memory), 1.0 / len(self.memory), dtype=np.float64)
        else:
            scaled_priorities = np.power(priorities, float(alpha))
            probabilities = scaled_priorities / np.sum(scaled_priorities)

        indices = np.random.choice(
            len(self.memory),
            size=batch_size,
            replace=True,
            p=probabilities,
        )
        sample_probabilities = probabilities[indices]
        weights = np.power(len(self.memory) * sample_probabilities, -float(beta))
        weights = weights / np.max(weights)
        transitions = [self.memory[index] for index in indices]
        return transitions, indices, weights.astype(np.float32)

    def update_sampling_priorities(self, indices, priorities):
        for index, priority in zip(indices, priorities):
            sampling_priority = max(float(priority), 1.0e-12)
            self.memory[int(index)].sampling_priority = sampling_priority
            self._max_sampling_priority = max(self._max_sampling_priority, sampling_priority)

    def max_sampling_priority(self):
        return self._max_sampling_priority

    def save(self, path, metadata=None):
        path = Path(path)
        path.parent.mkdir(exist_ok=True)

        metadata = metadata or {}
        states = np.array([transition.state for transition in self.memory], dtype=np.float32)
        next_states = np.array(
            [transition.next_state for transition in self.memory],
            dtype=np.float32,
        )
        actions = np.array([transition.action for transition in self.memory], dtype=np.int16)
        rewards = np.array([transition.reward for transition in self.memory], dtype=np.float32)
        dones = np.array([transition.done for transition in self.memory], dtype=np.bool_)
        next_action_masks = np.array(
            [transition.next_action_mask for transition in self.memory],
            dtype=np.bool_,
        )
        priorities = np.array([transition.priority for transition in self.memory], dtype=np.int16)
        sampling_priorities = np.array(
            [transition.sampling_priority for transition in self.memory],
            dtype=np.float32,
        )

        np.savez_compressed(
            path,
            states=states,
            next_states=next_states,
            actions=actions,
            rewards=rewards,
            dones=dones,
            next_action_masks=next_action_masks,
            priorities=priorities,
            sampling_priorities=sampling_priorities,
            metadata_json=np.array(json.dumps(metadata), dtype=np.str_),
        )

    @classmethod
    def load(cls, path, capacity, expected_metadata=None):
        path = Path(path)
        with np.load(path, allow_pickle=False) as data:
            metadata = json.loads(str(data["metadata_json"]))
            expected_metadata = expected_metadata or {}
            for key, expected_value in expected_metadata.items():
                actual_value = metadata.get(key)
                if actual_value != expected_value:
                    raise ValueError(
                        f"Replay buffer metadata mismatch for {key}: "
                        f"expected {expected_value!r}, got {actual_value!r}"
                    )

            replay_buffer = cls(capacity=capacity)
            states = data["states"]
            next_states = data["next_states"]
            actions = data["actions"]
            rewards = data["rewards"]
            dones = data["dones"]
            next_action_masks = data["next_action_masks"]
            priorities = data["priorities"] if "priorities" in data else np.zeros(len(actions))
            sampling_priorities = (
                data["sampling_priorities"]
                if "sampling_priorities" in data
                else np.ones(len(actions), dtype=np.float32)
            )

            for index in range(len(actions)):
                replay_buffer.add(
                    state=states[index].astype(np.float32).tolist(),
                    action=int(actions[index]),
                    reward=float(rewards[index]),
                    next_state=next_states[index].astype(np.float32).tolist(),
                    done=bool(dones[index]),
                    next_action_mask=next_action_masks[index].astype(bool).tolist(),
                    priority=int(priorities[index]),
                    sampling_priority=float(sampling_priorities[index]),
                )

        return replay_buffer, metadata

    def __len__(self):
        return len(self.memory)

    def _replacement_index(self, new_priority):
        lowest_priority = min(transition.priority for transition in self.memory)
        if new_priority < lowest_priority:
            return None

        for index, transition in enumerate(self.memory):
            if transition.priority == lowest_priority:
                return index

        return None
