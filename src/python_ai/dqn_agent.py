import random

import numpy as np
import tensorflow as tf


@tf.keras.utils.register_keras_serializable(package="MathCard")
class DuelingQValues(tf.keras.layers.Layer):
    def call(self, inputs):
        value, advantage = inputs
        centered_advantage = advantage - tf.reduce_mean(advantage, axis=1, keepdims=True)
        return value + centered_advantage


@tf.keras.utils.register_keras_serializable(package="MathCard")
class MaskedDuelingQValues(tf.keras.layers.Layer):
    def call(self, inputs):
        value, advantage, action_mask = inputs
        action_mask = tf.cast(action_mask > 0.0, advantage.dtype)
        legal_count = tf.reduce_sum(action_mask, axis=1, keepdims=True)
        legal_count = tf.maximum(legal_count, tf.constant(1.0, dtype=advantage.dtype))
        legal_advantage_mean = (
            tf.reduce_sum(advantage * action_mask, axis=1, keepdims=True) / legal_count
        )
        return value + advantage - legal_advantage_mean


class DQNAgent:
    def __init__(
        self,
        state_size,
        action_count,
        learning_rate=0.001,
        gamma=0.99,
        epsilon=1.0,
        epsilon_min=0.05,
        epsilon_decay=0.995,
        gradient_clip_norm=10.0,
        argmax_tie_epsilon=0.05,
        rank_sampling_decay=1.25,
    ):
        self.state_size = state_size
        self.action_count = action_count
        self.learning_rate = learning_rate
        self.gamma = gamma
        self.epsilon = epsilon
        self.epsilon_min = epsilon_min
        self.epsilon_decay = epsilon_decay
        self.gradient_clip_norm = gradient_clip_norm
        self.argmax_tie_epsilon = argmax_tie_epsilon
        self.rank_sampling_decay = rank_sampling_decay

        self.model = self._build_model()
        self.target_model = self._build_model()
        self.update_target_model()

    def _build_model(self):
        state_input = tf.keras.Input(shape=(self.state_size,), name="state")
        action_mask_input = tf.keras.Input(
            shape=(self.action_count,),
            name="action_mask",
            dtype=tf.float32,
        )
        hidden = tf.keras.layers.Dense(256, activation="relu")(state_input)
        hidden = tf.keras.layers.Dense(256, activation="relu")(hidden)
        hidden = tf.keras.layers.Dense(128, activation="relu")(hidden)

        value_stream = tf.keras.layers.Dense(64, activation="relu")(hidden)
        value = tf.keras.layers.Dense(1, activation="linear")(value_stream)
        advantage_stream = tf.keras.layers.Dense(64, activation="relu")(hidden)
        advantage = tf.keras.layers.Dense(self.action_count, activation="linear")(advantage_stream)

        q_values = MaskedDuelingQValues()([value, advantage, action_mask_input])
        model = tf.keras.Model(inputs=[state_input, action_mask_input], outputs=q_values)
        self.compile_model(model)
        return model

    def compile_model(self, model):
        model.compile(
            optimizer=tf.keras.optimizers.Adam(
                learning_rate=self.learning_rate,
                global_clipnorm=self.gradient_clip_norm,
            ),
            loss=tf.keras.losses.Huber(),
        )

    def update_target_model(self):
        self.target_model.set_weights(self.model.get_weights())

    def soft_update_target_model(self, tau):
        online_weights = self.model.get_weights()
        target_weights = self.target_model.get_weights()
        updated_weights = [
            tau * online_weight + (1.0 - tau) * target_weight
            for online_weight, target_weight in zip(online_weights, target_weights)
        ]
        self.target_model.set_weights(updated_weights)

    def choose_action(self, state, action_mask):
        return self.choose_action_info(state, action_mask)["action"]

    def choose_action_info(
        self,
        state,
        action_mask,
        use_rank_sampling=False,
        rank_sampling_probability=0.0,
        rank_top_k=3,
        rank_gap_threshold=0.5,
        require_q_values=False,
    ):
        legal_actions = self._legal_actions(action_mask)
        if not legal_actions:
            raise RuntimeError("No legal actions available.")

        epsilon_roll = random.random()
        if epsilon_roll < self.epsilon and not use_rank_sampling and not require_q_values:
            return {
                "action": int(random.choice(legal_actions)),
                "q_value": None,
                "q_margin": None,
                "greedy_action": None,
                "greedy_q_value": None,
                "selection_mode": "epsilon_uniform",
            }

        q_values = self._predict_q_values(
            np.array([state], dtype=np.float32),
            np.array([action_mask], dtype=np.bool_),
        )[0]
        greedy_action, greedy_q_value, q_margin = self._select_greedy_action(q_values, legal_actions)

        rank_sampling_allowed = (
            use_rank_sampling
            and len(legal_actions) > 1
            and rank_top_k > 1
            and (rank_gap_threshold is None or q_margin <= rank_gap_threshold)
        )

        selection_mode = "greedy"
        action = greedy_action
        q_value = greedy_q_value
        if epsilon_roll < self.epsilon:
            if rank_sampling_allowed:
                action = self._sample_ranked_action(q_values, legal_actions, rank_top_k)
                selection_mode = "epsilon_rank"
            else:
                action = int(random.choice(legal_actions))
                selection_mode = "epsilon_uniform"
            q_value = float(q_values[action])
        elif rank_sampling_allowed and random.random() < rank_sampling_probability:
            action = self._sample_ranked_action(q_values, legal_actions, rank_top_k)
            selection_mode = "rank"
            q_value = float(q_values[action])

        return {
            "action": int(action),
            "q_value": q_value,
            "q_margin": q_margin,
            "greedy_action": int(greedy_action),
            "greedy_q_value": greedy_q_value,
            "selection_mode": selection_mode,
        }

    def greedy_action_info(self, state, action_mask):
        legal_actions = self._legal_actions(action_mask)
        if not legal_actions:
            raise RuntimeError("No legal actions available.")

        q_values = self._predict_q_values(
            np.array([state], dtype=np.float32),
            np.array([action_mask], dtype=np.bool_),
        )[0]
        return self._select_greedy_action(q_values, legal_actions)

    def train_on_batch(self, transitions, sample_weights=None):
        states = np.array([transition.state for transition in transitions], dtype=np.float32)
        next_states = np.array([transition.next_state for transition in transitions], dtype=np.float32)
        actions = np.array([transition.action for transition in transitions], dtype=np.int32)
        rewards = np.array([transition.reward for transition in transitions], dtype=np.float32)
        dones = np.array([transition.done for transition in transitions], dtype=np.bool_)
        next_action_masks = np.array(
            [transition.next_action_mask for transition in transitions],
            dtype=np.bool_,
        )
        action_masks = np.array(
            [
                transition.action_mask
                if transition.action_mask is not None
                else [True] * self.action_count
                for transition in transitions
            ],
            dtype=np.bool_,
        )

        current_q_values = self._predict_q_values(states, action_masks)
        next_online_q_values = self._predict_q_values(next_states, next_action_masks)
        masked_next_online_q_values = np.where(next_action_masks, next_online_q_values, -1.0e9)
        best_next_actions = self._batch_select_greedy_actions(
            masked_next_online_q_values,
            next_action_masks,
        )

        next_target_q_values = self._predict_q_values(
            next_states,
            next_action_masks,
            model=self.target_model,
        )
        best_next_q_values = next_target_q_values[np.arange(len(transitions)), best_next_actions]
        best_next_q_values = np.where(dones, 0.0, best_next_q_values)

        predicted_action_q_values = current_q_values[np.arange(len(transitions)), actions]
        target_action_q_values = rewards + self.gamma * best_next_q_values
        td_errors = target_action_q_values - predicted_action_q_values

        target_q_values = current_q_values.copy()
        target_q_values[np.arange(len(transitions)), actions] = target_action_q_values

        next_q_values = np.concatenate(
            [
                next_online_q_values.reshape(-1),
                next_target_q_values.reshape(-1),
            ]
        )
        legal_q_values = np.concatenate(
            [
                next_online_q_values[next_action_masks],
                next_target_q_values[next_action_masks],
            ]
        )
        illegal_q_values = np.concatenate(
            [
                next_online_q_values[~next_action_masks],
                next_target_q_values[~next_action_masks],
            ]
        )

        if sample_weights is not None:
            sample_weights = np.array(sample_weights, dtype=np.float32)

        history = self.model.fit(
            [states, action_masks.astype(np.float32)],
            target_q_values,
            sample_weight=sample_weights,
            epochs=1,
            verbose=0,
        )
        return {
            "loss": float(history.history["loss"][0]),
            "pred_q_mean": float(np.mean(predicted_action_q_values)),
            "target_q_mean": float(np.mean(target_action_q_values)),
            "td_errors": td_errors.astype(np.float32),
            "td_abs_mean": float(np.mean(np.abs(td_errors))),
            "td_abs_max": float(np.max(np.abs(td_errors))),
            "q_abs_max": float(
                max(
                    np.max(np.abs(current_q_values)),
                    np.max(np.abs(next_online_q_values)),
                    np.max(np.abs(next_target_q_values)),
                )
            ),
            "q_min": float(np.min(next_q_values)),
            "q_max": float(np.max(next_q_values)),
            "legal_q_max": float(np.max(legal_q_values)) if legal_q_values.size else 0.0,
            "illegal_q_max": float(np.max(illegal_q_values)) if illegal_q_values.size else 0.0,
        }

    def decay_epsilon(self):
        self.epsilon = max(self.epsilon_min, self.epsilon * self.epsilon_decay)

    def save(self, path):
        self.model.save(path)

    def _predict_q_values(self, states, action_masks, model=None):
        model = model or self.model
        return model(
            [
                np.array(states, dtype=np.float32),
                np.array(action_masks, dtype=np.float32),
            ],
            training=False,
        ).numpy()

    def _legal_actions(self, action_mask):
        return [index for index, allowed in enumerate(action_mask) if allowed]

    def _mask_illegal_q_values(self, q_values, action_mask):
        action_mask_array = np.array(action_mask, dtype=np.bool_)
        return np.where(action_mask_array, q_values, -1.0e9)

    def _select_greedy_action(self, q_values, legal_actions):
        legal_q_values = np.array([q_values[action] for action in legal_actions], dtype=np.float32)
        best_q_value = float(np.max(legal_q_values))
        if len(legal_q_values) > 1:
            second_best_q_value = float(np.partition(legal_q_values, -2)[-2])
        else:
            second_best_q_value = best_q_value

        candidate_actions = [
            action
            for action in legal_actions
            if float(q_values[action]) >= best_q_value - self.argmax_tie_epsilon
        ]
        selected_action = int(random.choice(candidate_actions))
        selected_q_value = float(q_values[selected_action])
        return selected_action, selected_q_value, best_q_value - second_best_q_value

    def _sample_ranked_action(self, q_values, legal_actions, rank_top_k):
        ranked_actions = sorted(
            legal_actions,
            key=lambda action: float(q_values[action]),
            reverse=True,
        )[:max(1, rank_top_k)]
        ranks = np.arange(1, len(ranked_actions) + 1, dtype=np.float32)
        weights = 1.0 / np.power(ranks, self.rank_sampling_decay)
        probabilities = weights / np.sum(weights)
        return int(np.random.choice(ranked_actions, p=probabilities))

    def _batch_select_greedy_actions(self, masked_q_values, action_masks):
        selected_actions = np.zeros(masked_q_values.shape[0], dtype=np.int32)
        for row_index, row in enumerate(masked_q_values):
            legal_actions = np.flatnonzero(action_masks[row_index])
            if legal_actions.size == 0:
                continue

            legal_q_values = row[legal_actions]
            best_q_value = float(np.max(legal_q_values))
            candidate_actions = legal_actions[
                legal_q_values >= best_q_value - self.argmax_tie_epsilon
            ]
            selected_actions[row_index] = int(random.choice(candidate_actions.tolist()))
        return selected_actions
