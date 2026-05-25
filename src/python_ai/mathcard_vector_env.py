from mathcard_java_env import MathCardJavaEnv
from observation_encoder import observation_to_vector


class MathCardVectorEnv:
    def __init__(self):
        self.env = MathCardJavaEnv()

    def reset(self, return_info=False):
        result = self.env.reset()
        state = observation_to_vector(result["observation"])
        action_mask = result["actionMask"]
        if return_info:
            return state, action_mask, result
        return state, action_mask

    def step(self, action):
        result = self.env.step(action)
        next_state = observation_to_vector(result["observation"])
        reward = result["reward"]
        done = result["done"]
        action_mask = result["actionMask"]
        info = result
        return next_state, reward, done, action_mask, info

    def close(self):
        self.env.close()
