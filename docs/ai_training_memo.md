# AI Training Memo

## Current Direction

- Retrain from a clean model after reward/deck-quality and architecture changes.
- Keep `PER` enabled by default, but treat it as a tuning knob rather than a guaranteed improvement.
- Use compatible human demo replay as an optional seed when starting with an empty replay buffer.
- Avoid loading old replay buffers across reward/encoder/model-version changes.

## Already Implemented

- Dueling DQN.
- Masked dueling DQN:
  - model takes the action mask as a second input;
  - dueling advantage mean excludes illegal actions;
  - model version bumped to `masked_dueling_dqn_v1`.
  - replay metadata now requires stored action masks.
- Prioritized replay as a switch, default on.
- Boss diagnostics by boss level: L10/L20/L30/L40/L50 clear and damage average.
- Reward spike diagnostics and optional stop-on-spike.
- Human demo recorder:
  - default append to `models/human_demo_replay_buffer.npz`;
  - default JSONL log at `logs/human_demo_steps.jsonl`;
  - compatible demo replay can seed an empty training replay buffer.
- Deck-quality update for retraining:
  - lower `+` value;
  - moderately raise draw/cycle value;
  - add target number-card ratio shaping;
  - bump reward version to `boss_reward_v4_deck_balance`.

## Agreed But Not Implemented

### Reward Structure Review

- Current reward can become positive around level 20, which may make the model comfortable stabilizing at 20-30 instead of pushing to 50.
- After clean masked-dueling retraining, re-check:
  - battle clear vs boss clear reward;
  - death/missing-level penalty;
  - level 30/40/50 incentive spacing;
  - timeout behavior.

### Lookahead / Planning

- The drawing pile is known as an unordered multiset; only draw order is unknown.
- This makes planning easier than a hidden-information POMDP.
- Candidate methods:
  - shallow beam search over top-k legal actions;
  - expectimax/chance-node search for draw outcomes;
  - MCTS with stochastic draw sampling.
- Best first use: eval/watch mode only, not the training loop.
- Possible search score:
  - immediate rewards along the simulated path;
  - expected value over possible draw results;
  - leaf value from the DQN;
  - optional risk penalty for high-variance or bad-tail outcomes.

### Current-Hand Lethal Search

- A narrower and more practical tactical module than general lookahead.
- Also the cleanest basis for defense-vs-attack situation features.
- Goal: answer only one deterministic question: can the current visible hand kill the current enemy without relying on future draws?
- First version should ignore draw uncertainty entirely:
  - use current hand, current `calcValue`, current `operation`, current enemy HP, and legal play/attack rules;
  - search legal play/attack sequences;
  - if a lethal sequence exists, execute the first action in that sequence;
  - otherwise fall back to the DQN.
- This is an inference-time tactical override, similar in spirit to card-game "lethal solvers" or chess tactical search.
- It does not require retraining if used only at eval/watch time, but raw DQN results and DQN+solver results should be reported separately.
- Key derived fact: `closedHandNoLethal`.
  - True when the current hand has no draw/resource-generation actions and current-hand lethal search proves there is no lethal line.
  - This is a deterministic proof that the enemy cannot be killed this turn without introducing new unknown resources.
  - It should not force defense by itself, but it tells the DQN to stop chasing a nonexistent current-turn kill.

Potential deeper interactions with the AI:

- Observation features:
  - `lethalAvailable`;
  - `closedHandNoLethal`;
  - `bestCurrentHandDamage`;
  - `lethalDamageRatio`;
  - `minActionsToLethal`.
  - `incomingDamageAfterShield`;
  - `canFullyBlockThisTurn`.
- Reward shaping:
  - define tactical potential such as `bestCurrentHandDamage / enemyHealth`;
  - give a small reward for increasing that potential;
  - keep it light to avoid making the AI chase damage while ignoring defense or deck quality.
- Defense-vs-attack use:
  - tactical search provides the kill/no-kill fact;
  - shield and incoming-damage features provide the safety fact;
  - DQN still chooses whether no-lethal means defend, attack for setup damage, preserve cards, or end turn.
- Teacher replay:
  - when lethal exists, store the solver's chosen action as high-quality replay data;
  - useful for teaching execution of existing lethal lines;
  - less useful for teaching how to set up future lethal hands.
- Subgoal/options approach:
  - treat "build lethal hand" and "execute lethal" as separate high-level goals;
  - powerful but much more complex than a tactical override.

Recommended first implementation, if we choose to build it later:

1. Current-hand lethal search only.
2. No draw simulation.
3. Override only when lethal is guaranteed.
4. Add optional logging for missed/found lethal and `closedHandNoLethal`.
5. Later consider adding tactical/safety features to observation.

### NoisyNet

- Purpose: replace or supplement epsilon-greedy with parameter-space noise, giving more consistent state-dependent exploration.
- Potential benefit:
  - better exploration in rare high-level states;
  - less random-looking action noise than epsilon-greedy;
  - can help after the base value/Q estimates are stable.
- Risks:
  - changes model architecture and requires retraining;
  - adds another source of variance while reward shaping and illegal Q are still being fixed;
  - harder to diagnose than epsilon.
- Current priority: after masked dueling and clean reward retraining, not before.

### Deck Predictor

- Current data source: `logs/deck_predictor_data.jsonl`.
- Purpose:
  - learn which reward choices/removes improve future survival and boss clears;
  - eventually guide deck quality beyond hand-written heuristics;
  - possibly estimate good number/non-number ratios by stage and deck style.
- Why not urgent yet:
  - predictor labels are more useful once there are more high-level/victory examples;
  - current hand-written deck-quality shaping still needs stabilization;
  - predictor should not be trained on heavily mixed reward versions without care.
- Later use cases:
  - offline analysis of reward choices;
  - auxiliary scoring for reward selection;
  - seeding or correcting deck-quality terms.

## Next Recommended Order

1. Start clean masked-dueling training with the new reward version and optional human demo seed.
2. Re-evaluate level distribution, Q scale, illegal Q drift, and boss L20/L30/L40 clears.
3. Tune reward structure if the model still settles around 20-30.
4. Consider NoisyNet once the base model is stable.
5. Use deck predictor after more high-level or victory data exists.
6. Prototype shallow lookahead in eval/watch if decisions still look locally good but strategically weak.
