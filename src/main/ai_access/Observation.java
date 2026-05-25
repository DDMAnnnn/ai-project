package ai_access;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ui.GameState;
import ui.Mode;

// Represents the state an AI agent can observe.
public class Observation {
    private final GameState state;
    private final Mode mode;
    private final int level;
    private final int playerHealth;
    private final int shield;
    private final int round;
    private final int enemyHealth;
    private final int enemyMaxHealth;
    private final int enemyDamage;
    private final String enemyType;
    private final int drawingPileSize;
    private final int discardPileSize;
    private final int deckSize;
    private final int calcValue;
    private final char operation;
    private final List<String> deckCards;
    private final List<String> handCards;
    private final List<String> rewardOptions;

    public Observation(GameState state, Mode mode, int level, int playerHealth, int shield, int round,
            int enemyHealth, int enemyMaxHealth, int enemyDamage, String enemyType, int drawingPileSize,
            int discardPileSize, int deckSize, int calcValue, char operation, List<String> deckCards,
            List<String> handCards, List<String> rewardOptions) {
        this.state = state;
        this.mode = mode;
        this.level = level;
        this.playerHealth = playerHealth;
        this.shield = shield;
        this.round = round;
        this.enemyHealth = enemyHealth;
        this.enemyMaxHealth = enemyMaxHealth;
        this.enemyDamage = enemyDamage;
        this.enemyType = enemyType;
        this.drawingPileSize = drawingPileSize;
        this.discardPileSize = discardPileSize;
        this.deckSize = deckSize;
        this.calcValue = calcValue;
        this.operation = operation;
        this.deckCards = Collections.unmodifiableList(new ArrayList<>(deckCards));
        this.handCards = Collections.unmodifiableList(new ArrayList<>(handCards));
        this.rewardOptions = Collections.unmodifiableList(new ArrayList<>(rewardOptions));
    }

    public GameState getState() {
        return state;
    }

    public Mode getMode() {
        return mode;
    }

    public int getLevel() {
        return level;
    }

    public int getPlayerHealth() {
        return playerHealth;
    }

    public int getShield() {
        return shield;
    }

    public int getRound() {
        return round;
    }

    public int getEnemyHealth() {
        return enemyHealth;
    }

    public int getEnemyMaxHealth() {
        return enemyMaxHealth;
    }

    public int getEnemyDamage() {
        return enemyDamage;
    }

    public String getEnemyType() {
        return enemyType;
    }

    public int getDrawingPileSize() {
        return drawingPileSize;
    }

    public int getDiscardPileSize() {
        return discardPileSize;
    }

    public int getDeckSize() {
        return deckSize;
    }

    public int getCalcValue() {
        return calcValue;
    }

    public char getOperation() {
        return operation;
    }

    public List<String> getDeckCards() {
        return deckCards;
    }

    public List<String> getHandCards() {
        return handCards;
    }

    public List<String> getRewardOptions() {
        return rewardOptions;
    }
}
