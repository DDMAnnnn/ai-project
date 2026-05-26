# AI Training Memo

## Current Direction

- Retrain from a clean model after reward/deck-quality and architecture changes.
- Keep `PER` enabled by default, but treat it as a tuning knob rather than a guaranteed improvement.
- Use compatible human demo replay as an optional seed when starting with an empty replay buffer.
- Avoid loading old replay buffers across reward/encoder/model-version changes.

## Already Implemented

- Dueling DQN.
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

### Masked Dueling / Illegal Q Control

- Current action masks prevent illegal actions from being selected, but illegal Q values can still drift.
- Because dueling networks subtract a mean advantage term, drifting illegal actions can pollute legal-action values.
- Preferred next architecture change: masked dueling, where illegal actions are excluded from the dueling aggregation and Q target selection.
- This requires a new model architecture/version and should be paired with clean retraining.

### Reward Structure Review

- Current reward can become positive around level 20, which may make the model comfortable stabilizing at 20-30 instead of pushing to 50.
- After masked dueling is in place, re-check:
  - battle clear vs boss clear reward;
  - death/missing-level penalty;
  - level 30/40/50 incentive spacing;
  - timeout behavior.

### Defense vs Attack Situation Features

- Design goal: if the player cannot kill this turn, defense should often be preferred because HP does not refresh.
- Current learning signal uses effective shield reward plus damage-taken penalty.
- Potential observation features:
  - `lethalAvailable`;
  - `bestImmediateDamage`;
  - `incomingDamageAfterShield`;
  - `canFullyBlockThisTurn`.

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

1. Implement masked dueling and bump model version.
2. Start clean training with the new reward version and optional human demo seed.
3. Re-evaluate level distribution, Q scale, illegal Q drift, and boss L20/L30/L40 clears.
4. Tune reward structure if the model still settles around 20-30.
5. Consider NoisyNet once the base model is stable.
6. Use deck predictor after more high-level or victory data exists.
7. Prototype shallow lookahead in eval/watch if decisions still look locally good but strategically weak.
