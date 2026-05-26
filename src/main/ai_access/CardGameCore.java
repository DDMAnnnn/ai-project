package ai_access;

import java.util.*;


import model.Deck;
import model.cards.*;
import model.enemies.*;
import ui.Mode;
import ui.GameState;

// EFFECTS: Represents the cardGame.
public class CardGameCore {

    private static final int INITIAL_HEALTH = 100;
    public static final int MAX_HEALTH = 500;
    public static final int WIN_LEVEL = 50;
    private static final int INITIAL_HAND_SIZE = 5;
    private static final int MAX_HAND_SIZE = 10;

    private static final double ILLEGAL_ACTION_PENALTY = -1.0;
    private static final double STEP_PENALTY = -0.01;
    private static final double ENEMY_HEALTH_PROGRESS_REWARD_SCALE = 1.0;
    private static final double BOSS_HEALTH_PROGRESS_REWARD_SCALE = 3.0;
    private static final double EFFECTIVE_DEFEND_REWARD_SCALE = 0.05;
    private static final double DAMAGE_TAKEN_PENALTY_SCALE = 0.10;
    private static final double BATTLE_CLEAR_REWARD = 1.0;
    private static final double BOSS_CLEAR_REWARD = 9.0;
    private static final double VICTORY_REWARD = 100.0;
    private static final double GAME_OVER_BASE_PENALTY = -10.0;
    private static final double MISSING_LEVEL_PENALTY = -0.5;
    private static final double DECK_QUALITY_REWARD_SCALE = 0.05;
    private static final double DECK_QUALITY_REWARD_LIMIT = 0.25;
    private static final double REMOVE_DECK_QUALITY_REWARD_SCALE = 0.02;
    private static final double REMOVE_DECK_QUALITY_REWARD_LIMIT = 0.10;
    private static final double UNDER_TARGET_REMOVE_PENALTY = 0.02;
    private static final double[] DRAW_STAGE_WEIGHT = {0.45, 0.90, 1.15, 1.30, 1.45};
    private static final double[] SCALE_STAGE_WEIGHT = {0.25, 0.95, 1.25, 1.50, 1.75};
    private static final double[] CONTROL_STAGE_WEIGHT = {0.30, 0.75, 1.05, 1.35, 1.65};
    private static final double[] RISK_STAGE_WEIGHT = {0.40, 0.80, 1.00, 1.20, 1.40};
    private static final double[] DRAW_TARGET = {2.0, 4.0, 5.0, 6.0, 7.0};
    private static final double[] SCALE_TARGET = {1.0, 3.0, 4.0, 5.0, 6.0};
    private static final double[] CONTROL_TARGET = {1.0, 2.0, 3.0, 4.0, 5.0};
    private static final double[] RISK_TARGET = {1.5, 2.5, 3.5, 4.5, 5.5};
    private static final int[] TARGET_DECK_SIZE = {16, 22, 26, 30, 34};
    private static final double[][] SMALL_NUMBER_STAGE_BONUS = {
            {0.30, 0.00, -0.20, -0.30, -0.40},
            {0.20, 0.20, 0.00, -0.10, -0.10},
            {0.00, 0.40, 0.60, 0.80, 1.00}
    };

    private Deck deck;
    private List<Card> drawingPile;
    private List<Card> hand;
    private List<Card> discardPile;
    private Enemy currentEnemy;
    private int playerHealth;
    private int level;
    private Card lastCard;
    private int calcValue;
    private char operation;
    private int shield;
    private Mode mode;
    private GameState state;
    private int round;
    private List<String> rewards;

    private static class RemovalChoice {
        private final int index;
        private final double qualityAfter;

        private RemovalChoice(int index, double qualityAfter) {
            this.index = index;
            this.qualityAfter = qualityAfter;
        }
    }

    private static class DeckQualityStats {
        private double drawAccess;
        private double scalePower;
        private double controlPower;
        private int highNumberCount;
        private int numberCount;
        private int symbolCount;
        private int specialCount;
    }

    private static class CardQualityProfile {
        private final double base;
        private final double draw;
        private final double scale;
        private final double control;
        private final double risk;
        private final double[] stageBonus;

        private CardQualityProfile(double base, double draw, double scale, double control, double risk,
                double[] stageBonus) {
            this.base = base;
            this.draw = draw;
            this.scale = scale;
            this.control = control;
            this.risk = risk;
            this.stageBonus = stageBonus;
        }
    }

    public CardGameCore() {
        System.setProperty("mathcard.silentEventLog", "true");
        initializeNewGame();
    }

    public Deck getDeck() {
        return deck;
    }

    public List<Card> getDrawingPile() {
        return drawingPile;
    }

    public List<Card> getHand() {
        return hand;
    }

    public List<Card> getDiscardPile() {
        return discardPile;
    }

    public Enemy getCurrentEnemy() {
        return currentEnemy;
    }

    public int getPlayerHealth() {
        return playerHealth;
    }

    public int getShield() {
        return shield;
    }

    public int getLevel() {
        return level;
    }

    public Card getLastCard() {
        return lastCard;
    }

    public int getCalcValue() {
        return calcValue;
    }

    public char getOperation() {
        return operation;
    }

    public Mode getMode() {
        return mode;
    }

    public int getRound() {
        return round;
    }

    public GameState getState() {
        return state;
    }

    public List<String> getRewards() {
        return rewards;
    }

    public int getMaxHealth() {
        return MAX_HEALTH;
    }

    public int getWinLevel() {
        return WIN_LEVEL;
    }
    
    public boolean isGameOver() {
        return playerHealth <= 0;
    }

    public boolean isGameWon() {
        return state == GameState.VICTORY;
    }

    private void initializeNewGame() {
        deck = new Deck(true);
        drawingPile = new ArrayList<>(deck.getDeck());
        hand = new ArrayList<>();
        discardPile = new ArrayList<>();
        rewards = new ArrayList<String>();
        playerHealth = INITIAL_HEALTH;
        currentEnemy = null;
        shield = 0;
        reset();
        level = 1;
        mode = Mode.NORMAL;
        state = GameState.INITIAL;
        round = 0;
    }

    public ActionResult start() {
        initializeNewGame();
        newBattle();
        return legalResult(0.0, "new game started");
    }

    public void loadBattle(Deck deck, int playerHealth, int level, Mode mode, Enemy enemy) {
        this.deck = deck;
        this.playerHealth = playerHealth;
        this.level = level;
        this.mode = mode;
        this.currentEnemy = enemy;
        drawingPile = new ArrayList<>(deck.getDeck());
        hand.clear();
        discardPile.clear();
        rewards.clear();
        shield = 0;
        round = 0;
        state = GameState.BATTLE;
        reset();
        startRound();
    }

    public void loadReward(Deck deck, int playerHealth, int level, Mode mode, Enemy enemy, List<String> rewards) {
        this.deck = deck;
        this.playerHealth = playerHealth;
        this.level = level;
        this.mode = mode;
        this.currentEnemy = enemy;
        drawingPile = new ArrayList<>(deck.getDeck());
        hand.clear();
        discardPile.clear();
        this.rewards = new ArrayList<>(rewards);
        shield = 0;
        round = 0;
        state = GameState.REWARD;
        reset();
    }

    private void checkBattleStatus() {
        if (currentEnemy.getHealth() <= 0) {
            if (level >= WIN_LEVEL) {
                state = GameState.VICTORY;
            } else {
                rewardStatus();
            }
        }
    }

    private double battleStatusReward() {
        if (isGameWon()) {
            return VICTORY_REWARD;
        } else if (state == GameState.REWARD) {
            return BATTLE_CLEAR_REWARD + bossClearReward();
        } else if (isGameOver()) {
            return gameOverPenalty();
        } else {
            return 0.0;
        }
    }

    private double bossClearReward() {
        return level % 10 == 0 ? BOSS_CLEAR_REWARD : 0.0;
    }

    private double gameOverPenalty() {
        int missingLevels = Math.max(0, WIN_LEVEL - level);
        return GAME_OVER_BASE_PENALTY + missingLevels * MISSING_LEVEL_PENALTY;
    }
    
    private void rewardStatus() {
        heal();
        enterRewardState();
    }

    private void newBattle() {
        state = GameState.BATTLE;
        currentEnemy = createNewEnemy();
        shuffle();
        startRound();
    }

    private Enemy createNewEnemy() {
        int modeScaling = level / 10;
        if (level % 10 == 0) {
            return new BossEnemy((int) Math.floor(level * Math.pow(2, modeScaling)));
        } else if (Math.random() > 0.2) {
            return new NormalEnemy((int) Math.floor(level * Math.pow(1.25, modeScaling)));
        } else {
            return new EliteEnemy((int) Math.floor(level * Math.pow(1.5, modeScaling)));
        }
    }

    private void heal() {
        playerHealth += 1 + level / 5;
        if (playerHealth >= MAX_HEALTH) {
            playerHealth = MAX_HEALTH;
        }
    }

    private void enterRewardState() {
        state = GameState.REWARD;
        if (rewards.isEmpty()) {
            rewards = cardOptions();
        }
    }

    public ActionResult chooseRewardAt(int rewardIndex) {
        if (state != GameState.REWARD) {
            return illegalResult("cannot choose reward outside reward state");
        }
        if (rewardIndex < 0 || rewardIndex >= rewards.size()) {
            return illegalResult("reward index out of range");
        }

        String chosenOption = rewards.get(rewardIndex);
        int evaluationLevel = rewardEvaluationLevel();
        double qualityBefore = deckQuality(deck.getDeck(), evaluationLevel);
        int deckSizeBefore = deck.getDeck().size();
        double reward = 0.0;
        String message = "reward chosen";
        if (chosenOption.equals("Skip")) {
            message = "reward skipped dq=+0.00";
        } else if (chosenOption.equals("Remove")) {
            RemovalChoice removalChoice = bestRemovalChoice(evaluationLevel);
            Card removedCard = autoRemoveWorstCard(removalChoice);
            if (removalChoice != null) {
                reward = removeDeckQualityReward(
                        removalChoice.qualityAfter - qualityBefore,
                        deckSizeBefore,
                        evaluationLevel);
            }
            finishRewardAndStartNextBattle();
            if (removedCard == null) {
                return legalResult(0.0, "remove chosen; deck empty");
            }
            message = String.format(Locale.US, "removed %s:%s dq=%+.2f", removedCard.getCardType(),
                    removedCard.getName(), removalChoice.qualityAfter - qualityBefore);
            return legalResult(reward, message);
        } else {
            Card chosenCard = optionToCard(chosenOption);
            deck.addCard(chosenCard);
            double qualityAfter = deckQuality(deck.getDeck(), evaluationLevel);
            double qualityDelta = qualityAfter - qualityBefore;
            reward = deckQualityReward(qualityDelta);
            message = String.format(Locale.US, "picked %s:%s dq=%+.2f", chosenCard.getCardType(),
                    chosenCard.getName(), qualityDelta);
        }

        finishRewardAndStartNextBattle();
        return legalResult(reward, message);
    }

    private int rewardEvaluationLevel() {
        return Math.min(WIN_LEVEL, level + 1);
    }

    private Card autoRemoveWorstCard(RemovalChoice removalChoice) {
        List<Card> cards = deck.getDeck();
        if (cards.isEmpty() || removalChoice == null) {
            return null;
        }
        return cards.remove(removalChoice.index);
    }

    private RemovalChoice bestRemovalChoice(int evaluationLevel) {
        List<Card> cards = deck.getDeck();
        if (cards.isEmpty()) {
            return null;
        }

        int bestIndex = 0;
        double bestQuality = Double.NEGATIVE_INFINITY;
        for (int index = 0; index < cards.size(); index++) {
            List<Card> candidateDeck = new ArrayList<>(cards);
            candidateDeck.remove(index);
            double qualityAfter = deckQuality(candidateDeck, evaluationLevel);
            if (qualityAfter > bestQuality) {
                bestIndex = index;
                bestQuality = qualityAfter;
            }
        }
        return new RemovalChoice(bestIndex, bestQuality);
    }

    private double deckQualityReward(double qualityDelta) {
        double scaledReward = qualityDelta * DECK_QUALITY_REWARD_SCALE;
        return Math.max(-DECK_QUALITY_REWARD_LIMIT, Math.min(DECK_QUALITY_REWARD_LIMIT, scaledReward));
    }

    private double removeDeckQualityReward(double qualityDelta, int deckSizeBefore, int evaluationLevel) {
        double scaledReward = qualityDelta * REMOVE_DECK_QUALITY_REWARD_SCALE;
        double clippedReward = Math.max(
                -REMOVE_DECK_QUALITY_REWARD_LIMIT,
                Math.min(REMOVE_DECK_QUALITY_REWARD_LIMIT, scaledReward));
        int targetDeckSize = targetDeckSize(evaluationLevel);

        if (qualityDelta > 0.0 && deckSizeBefore <= targetDeckSize) {
            clippedReward = 0.0;
        }
        if (deckSizeBefore < targetDeckSize) {
            double missingCardPenalty = Math.min(
                    REMOVE_DECK_QUALITY_REWARD_LIMIT,
                    (targetDeckSize - deckSizeBefore) * UNDER_TARGET_REMOVE_PENALTY);
            clippedReward -= missingCardPenalty;
        }

        return Math.max(
                -REMOVE_DECK_QUALITY_REWARD_LIMIT,
                Math.min(REMOVE_DECK_QUALITY_REWARD_LIMIT, clippedReward));
    }

    private double deckQuality(List<Card> cards, int evaluationLevel) {
        int stage = stageIndex(evaluationLevel);
        DeckQualityStats stats = deckQualityStats(cards);
        double quality = 0.0;
        for (Card card : cards) {
            quality += cardQuality(card, stats, stage);
        }

        int bloat = Math.max(0, cards.size() - targetDeckSize(evaluationLevel));
        quality -= bloat * 0.12;
        if (stats.numberCount < 6) {
            quality -= (6 - stats.numberCount) * 0.75;
        }
        if (stats.symbolCount + stats.specialCount < 4) {
            quality -= (4 - stats.symbolCount - stats.specialCount) * 0.45;
        }
        return quality;
    }

    private DeckQualityStats deckQualityStats(List<Card> cards) {
        DeckQualityStats stats = new DeckQualityStats();
        for (Card card : cards) {
            if (card instanceof NumberCard) {
                int value = ((NumberCard) card).getValue();
                stats.numberCount += 1;
                if (value >= 40) {
                    stats.highNumberCount += 1;
                    stats.scalePower += 0.4;
                }
            } else if (card instanceof SymbolCard) {
                stats.symbolCount += 1;
                addSymbolStats(stats, ((SymbolCard) card).getSymbol());
            } else if (card instanceof SpecialCard) {
                stats.specialCount += 1;
                addSpecialStats(stats, card.getName());
            }
        }
        stats.controlPower += stats.drawAccess * 0.25;
        return stats;
    }

    private void addSymbolStats(DeckQualityStats stats, char symbol) {
        switch (symbol) {
            case '+':
                stats.drawAccess += 0.5;
                stats.scalePower += 0.5;
                break;
            case '-':
                stats.drawAccess += 1.0;
                stats.controlPower += 1.0;
                break;
            case '*':
                stats.scalePower += 1.0;
                break;
            case '/':
                stats.drawAccess += 1.5;
                stats.controlPower += 1.4;
                break;
            case '^':
                stats.drawAccess -= 0.4;
                stats.scalePower += 1.4;
                break;
            default:
                break;
        }
    }

    private void addSpecialStats(DeckQualityStats stats, String name) {
        if (name.equals("draw")) {
            stats.drawAccess += 1.5;
        } else if (name.equals("split")) {
            stats.drawAccess += 0.5;
            stats.controlPower += 1.2;
        } else if (name.equals("merge")) {
            stats.drawAccess += 0.5;
            stats.scalePower += 1.1;
        }
    }

    private double cardQuality(Card card, DeckQualityStats stats, int stage) {
        if (card instanceof NumberCard) {
            return numberQuality(((NumberCard) card).getValue(), stats, stage);
        } else if (card instanceof SymbolCard) {
            return profileQuality(symbolQualityProfile(((SymbolCard) card).getSymbol()), stats, stage);
        } else if (card instanceof SpecialCard) {
            return profileQuality(specialQualityProfile(card.getName()), stats, stage);
        }
        return 0.0;
    }

    private double numberQuality(int value, DeckQualityStats stats, int stage) {
        if (value == 0) {
            return -2.0;
        } else if (value == 1) {
            return -1.0;
        } else if (value <= 4) {
            return 0.5 + SMALL_NUMBER_STAGE_BONUS[0][stage];
        } else if (value <= 9) {
            return 1.2 + SMALL_NUMBER_STAGE_BONUS[0][stage];
        } else if (value <= 39) {
            return 2.0 + SMALL_NUMBER_STAGE_BONUS[1][stage];
        } else if (value <= 99) {
            return 3.0 + SMALL_NUMBER_STAGE_BONUS[2][stage];
        } else if (value <= 499) {
            return 3.3 + SMALL_NUMBER_STAGE_BONUS[2][stage] - oversizedNumberPenalty(stats, stage) * 0.5;
        }

        return 3.0 + SMALL_NUMBER_STAGE_BONUS[2][stage] - oversizedNumberPenalty(stats, stage);
    }

    private CardQualityProfile symbolQualityProfile(char symbol) {
        switch (symbol) {
            case '+':
                return new CardQualityProfile(0.6, 1.0, 0.5, 0.0, 0.0,
                        new double[] {0.1, 0.0, -0.1, -0.2, -0.3});
            case '-':
                return new CardQualityProfile(0.6, 2.0, 0.0, 1.3, 0.0,
                        new double[] {0.1, 0.3, 0.5, 0.6, 0.6});
            case '*':
                return new CardQualityProfile(0.9, 0.0, 2.4, 0.0, 0.4,
                        new double[] {-0.2, 0.4, 0.7, 1.0, 1.2});
            case '/':
                return new CardQualityProfile(0.7, 3.0, 0.0, 2.2, 0.0,
                        new double[] {0.0, 0.5, 0.8, 1.1, 1.2});
            case '^':
                return new CardQualityProfile(0.5, -0.8, 3.4, 0.0, 1.4,
                        new double[] {-0.6, 0.0, 0.4, 0.8, 1.1});
            default:
                return new CardQualityProfile(0.0, 0.0, 0.0, 0.0, 0.0,
                        new double[] {0.0, 0.0, 0.0, 0.0, 0.0});
        }
    }

    private CardQualityProfile specialQualityProfile(String name) {
        if (name.equals("draw")) {
            return new CardQualityProfile(1.2, 3.2, 0.0, 0.0, 0.0,
                    new double[] {0.0, 0.4, 0.6, 0.8, 0.9});
        } else if (name.equals("split")) {
            return new CardQualityProfile(1.0, 1.0, 0.0, 2.4, 0.0,
                    new double[] {0.0, 0.4, 0.7, 1.0, 1.1});
        } else if (name.equals("merge")) {
            return new CardQualityProfile(1.0, 1.0, 2.6, 0.0, 0.8,
                    new double[] {-0.2, 0.5, 0.8, 1.1, 1.2});
        }
        return new CardQualityProfile(0.0, 0.0, 0.0, 0.0, 0.0,
                new double[] {0.0, 0.0, 0.0, 0.0, 0.0});
    }

    private double profileQuality(CardQualityProfile profile, DeckQualityStats stats, int stage) {
        return profile.base
                + profile.stageBonus[stage]
                + profile.draw * DRAW_STAGE_WEIGHT[stage] * scarcityFactor(stats.drawAccess, DRAW_TARGET[stage])
                + profile.scale * SCALE_STAGE_WEIGHT[stage] * scarcityFactor(stats.scalePower, SCALE_TARGET[stage])
                + profile.control * CONTROL_STAGE_WEIGHT[stage]
                        * scarcityFactor(stats.controlPower, CONTROL_TARGET[stage])
                - profile.risk * RISK_STAGE_WEIGHT[stage] * oversizedNumberPenalty(stats, stage);
    }

    private double oversizedNumberPenalty(DeckQualityStats stats, int stage) {
        double pressure = Math.max(0.0, stats.scalePower - stats.controlPower) / RISK_TARGET[stage];
        return Math.min(4.0, pressure);
    }

    private double scarcityFactor(double count, double target) {
        if (target <= 0.0) {
            return 1.0;
        } else if (count <= target) {
            return 1.25 - 0.25 * count / target;
        }
        return Math.max(0.25, 1.0 - 0.25 * (count - target));
    }

    private int stageIndex(int evaluationLevel) {
        int normalizedLevel = Math.max(1, evaluationLevel);
        return Math.min(4, (normalizedLevel - 1) / 10);
    }

    private int targetDeckSize(int evaluationLevel) {
        return TARGET_DECK_SIZE[stageIndex(evaluationLevel)];
    }

    private void finishRewardAndStartNextBattle() {
        rewards.clear();
        level += 1;
        updateMode();
        drawingPile = new ArrayList<>(deck.getDeck());
        discardPile.clear();
        hand.clear();
        round = 0;
        currentEnemy = null;
        newBattle();
    }

    private List<String> cardOptions() {
        Random random = new Random();
        List<String> options = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            int cardType = random.nextInt(3);
            if (cardType == 0) {
                options.add(String.valueOf(random.nextInt(100)));
            } else if (cardType == 1) {
                char[] symbols = { '+', '-', '*', '/', '^' };
                options.add(String.valueOf(symbols[random.nextInt(symbols.length)]));
            } else {
                String[] specialNames = { "merge", "split", "draw" };
                options.add(specialNames[random.nextInt(specialNames.length)]);
            }
        }
        options.add("Skip");
        options.add("Remove");
        return options;
    }

    private Card optionToCard(String chosenOption) {
        Set<String> symbolSet = Set.of("+", "-", "*", "/", "^");

        if (chosenOption.matches("[0-9]{1,2}")) {
            return new NumberCard(Integer.parseInt(chosenOption));
        } else if (symbolSet.contains(chosenOption)) {
            return new SymbolCard(chosenOption.charAt(0));
        } else {
            return new SpecialCard(chosenOption);
        }
    }

    private void updateMode() {
        switch (level) {
            case 11:
                mode = Mode.HARD;
                break;
            case 21:
                mode = Mode.MASTER;
                break;
            case 31:
                mode = Mode.HELL;
                break;
            case 41:
                mode = Mode.INFERNO;
                break;
            default:
                break;
        }
    }

    private void startRound() {
        int handSize = Math.min(INITIAL_HAND_SIZE + (level - 1) / 10, 9);
        round += 1;
        shield = 0;
        drawCards(handSize);
    }


    public ActionResult playCardAt(int handIndex) {
        if (state != GameState.BATTLE) {
            return illegalResult("cannot play card outside battle state");
        }
        if (handIndex < 0 || handIndex >= hand.size()) {
            return illegalResult("hand index out of range");
        }

        Card chosenCard = hand.get(handIndex);
        if (!playCard(chosenCard)) {
            return illegalResult("card cannot be played now");
        }

        return legalResult(STEP_PENALTY, "card played");
    }

    public boolean canPlayCardAt(int handIndex) {
        if (state != GameState.BATTLE) {
            return false;
        }
        if (handIndex < 0 || handIndex >= hand.size()) {
            return false;
        }
        return canPlayCard(hand.get(handIndex));
    }

    public ActionResult endRound() {
        if (state != GameState.BATTLE) {
            return illegalResult("cannot end round outside battle state");
        }

        reset();
        discardPile.addAll(hand);
        hand.clear();

        int healthBefore = playerHealth;
        int incomingDamage = currentEnemy.getHealth() > 0 ? currentEnemy.getDamage() : 0;
        if (incomingDamage > 0) {
            enemyAttack();
        }

        checkBattleStatus();

        double reward = STEP_PENALTY;
        int damageTaken = Math.max(0, healthBefore - playerHealth);
        reward -= damageTaken * DAMAGE_TAKEN_PENALTY_SCALE;
        reward += battleStatusReward();

        if (state == GameState.BATTLE && !isGameOver() && currentEnemy.getHealth() > 0) {
            startRound();
        }

        return legalResult(reward, "round ended");
    }

    public void drawCards(int numOfCards) {
        for (int i = 0; i < numOfCards; i++) {
            if (drawingPile.isEmpty()) {
                reshuffle();
            }
            if (drawingPile.isEmpty()) {
                return;
            }

            Card drawnCard = drawingPile.remove(0);
            if (hand.size() < MAX_HAND_SIZE) {
                hand.add(drawnCard);
            } else {
                discardPile.add(drawnCard);
            }
        }
    }

    private void discardCards(int numOfCards) {
        for (int i = 0; i < numOfCards; i++) {
            if (!hand.isEmpty()) {
                Collections.shuffle(hand);
                discardPile.add(hand.remove(0));
            }
        }
    }

    private void reshuffle() {
        drawingPile.addAll(discardPile);
        discardPile.clear();
        shuffle();
    }

    private void shuffle() {
        Collections.shuffle(drawingPile);
    }

    public ActionResult cancel() {
        if (state != GameState.BATTLE) {
            return illegalResult("cannot cancel outside battle state");
        }
        if (lastCard == null) {
            return illegalResult("no pending card action to cancel");
        }

        if (operation == '\0') {
            returnToHand(lastCard);
            discardPile.remove(lastCard);
        } else {
            returnToHand(lastCard);
            NumberCard calCard = new NumberCard(calcValue);
            returnToHand(calCard);
            discardPile.removeIf(card -> calCard.cardEquals(card));
        }

        reset();
        return legalResult(STEP_PENALTY, "pending action canceled");
    }

    private boolean playCard(Card card) {
        boolean played;
        if (card instanceof NumberCard) {
            played = playNumberCard((NumberCard) card);
        } else if (card instanceof SymbolCard) {
            played = playSymbolCard((SymbolCard) card);
        } else if (card instanceof SpecialCard) {
            played = playSpecialCard((SpecialCard) card);
        } else {
            played = false;
        }

        if (played) {
            hand.remove(card);
            discardPile.add(card);
        }
        return played;
    }

    private boolean canPlayCard(Card card) {
        if (card instanceof NumberCard) {
            return canPlayNumberCard();
        } else if (card instanceof SymbolCard) {
            return canPlaySymbolCard();
        } else if (card instanceof SpecialCard) {
            return canPlaySpecialCard((SpecialCard) card);
        } else {
            return false;
        }
    }

    private boolean canPlayNumberCard() {
        if (lastCard == null && operation == '\0') {
            return true;
        }
        return lastCard != null && !lastIsNumCard();
    }

    private boolean canPlaySymbolCard() {
        return lastIsNumCard() && hasNumberCard();
    }

    private boolean canPlaySpecialCard(SpecialCard card) {
        if (card.getName().equals("merge")) {
            return lastIsNumCard() && hasNumberCard();
        } else if (card.getName().equals("split")) {
            return lastIsNumCard() && calcValue != Integer.MIN_VALUE && canSplitCard();
        } else if (card.getName().equals("draw")) {
            return lastIsNumCard() && calcValue != Integer.MIN_VALUE;
        }
        return false;
    }

    private boolean playNumberCard(NumberCard card) {
        if (lastCard == null && operation == '\0') {
            calcValue = card.getValue();
            lastCard = card;
            return true;
        } else if (lastIsNumCard()) {
            return false;
        } else {
            calculateValue(card);
            returnToHand(new NumberCard(calcValue));
            reset();
            return true;
        }
    }

    private boolean playSymbolCard(SymbolCard card) {
        if (!lastIsNumCard() || !hasNumberCard()) {
            return false;
        }
        operation = card.getSymbol();
        lastCard = card;
        return true;
    }

    private boolean playSpecialCard(SpecialCard card) {
        if (card.getName().equals("merge")) {
            return mergeCard(card);
        } else if (card.getName().equals("split")) {
            return splitCard();
        } else if (card.getName().equals("draw")) {
            return drawCard();
        }
        return false;
    }

    private void returnToHand(Card card) {
        hand.add(card);
    }


    private void reset() {
        lastCard = null;
        calcValue = Integer.MIN_VALUE;
        operation = '\0';
    }

    private void calculateValue(NumberCard card) {
        switch (operation) {
            case '+':
                addCardValue(card);
                break;
            case '-':
                subtractCardValue(card);
                break;
            case '*':
                multiplyCardValue(card);
                break;
            case '/':
                divideCardValue(card);
                break;
            case '^':
                powerCardValue(card);
                break;
            case 'm':
                mergeCardValue(card);
                break;
            default:
                break;
        }
    }

    private void addCardValue(NumberCard card) {
        long temporaryValue = (long) (calcValue + card.getValue());
        calcValue = (int) Math.min(temporaryValue, Integer.MAX_VALUE);
        drawCards(1);
    }

    private void subtractCardValue(NumberCard card) {
        calcValue = Math.max(calcValue - card.getValue(), 0);
        drawCards(2);
    }

    private void multiplyCardValue(NumberCard card) {
        long temporaryValue = (long) calcValue * card.getValue();
        calcValue = (int) Math.min(temporaryValue, Integer.MAX_VALUE);
    }

    private void divideCardValue(NumberCard card) {
        if (card.getValue() != 0) {
            calcValue /= card.getValue();
        }
        drawCards(3);
    }

    private void powerCardValue(NumberCard card) {
        long temporaryValue = (long) Math.pow(calcValue, card.getValue());
        calcValue = (int) Math.min(temporaryValue, Integer.MAX_VALUE);
        discardCards(1);
    }

    private boolean mergeCard(SpecialCard card) {
        if (!lastIsNumCard() || !hasNumberCard()) {
            return false;
        }
        operation = 'm';
        lastCard = card;
        drawCards(1);
        return true;
    }

    private void mergeCardValue(NumberCard card) {
        String merged = String.valueOf(calcValue) + card.getValue();
        calcValue = parseMergedCardValue(merged);
    }

    private int parseMergedCardValue(String merged) {
        String maxInteger = String.valueOf(Integer.MAX_VALUE);
        if (merged.length() > maxInteger.length()
                || (merged.length() == maxInteger.length() && merged.compareTo(maxInteger) > 0)) {
            return Integer.MAX_VALUE;
        }

        return Integer.parseInt(merged);
    }

    private boolean splitCard() {
        if (!lastIsNumCard() || calcValue == Integer.MIN_VALUE || !canSplitCard()) {
            return false;
        }

        NumberCard card = (NumberCard) lastCard;
        String numString = String.valueOf(card.getValue());
        int part1 = Character.getNumericValue(numString.charAt(0));
        int part2 = Integer.parseInt(numString.substring(1));
        returnToHand(new NumberCard(part1));
        returnToHand(new NumberCard(part2));
        discardPile.remove(lastCard);
        drawCards(1);
        reset();
        return true;
    }

    private boolean drawCard() {
        if (!lastIsNumCard() || calcValue == Integer.MIN_VALUE) {
            return false;
        }

        NumberCard card = (NumberCard) lastCard;
        int toDraw = Math.min(card.getValue(), 5);
        drawCards(toDraw);
        reset();
        return true;
    }
    
    private boolean canSplitCard() {
        NumberCard card = (NumberCard) lastCard;
        return String.valueOf(card.getValue()).length() > 1;
    }

    private boolean lastIsNumCard() {
        return lastCard instanceof NumberCard;
    }

    private void enemyAttack() {
        int damage = currentEnemy.getDamage();
        int unshieldedDamage = damage - shield;
        if (damage <= shield) {
            shield -= damage;
        } else {
            playerHealth -= unshieldedDamage;
            shield = 0;
        }
    }

    private int enemyTakeDamage(int damage) {
        int healthBefore = currentEnemy.getHealth();
        if (attackDamageWouldBeIgnored(damage)) {
            return 0;
        }
        currentEnemy.setHealth(currentEnemy.getHealth() - damage);
        if (currentEnemy.getHealth() < 0) {
            currentEnemy.setHealth(0);
        }
        return Math.max(0, healthBefore - currentEnemy.getHealth());
    }

    private double enemyHealthProgressReward(int damageDealt) {
        if (currentEnemy == null || currentEnemy.getMaxHealth() <= 0) {
            return 0.0;
        }
        return ((double) damageDealt / currentEnemy.getMaxHealth()) * enemyHealthProgressRewardScale();
    }

    private double enemyHealthProgressRewardScale() {
        if (currentEnemy != null && currentEnemy.getEnemyType() == EnemyType.BOSS) {
            return BOSS_HEALTH_PROGRESS_REWARD_SCALE;
        }
        return ENEMY_HEALTH_PROGRESS_REWARD_SCALE;
    }

    private boolean attackDamageWouldBeIgnored(int damage) {
        if (currentEnemy == null) {
            return true;
        }
        return damage >= ignoredAttackDamageThreshold();
    }

    private double ignoredAttackDamageThreshold() {
        int modeLimit = level / 10 + 1;
        return 5 + Math.pow(5, modeLimit) + currentEnemy.getHealth();
    }

    private int effectiveShield() {
        if (currentEnemy == null || currentEnemy.getHealth() <= 0) {
            return 0;
        }
        return Math.min(shield, currentEnemy.getDamage());
    }


    private boolean canAct() {
        return lastCard == null && hasNumberCard();
    }


    private boolean hasNumberCard() {
        for (Card card : hand) {
            if (card instanceof NumberCard) {
                return true;
            }
        }
        return false;
    }


    private boolean validNumberCardIndex(int handIndex) {
        return handIndex >= 0 && handIndex < hand.size() && hand.get(handIndex) instanceof NumberCard;
    }

    public boolean canAttackWithCardAt(int handIndex) {
        if (state != GameState.BATTLE || !canAct() || !validNumberCardIndex(handIndex)) {
            return false;
        }

        NumberCard attackCard = (NumberCard) hand.get(handIndex);
        return !attackDamageWouldBeIgnored(attackCard.getValue());
    }

    public ActionResult attackWithCardAt(int handIndex) {
        if (state != GameState.BATTLE) {
            return illegalResult("cannot attack outside battle state");
        }
        if (!canAct()) {
            return illegalResult("cannot attack during pending card action or without number cards");
        }
        if (!validNumberCardIndex(handIndex)) {
            return illegalResult("invalid attack card index");
        }
        if (!canAttackWithCardAt(handIndex)) {
            return illegalResult("attack damage is too large");
        }

        NumberCard attackCard = (NumberCard) hand.get(handIndex);
        int damageDealt = enemyTakeDamage(attackCard.getValue());
        hand.remove(attackCard);
        discardPile.add(attackCard);
        checkBattleStatus();

        double reward = STEP_PENALTY + enemyHealthProgressReward(damageDealt) + battleStatusReward();
        return legalResult(reward, "attack completed");
    }

    public ActionResult defendWithCardAt(int handIndex) {
        if (state != GameState.BATTLE) {
            return illegalResult("cannot defend outside battle state");
        }
        if (!canAct()) {
            return illegalResult("cannot defend during pending card action or without number cards");
        }
        if (!validNumberCardIndex(handIndex)) {
            return illegalResult("invalid defend card index");
        }

        NumberCard defendCard = (NumberCard) hand.get(handIndex);
        int effectiveShieldBefore = effectiveShield();
        int shieldedAmount = (int) Math.round(Math.log(Math.max(2, defendCard.getValue())));
        shield = Math.max(1, Math.min(shield + shieldedAmount, 999));
        int effectiveShieldGain = Math.max(0, effectiveShield() - effectiveShieldBefore);
        hand.remove(defendCard);
        discardPile.add(defendCard);

        double reward = STEP_PENALTY + effectiveShieldGain * EFFECTIVE_DEFEND_REWARD_SCALE;
        return legalResult(reward, "defend completed");
    }

    public ActionResult removeCardAt(int deckIndex) {
        if (deckIndex < 0 || deckIndex >= deck.getDeck().size()) {
            return illegalResult("deck index out of range");
        }
        deck.getDeck().remove(deckIndex);
        return legalResult(0.0, "card removed");
    }

    public Observation getObservation() {
        int enemyHealth = currentEnemy == null ? 0 : currentEnemy.getHealth();
        int enemyMaxHealth = currentEnemy == null ? 0 : currentEnemy.getMaxHealth();
        int enemyDamage = currentEnemy == null ? 0 : currentEnemy.getDamage();
        String enemyType = currentEnemy == null ? "NONE" : currentEnemy.getEnemyType().name();

        return new Observation(state, mode, level, playerHealth, shield, round, enemyHealth, enemyMaxHealth,
                enemyDamage, enemyType, drawingPile.size(), discardPile.size(), deck.getSize(), calcValue, operation,
                cardNames(deck.getDeck()), cardNames(hand), rewards);
    }

    private List<String> cardNames(List<Card> cards) {
        List<String> names = new ArrayList<>();
        for (Card card : cards) {
            names.add(card.getCardType() + ":" + card.getName());
        }
        return names;
    }

    private ActionResult illegalResult(String message) {
        return new ActionResult(false, ILLEGAL_ACTION_PENALTY, isGameOver() || isGameWon(), message);
    }

    private ActionResult legalResult(double reward, String message) {
        return new ActionResult(true, reward, isGameOver() || isGameWon(), message);
    }
}
