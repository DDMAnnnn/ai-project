import math


STATES = ["INITIAL", "BATTLE", "REWARD", "VICTORY"]
MODES = ["NORMAL", "HARD", "MASTER", "HELL", "INFERNO"]
ENEMY_TYPES = ["NONE", "NORMAL", "ELITE", "BOSS"]
OPERATIONS = ["\0", "+", "-", "*", "/", "^", "m"]

CARD_TYPES = ["EMPTY", "NUMBER", "SYMBOL", "SPECIAL"]
CARD_SYMBOLS = ["NONE", "+", "-", "*", "/", "^", "m"]
SPECIAL_CARDS = ["NONE", "merge", "split", "draw"]
REWARD_TYPES = ["EMPTY", "CARD", "SKIP", "REMOVE"]

MAX_HAND_CARDS = 10
MAX_REWARD_OPTIONS = 5
INT_MAX = 2_147_483_647
FINAL_BOSS_MAX_HEALTH = 16_000
ENEMY_MAX_HEALTH_SCALE = FINAL_BOSS_MAX_HEALTH / 2
ENCODER_VERSION = "log_cards_calc_enemy_max_8000_v1"


def one_hot(value, choices):
    if value not in choices:
        raise ValueError(f"Unknown value {value!r}. Expected one of: {choices}")

    vector = []
    for choice in choices:
        if value == choice:
            vector.append(1.0)
        else:
            vector.append(0.0)
    return vector


def normalize(value, scale):
    return float(value) / float(scale)


def normalize_int_sized_value(value):
    value = max(0, int(value))
    return math.log1p(value) / math.log1p(INT_MAX)


def normalize_enemy_health(health, max_health):
    max_health = max(1, int(max_health))
    health = max(0, int(health))
    return min(float(health) / float(max_health), 1.0)


def normalize_enemy_max_health(value):
    return normalize(value, ENEMY_MAX_HEALTH_SCALE)


def normalize_calc_value(value):
    if value == -2147483648:
        return [1.0, 0.0]
    return [0.0, normalize_int_sized_value(value)]


def encode_card(card):
    if card is None:
        return (
            one_hot("EMPTY", CARD_TYPES)
            + [0.0]
            + one_hot("NONE", CARD_SYMBOLS)
            + one_hot("NONE", SPECIAL_CARDS)
        )

    card_type, card_name = card.split(":", 1)

    if card_type == "NUMBER":
        return (
            one_hot("NUMBER", CARD_TYPES)
            + [normalize_int_sized_value(card_name)]
            + one_hot("NONE", CARD_SYMBOLS)
            + one_hot("NONE", SPECIAL_CARDS)
        )

    if card_type == "SYMBOL":
        return (
            one_hot("SYMBOL", CARD_TYPES)
            + [0.0]
            + one_hot(card_name, CARD_SYMBOLS)
            + one_hot("NONE", SPECIAL_CARDS)
        )

    if card_type == "SPECIAL":
        return (
            one_hot("SPECIAL", CARD_TYPES)
            + [0.0]
            + one_hot("NONE", CARD_SYMBOLS)
            + one_hot(card_name, SPECIAL_CARDS)
        )

    raise ValueError(f"Unknown card type: {card_type}")


def reward_option_to_card(reward_option):
    if reward_option.isdigit():
        return "NUMBER:" + reward_option

    if reward_option in ["+", "-", "*", "/", "^"]:
        return "SYMBOL:" + reward_option

    if reward_option in ["merge", "split", "draw"]:
        return "SPECIAL:" + reward_option

    raise ValueError(f"Unknown card reward option: {reward_option}")


def encode_reward_option(reward_option):
    if reward_option is None:
        return one_hot("EMPTY", REWARD_TYPES) + encode_card(None)

    if reward_option == "Skip":
        return one_hot("SKIP", REWARD_TYPES) + encode_card(None)

    if reward_option == "Remove":
        return one_hot("REMOVE", REWARD_TYPES) + encode_card(None)

    return one_hot("CARD", REWARD_TYPES) + encode_card(reward_option_to_card(reward_option))


def encode_hand_cards(hand_cards):
    vector = []
    for index in range(MAX_HAND_CARDS):
        if index < len(hand_cards):
            vector += encode_card(hand_cards[index])
        else:
            vector += encode_card(None)
    return vector


def encode_reward_options(reward_options):
    vector = []
    for index in range(MAX_REWARD_OPTIONS):
        if index < len(reward_options):
            vector += encode_reward_option(reward_options[index])
        else:
            vector += encode_reward_option(None)
    return vector


def observation_to_vector(observation):
    vector = []

    vector += one_hot(observation["state"], STATES)
    vector += one_hot(observation["mode"], MODES)
    vector += one_hot(observation["enemyType"], ENEMY_TYPES)
    vector += one_hot(observation["operation"], OPERATIONS)

    vector.append(normalize(observation["level"], 50))
    vector.append(normalize(observation["playerHealth"], 500))
    vector.append(normalize(observation["shield"], 999))
    vector.append(normalize(observation["round"], 100))
    vector.append(normalize_enemy_health(observation["enemyHealth"], observation["enemyMaxHealth"]))
    vector.append(normalize_enemy_max_health(observation["enemyMaxHealth"]))
    vector.append(normalize(observation["enemyDamage"], 100))
    vector.append(normalize(observation["drawingPileSize"], 100))
    vector.append(normalize(observation["discardPileSize"], 100))
    vector.append(normalize(observation["deckSize"], 100))
    vector += normalize_calc_value(observation["calcValue"])

    vector += encode_hand_cards(observation["handCards"])
    vector += encode_reward_options(observation["rewardOptions"])

    if len(vector) != VECTOR_SIZE:
        raise ValueError(f"Expected vector length {VECTOR_SIZE}, got {len(vector)}")

    return vector


CARD_VECTOR_SIZE = len(encode_card(None))
REWARD_OPTION_VECTOR_SIZE = len(encode_reward_option(None))
BASE_VECTOR_SIZE = (
    len(STATES)
    + len(MODES)
    + len(ENEMY_TYPES)
    + len(OPERATIONS)
    + 10
    + 2
)
VECTOR_SIZE = (
    BASE_VECTOR_SIZE
    + MAX_HAND_CARDS * CARD_VECTOR_SIZE
    + MAX_REWARD_OPTIONS * REWARD_OPTION_VECTOR_SIZE
)


if __name__ == "__main__":
    from mathcard_java_env import MathCardJavaEnv

    with MathCardJavaEnv() as env:
        result = env.reset()
        vector = observation_to_vector(result["observation"])

        print("Observation encoded successfully.")
        print(f"Vector length: {len(vector)}")
        print(f"Expected length: {VECTOR_SIZE}")
        print(f"Legal actions: {sum(result['actionMask'])}")
