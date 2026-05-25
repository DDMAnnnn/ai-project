package ai_access;

import model.cards.NumberCard;
import ui.GameState;

public class MathCardEnv {
    private static final int PLAY_CARD_START = 0;
    private static final int ATTACK_START = 10;
    private static final int DEFEND_START = 20;
    private static final int END_ROUND = 30;
    //private static final int CANCEL = 31;
    private static final int REWARD_START = 32;
    private static final int ACTION_COUNT = 37;

    private CardGameCore game;

    public MathCardEnv() {
        this(new CardGameCore());
    }

    public MathCardEnv(CardGameCore game) {
        this.game = game;
    }

    public StepResult reset() {
        ActionResult actionResult = game.start();
        return new StepResult(
                game.getObservation(),
                actionResult.getReward(),
                actionResult.isDone(),
                getActionMask(),
                actionResult
        );
    }

    public StepResult step(int action) {
        ActionResult actionResult;

        if (action >= PLAY_CARD_START && action < ATTACK_START) {
            actionResult = game.playCardAt(action - PLAY_CARD_START);
        } else if (action >= ATTACK_START && action < DEFEND_START) {
            actionResult = game.attackWithCardAt(action - ATTACK_START);
        } else if (action >= DEFEND_START && action < END_ROUND) {
            actionResult = game.defendWithCardAt(action - DEFEND_START);
        } else if (action == END_ROUND) {
            actionResult = game.endRound();
        }
        //  else if (action == CANCEL) {
        //     actionResult = game.cancel();
        // } 
        else if (action >= REWARD_START && action < ACTION_COUNT) {
            actionResult = game.chooseRewardAt(action - REWARD_START);
        } else {
            actionResult = new ActionResult(false, -1.0, game.isGameOver() || game.isGameWon(),
                    "unknown action");
        }

        return new StepResult(
                game.getObservation(),
                actionResult.getReward(),
                actionResult.isDone(),
                getActionMask(),
                actionResult
        );
    }

    public CardGameCore getGame() {
        return game;
    }

    public boolean[] getActionMask() {
        boolean[] mask = new boolean[ACTION_COUNT];

        if (game.getState() == GameState.REWARD) {
            for (int i = 0; i < game.getRewards().size(); i++) {
                mask[REWARD_START + i] = true;
            }
            return mask;
        }

        if (game.getState() == GameState.BATTLE) {
            for (int i = 0; i < game.getHand().size() && i < ATTACK_START; i++) {
                if (game.canPlayCardAt(i)) {
                    mask[PLAY_CARD_START + i] = true;
                }
            }

            if (game.getLastCard() == null) {
                for (int i = 0; i < game.getHand().size() && i < 10; i++) {
                    if (game.getHand().get(i) instanceof NumberCard) {
                        if (game.canAttackWithCardAt(i)) {
                            mask[ATTACK_START + i] = true;
                        }
                        mask[DEFEND_START + i] = true;
                    }
                }
            }

            mask[END_ROUND] = true;
        }

        return mask;
    }
}
