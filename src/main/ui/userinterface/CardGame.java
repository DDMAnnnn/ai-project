package ui.userinterface;

import model.Deck;
import model.cards.*;
import model.enemies.*;
import persistence.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

import ui.Mode;
import ui.GameState;

//Represents the cardGame, edited for the user interface.
public class CardGame {
    private Deck deck;
    private List<Card> drawingPile;
    private List<Card> hand;
    private List<Card> discardPile;
    private Enemy currentEnemy;
    private int playerHealth;
    private static final int INITIAL_HEALTH = 100;
    private static final int INITIAL_MAX_HEALTH = 150;
    private int maxHealth;
    private int level;
    private static final int INITIAL_HAND_SIZE = 5;
    private Card lastCard;
    private int calcValue;
    private char operation;
    private int shield;
    private Mode mode;
    private GameState state;
    private int round;
    private List<String> rewards;
    private JsonWriter jsonWriterGame;
    private JsonReaderGame jsonReaderGame;
    private JsonWriter jsonWriterReward;
    private JsonReaderReward jsonReaderReward;
    private JsonWriter jsonWriterState;
    private JsonReaderState jsonReaderState;
    private static final String JSON_STORE_GAME = "./data/gameData.json";
    private static final String JSON_STORE_REWARD = "./data/rewardData.json";
    private static final String JSON_STORE_STATE = "./data/stateData.json";
    private CardGameGUI gui;

    // ------------------------------------------------------------------
    // Constructor
    // ------------------------------------------------------------------

    // MODIFIES: this
    // EFFECTS: construct a card game.
    public CardGame(boolean autostart, CardGameGUI gui) throws IOException {
        this.gui = gui;
        deck = new Deck(true);
        drawingPile = new ArrayList<>(deck.getDeck());
        hand = new ArrayList<>();
        discardPile = new ArrayList<>();
        rewards = new ArrayList<>();
        playerHealth = INITIAL_HEALTH;
        maxHealth = INITIAL_MAX_HEALTH;
        shield = 0;
        reset();
        level = 1;
        mode = Mode.NORMAL;
        state = GameState.INITIAL;
        jsonWriterGame = new JsonWriter(JSON_STORE_GAME);
        jsonReaderGame = new JsonReaderGame(JSON_STORE_GAME);
        jsonWriterReward = new JsonWriter(JSON_STORE_REWARD);
        jsonReaderReward = new JsonReaderReward(JSON_STORE_REWARD);
        jsonWriterState = new JsonWriter(JSON_STORE_STATE);
        jsonReaderState = new JsonReaderState(JSON_STORE_STATE);

    }

    // ------------------------------------------------------------------
    // Getters and Setters
    // ------------------------------------------------------------------

    // ------------------------------------------------------------------
    // getters
    // ------------------------------------------------------------------

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

    public int getMaxHealth() {
        return maxHealth;
    }

    // ------------------------------------------------------------------
    // Setters
    // ------------------------------------------------------------------

    public void setDeck(Deck deck) {
        this.deck = deck;
    }

    public void setDrawingPile(List<Card> drawingPile) {
        this.drawingPile = drawingPile;
    }

    public void setHand(List<Card> hand) {
        this.hand = hand;
    }

    public void setDiscardPile(List<Card> discardPile) {
        this.discardPile = discardPile;
    }

    public void setCurrentEnemy(Enemy currentEnemy) {
        this.currentEnemy = currentEnemy;
    }

    public void setPlayerHealth(int playerHealth) {
        this.playerHealth = playerHealth;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public void setLastCard(Card lastPlayedCard) {
        this.lastCard = lastPlayedCard;
    }

    public void setCalcValue(int calculationValue) {
        this.calcValue = calculationValue;
    }

    public void setOperation(char currentOperation) {
        this.operation = currentOperation;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public void setRound(int round) {
        this.round = round;
    }

    public void setShield(int shield) {
        this.shield = shield;
    }

    public void setState(GameState state) {
        this.state = state;
    }

    public void setMaxHealth(int maxHealth) {
        this.maxHealth = maxHealth;
    }

    // ------------------------------------------------------------------
    // Game flow: battle -> defeat enemy -> get reward
    // ------------------------------------------------------------------

    // MODIFIES: this
    // EFFECTS: start the game.
    public void start() {
        newBattle();
    }

    // MODIFIES: this
    // EFFECTS: load the game with data.
    public void load() {
        loadGameState();
        loadGame();
        newBattle();
    }

    // ------------------------------------------------------------------
    // Single Battle Handler: battle, rounds, rewards
    // ------------------------------------------------------------------

    // MODIFIES: this
    // EFFECTS: handle battle rounds. if enemy defeated, get reward and start a new
    // battle.
    private void battle() {
        shuffle();
        startRound();
        gui.updateGameStatus();
        
    }

    // MODIFIES: this
    // EFFECTS: handle rewards and reset status after a battle is finished.
    private void rewardStatus() {
        heal();
        reward();
        rewards.clear();
        level += 1;
        mode();
        drawingPile = new ArrayList<>(deck.getDeck());
        discardPile.clear();
        hand.clear();
        round = 0;
        currentEnemy = null;
        newBattle();
    }

    // MODIFIES: this
    // EFFECTS: start a new battle.
    private void newBattle() {
        setState(GameState.BATTLE);
        currentEnemy = createNewEnemy();
        gui.updateGameStatus();
        battle();
        autoSave();
    }

    // MODIFIES: gui
    // EFFECTS: tells user the current enemy at the start of battle.
    private void showEnemy() {

        if (round == 1) {
            if (level % 10 == 0) {
                gui.showMessage(currentEnemy.getName());
            } else {
                gui.showMessage("New enemy Appeared: " + currentEnemy.getName() + ".");
            }
        }

        gui.updateGameStatus();
    }

    // REQUIRES: Boss enemy only appears every 10 level.
    // EFFECTS: create a new enemy.
    private Enemy createNewEnemy() {
        if (currentEnemy == null) {
            int mode = level / 50;
            if (level % 10 == 0) {
                return new BossEnemy((int) Math.floor(level * (Math.pow(2, mode))));
            } else if (Math.random() > 0.2) {
                return new NormalEnemy((int) Math.floor(level * (Math.pow(1.25, mode))));
            } else {
                return new EliteEnemy((int) Math.floor(level * (Math.pow(1.5, mode))));
            }
        } else {
            return currentEnemy;
        }
    }

    // MODIFIES: this
    // EFFECTS: heal the player, increase max health every 10 level.
    private void heal() {
        playerHealth += (1 + level / 5);
        if (level % 10 == 0) {
            maxHealth += 50;
        }
        if (playerHealth >= maxHealth) {
            playerHealth = maxHealth;
        }
    }

    // MODIFIES: this
    // EFFECTS: get reward after enemy is defeated. Reward includes 3 random cards,
    // "skip" and "remove".
    // "skip" skip the reward, "remove" remove a card from deck.
    private void reward() {
        setState(GameState.REWARD);
        if (rewards.isEmpty()) {
            rewards = cardOptions();
        }
        autoSave();
        gui.showRewardOptions(rewards);
    }

    // EFFECTS: generate 3 random cards, skip, remove in string as reward.
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
            } else if (cardType == 2) {
                String[] specialNames = { "merge", "split", "draw" };
                options.add(specialNames[random.nextInt(specialNames.length)]);
            }
        }
        options.add("Skip");
        options.add("Remove");
        return options;
    }

    // EFFECTS: convert the string in reward to card in game.
    private Card optionToCard(String chosenOption) {
        Set<String> symbolSet = Set.of("+", "-", "*", "/", "^");

        if (chosenOption.matches("[0-9]{1,2}")) {
            int number = Integer.parseInt(chosenOption);
            return new NumberCard(number);
        } else if (symbolSet.contains(chosenOption)) {
            char symbol = chosenOption.charAt(0);
            return new SymbolCard(symbol);
        } else {
            return new SpecialCard(chosenOption);
        }
    }

    // MODIFIES: this
    // EFFECTS: Handles the reward choice made by the player.
    public void handleReward(String chosenOption, List<String> options) {
        if (options.contains(chosenOption)) {
            if (chosenOption.equals("Skip")) {
                gui.showMessage("You chose to skip.");
            } else if (chosenOption.equals("Remove")) {
                removeCard();
                gui.showMessage("Card removed!");
            } else {
                deck.addCard(optionToCard(chosenOption));
                gui.showMessage("Added card: " + chosenOption + " to your deck.");
            }
            rewards.clear();

            // mode();
            // drawingPile = new ArrayList<>(deck.getDeck());
            // discardPile.clear();
            // round = 0;
            // currentEnemy = null;
            // newBattle();
        } else {
            gui.showMessage("Invalid choice, please retry.");
            reward();
        }
    }

    // REQUIRES: the deck is not empty.
    // MODIFIES: this
    // EFFECTS: remove a card from the deck.
    private void removeCard() {
        List<Card> deckCards = deck.getDeck();
        String[] cardOptions = new String[deckCards.size()];
        for (int i = 0; i < deckCards.size(); i++) {
            cardOptions[i] = (i + 1) + ". " + deckCards.get(i).getName();
        }
        String chosenCard = gui.promptChoice("Choose a card to remove from your deck:", cardOptions);
        if (chosenCard != null) {
            try {
                int removeChoice = Integer.parseInt(chosenCard.split("\\.")[0]) - 1;
                if (removeChoice >= 0 && removeChoice < deckCards.size()) {
                    Card removedCard = deckCards.remove(removeChoice);
                    gui.showMessage("Removed card: " + removedCard.getName());
                } else {
                    gui.showMessage("Invalid choice. No card removed.");
                }
            } catch (NumberFormatException e) {
                gui.showMessage("Invalid input. No card removed.");
            }
        } else {
            gui.showMessage("No card selected. No card removed.");
        }
    }

    // MODIFIES: this
    // EFFECTS: change modes to at certain level.
    private void mode() {
        switch (level) {
            case 51:
                gui.showMessage("Congrats! Welcome to hard mode!");
                setMode(Mode.HARD);
                break;

            case 101:
                gui.showMessage("Welcome to master mode! Enjoy!");
                setMode(Mode.MASTER);
                break;
            case 151:
                gui.showMessage("Welcome to hell! Be prepared!");
                setMode(Mode.HELL);
                break;
            case 201:
                gui.showMessage("Inferno mode! Suffer!");
                setMode(Mode.INFERNO);
                break;
            default:
                break;
        }
    }

    // ------------------------------------------------------------------
    // Single Round Handler: start a round, act, end round
    // ------------------------------------------------------------------

    // MODIFIES: this
    // EFFECTS: handle a single round. Reset defense, draw cards, take actions and
    // endround.
    private void startRound() {
        int handSize = Math.min(INITIAL_HAND_SIZE + (level - 1) / 50, 9);
        round += 1;
        shield = 0;
        drawCards(handSize);
        gui.updateGameStatus();
        showEnemy();
        

        
    }

    // MODIFIES: this
    // EFFECTS: end the round, discard hand, set all status to default, and enemy
    // attack.
    public void endRound() {
        calcValue = Integer.MIN_VALUE;
        reset();
        discardPile.addAll(hand);
        hand.clear();
        if (currentEnemy.getHealth() > 0) {
            enemyAttack();
            checkBattleStatus();
            if (playerHealth > 0) {
                startRound();
            }
        } else {
            checkBattleStatus();
        }
    }

    // MODIFIES: this
    // EFFECTS: draw card from drawing pile to hand, if drawing pile is empty,
    // reshuffle discard pile into drawing pile and continue draw card.
    public void drawCards(int numOfCards) {
        for (int i = 0; i < numOfCards; i++) {
            if (!drawingPile.isEmpty()) {
                Card drawnCard = drawingPile.remove(0);
                if (hand.size() <= 10) {
                    hand.add(drawnCard);
                } else {
                    discardPile.add(drawnCard);
                }
            } else {
                reshuffle();
                if (!drawingPile.isEmpty()) {
                    Card drawnCard = drawingPile.remove(0);
                    if (hand.size() < 10) {
                        hand.add(drawnCard);
                    } else {
                        discardPile.add(drawnCard);
                    }
                }
            }
        }
    }

    // MODIFIES: this
    // EFFECTS: discard cards from hand randomly.
    private void discardCards(int numOfCards) {
        for (int i = 0; i < numOfCards; i++) {
            if (!hand.isEmpty()) {
                Collections.shuffle(hand);
                discardPile.add(hand.remove(0));
            } else {
                gui.showMessage("No more cards to discard!");
            }
        }
    }

    // MODIFIES: this
    // REQUIRES: drawing pile is empty.
    // EFFECTS:reshuffle discard pile into drawing pile and shuffle drawing pile.
    private void reshuffle() {
        drawingPile.addAll(discardPile);
        discardPile.clear();
        shuffle();
    }

    // MODIFIES: this
    // EFFECTS: shuffle the drawing pile.
    private void shuffle() {
        Collections.shuffle(drawingPile);
    }

    // MODIFIES: this
    // EFFECTS: cancel the previous action.
    public void cancel() {
        if (lastCard != null) {
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
            gui.showMessage("Action canceled!");
            gui.updateGameStatus();
        } else {
            gui.showMessage("No card played previously.");
        }
    }

    // ------------------------------------------------------------------
    // Card Handler: play different types of card
    // ------------------------------------------------------------------

    // MODIFIES: this
    // EFFECTS: play a chosen card.
    public void playCard(Card card) {
        if (card instanceof NumberCard) {
            playNumberCard((NumberCard) card);
        } else if (card instanceof SymbolCard) {
            playSymbolCard((SymbolCard) card);
        } else if (card instanceof SpecialCard) {
            playSpecialCard((SpecialCard) card);
        } else {
            gui.showMessage("Unknown card type.");
        }
        hand.remove(card);
        discardPile.add(card);
        gui.updateGameStatus();
    }

    // REQUIRES: 2 number card can't be played consecutively, card value <
    // Integer.MAX_VALUE.
    // MODIFIES: this
    // EFFECTS: play a number card.
    private void playNumberCard(NumberCard card) {
        if (lastCard == null && operation == '\0') {
            calcValue = card.getValue();
            lastCard = card;
        } else if (lastIsNumCard()) {
            gui.showMessage("Two NumberCards cannot be played consecutively.");
            returnToHand(card);
        } else {
            calculateValue(card);
            returnToHand(new NumberCard(calcValue));
            reset();
        }
    }

    // REQUIRES: a number card must be played right before and after a symbol card.
    // MODIFIES: this
    // EFFECTS: play a symbol card.
    private void playSymbolCard(SymbolCard card) {
        if (hasNumberCard()) {
            if (!lastIsNumCard()) {
                gui.showMessage("A SymbolCard can only be played after a NumberCard.");
                returnToHand(card);
                return;
            }
            operation = card.getSymbol();
            lastCard = card;
        } else {
            gui.showMessage("You don't have enough NumberCards!");
        }
    }

    // REQUIRES: different for "merge", "split" and "draw"
    // merge: number card before and after;
    // split: number card before;
    // draw: number card before, can't draw more than 5 card at one time.
    // MODIFIES: this
    // EFFECTS: play a special card.
    private void playSpecialCard(SpecialCard card) {
        if (card.getName().equals("merge")) {
            mergeCard(card);
        } else if (card.getName().equals("split")) {
            splitCard();
        } else if (card.getName().equals("draw")) {
            drawCard();
        } else {
            gui.showMessage("Unknown special card action.");
        }
    }

    // MODIFIES: this
    // return the result of playing card back to hand.
    private void returnToHand(Card card) {
        hand.add(card);
    }

    // MODIFIES: this
    // EFFECTS: reset the storage status to default.
    private void reset() {
        lastCard = null;
        calcValue = Integer.MIN_VALUE;
        operation = '\0';
    }

    // ------------------------------------------------------------------
    // Calculation Handler: handle operations with symbol card
    // ------------------------------------------------------------------

    // MODIFIES: this
    // EFFECTS:calculate value based on the symbolcard played.
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
                gui.showMessage("Unknown operation, please retry.");
                break;
        }
    }

    // MODIFIES: this
    // EFFECTS:calculate value if the card played is +.
    private void addCardValue(NumberCard card) {
        long temporaryValue = (long) (calcValue + card.getValue());
        calcValue = (int) Math.min(temporaryValue, Integer.MAX_VALUE);
        drawCards(1);
    }

    // MODIFIES: this
    // EFFECTS:calculate value if the card played is -.
    private void subtractCardValue(NumberCard card) {
        calcValue = Math.max(calcValue - card.getValue(), 0);
        drawCards(2);
    }

    // MODIFIES: this
    // EFFECTS:calculate value if the card played is *.
    private void multiplyCardValue(NumberCard card) {
        long temporaryValue = (long) calcValue * card.getValue();
        calcValue = (int) Math.min(temporaryValue, Integer.MAX_VALUE);
    }

    // MODIFIES: this
    // EFFECTS:calculate value if the card played is /.
    private void divideCardValue(NumberCard card) {
        if (card.getValue() != 0) {
            calcValue /= card.getValue();
        } else {
            gui.showMessage("Cannot divide by zero.");
        }
        drawCards(3);
    }

    // MODIFIES: this
    // EFFECTS:calculate value if the card played is ^.
    private void powerCardValue(NumberCard card) {
        long temporaryValue = (long) Math.pow(calcValue, card.getValue());
        calcValue = (int) Math.min(temporaryValue, Integer.MAX_VALUE);
        discardCards(1);
    }

    // ------------------------------------------------------------------
    // Special Card Handler: handle operations with "merge", "split", and "draw"
    // ------------------------------------------------------------------

    // REQUIRES: a number card must be played right before and after a merge card.
    // MODIFIES: this
    // EFFECTS: play a merge card which merge 2 number cards into 1.
    private void mergeCard(SpecialCard card) {
        SymbolCard specSymCard = new SymbolCard('m');
        if (hasNumberCard()) {
            if (!lastIsNumCard()) {
                gui.showMessage("A Merge Card can only be played after a NumberCard.");
                returnToHand(card);
                return;
            }
            operation = specSymCard.getSymbol();
            lastCard = card;
            drawCards(1);
        } else {
            gui.showMessage("You don't have enough NumberCards!");
        }
    }

    // EEFECTS: merge 2 cards.
    private void mergeCardValue(NumberCard card) {
        String merged = String.valueOf(calcValue) + String.valueOf(card.getValue());
        calcValue = Integer.parseInt(merged);
    }

    // REQUIRES: the number card played before must be at least 2 digits.
    // MODIFIES: this
    // EFFECTS: play a split card which split a number card into 2.
    private void splitCard() {
        if (lastIsNumCard() && calcValue != Integer.MIN_VALUE) {
            NumberCard card = (NumberCard) lastCard;
            String numString = String.valueOf(card.getValue());
            if (canSplitCard()) {
                int part1 = Character.getNumericValue(numString.charAt(0));
                int part2 = Integer.parseInt(numString.substring(1));
                returnToHand(new NumberCard(part1));
                returnToHand(new NumberCard(part2));
                hand.remove(card);
                drawCards(1);
                reset();
            } else {
                gui.showMessage("Cannot split a single-digit number.");
            }
        } else {
            handleSpecialCardError("split", "Split requires a number card to be played before.");
        }
    }

    // REQUIRES: no more than 5 card drawn one time.
    // MODIFIES: this
    // EFFECTS: draw card based on number played before, maximum 5.
    private void drawCard() {
        if (lastIsNumCard() && calcValue != Integer.MIN_VALUE) {
            NumberCard card = (NumberCard) lastCard;
            int toDraw = Math.min(card.getValue(), 5);
            drawCards(toDraw);
            reset();
        } else {
            handleSpecialCardError("draw", "Draw requires a NumberCard to be played before.");
        }
    }

    // EFFECTS: check whether the previous card has at leat 2 digits.
    private boolean canSplitCard() {
        NumberCard card = (NumberCard) lastCard;
        String numString = String.valueOf(card.getValue());
        return numString.length() > 1;
    }

    // EFFECTS: check whether last card played is a NumberCard.
    private boolean lastIsNumCard() {
        return lastCard instanceof NumberCard;
    }

    // EFFECTS: abstract function that handle error for special cards.
    private void handleSpecialCardError(String cardType, String errorMessage) {
        gui.showMessage(errorMessage);
        SpecialCard specialCard = new SpecialCard(cardType);
        returnToHand(specialCard);
        discardPile.removeIf(card -> specialCard.cardEquals(card));
        reset();
    }

    // ------------------------------------------------------------------
    // Attack & Defend Handler
    // ------------------------------------------------------------------

    // MODIFIES: this
    // EFFECTS: let enemy attack, player lose HP or shield.
    private void enemyAttack() {
        int damage = currentEnemy.getDamage();
        int unshieldedDamage = damage - shield;
        if (damage <= shield) {
            shield -= damage;
            gui.showMessage("Damage blocked!");
        } else {
            playerHealth -= unshieldedDamage;
            shield = 0;
            gui.showMessage("Enemy dealt " + unshieldedDamage + " damage to you! Current health: " + playerHealth);
        }

        if (playerHealth <= 0) {
            gui.showMessage("Game over. Player defeated!");
            gui.handleGameOver();
        } else {
            gui.updateGameStatus();
        }
    }

    // MODIFIES: this
    // EFFECTS: if damage >= enemy current health + 10, the enemy ignore this
    // attack.
    private void enemyTakeDamage(int damage) {
        int modeLimit = (level / 50) + 1;
        if (damage >= 5 + Math.pow(5, modeLimit) + currentEnemy.getHealth()) {
            gui.showMessage("Enemy ignored this attack.");
            return;
        }
        currentEnemy.setHealth(currentEnemy.getHealth() - damage);
        if (currentEnemy.getHealth() < 0) {
            currentEnemy.setHealth(0);
        }
        gui.showMessage("Enemy took " + damage + " damage. Remaining health: " + currentEnemy.getHealth());
    }

    // REQUIRES: number of numbercard > 0 in hand.
    // MODIFIES: this
    // EFFECTS: choose a number card in hand and attack or defend.

    public void playerAction(String action) {
        if (!canAct(action)) {
            return;
        }

        List<Card> numberCards = new ArrayList<>();
        for (Card card : hand) {
            if (card instanceof NumberCard) {
                numberCards.add(card);
            }
        }
        if (numberCards.isEmpty()) {
            gui.showMessage("No NumberCards available to " + action + ".");
            return;
        }
        String[] cardOptions = new String[numberCards.size()];
        for (int i = 0; i < numberCards.size(); i++) {
            cardOptions[i] = (i + 1) + ". " + numberCards.get(i).getName();
        }
        String input = gui.promptChoice("Choose a NumberCard to " + action + ":", cardOptions);
        
        if (action.equals("attack")) {
            attack(numberCards, input);
        } else if (action.equals("defend")) {
            defend(numberCards, input);
        }

    }

    // MODIFIES: this
    // EFFECT: use the card to attack based on the user choice.
    private void attack(List<Card> numberCards, String input) {
        if (input != null) {
            try {
                int choice = Integer.parseInt(input.split("\\.")[0]) - 1;
                if (choice >= 0 && choice < numberCards.size()) {
                    NumberCard attackCard = (NumberCard) numberCards.get(choice);
                    enemyTakeDamage(attackCard.getValue());
                    hand.remove(attackCard);
                    discardPile.add(attackCard);
                    checkBattleStatus();
                } else {
                    gui.showMessage("Invalid choice.");
                }
            } catch (NumberFormatException e) {
                gui.showMessage("Invalid input.");
            }
        } else {
            gui.showMessage("No card selected.");
        }
    }


    // MODIFIES: this
    // EFFECT: use the card to defend based on the user choice.
    private void defend(List<Card> numberCards, String input) {
        if (input != null) {
            try {
                int choice = Integer.parseInt(input.split("\\.")[0]) - 1;
                if (choice >= 0 && choice < numberCards.size()) {
                    NumberCard defendCard = (NumberCard) numberCards.get(choice);
                    int shieldedAmount = (int) Math
                            .round(Math.pow((Math.log(defendCard.getValue())), (1 + level / 10)));
                    shield = Math.max(1, Math.min(shield + shieldedAmount, 999));
                    hand.remove(defendCard);
                    discardPile.add(defendCard);
                    gui.updateGameStatus();
                } else {
                    gui.showMessage("Invalid choice.");
                }
            } catch (NumberFormatException e) {
                gui.showMessage("Invalid input.");
            }
        } else {
            gui.showMessage("No card selected.");
        }
    }

    // EFFECTS: check whether the action (attack or defend) is allowed.
    private boolean canAct(String action) {
        if (lastCard != null) {
            gui.showMessage("Cannot " + action + " during operation.");
            cancel();
            return false;
        }
        if (!hasNumberCard()) {
            gui.showMessage("Not enough NumberCards to " + action + "!");
            return false;
        } else {
            return true;
        }
    }

    // EFFECTS: check if there is a number card in hand to attack or defend.
    private boolean hasNumberCard() {
        for (Card card : hand) {
            if (card instanceof NumberCard) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------
    // Save & Load functions
    // ------------------------------------------------------------------

    // EFFECTS: manually save the game, will quit upon saving.
    public void saveAndQuit() {
        autoSave();
        gui.showMessage("Game saved successfully. See you next time!");
        System.exit(0);
    }

    // EFFECTS: Automatically save the game at:
    // 1. the start of each battle;
    // 2. the start of reward selection.
    private void autoSave() {
        if (state == GameState.BATTLE) {
            saveGameBattle();
        } else if (state == GameState.REWARD) {
            saveGameReward();
        } else {
            gui.showMessage("Unable to save game: unknown state.");
        }
    }

    // EFFECTS: saves the game to file during battle.
    private void saveGameBattle() {
        GameData gameData = new GameData(deck, playerHealth, level, mode, currentEnemy);
        States gameState = new States(state);
        try {
            jsonWriterGame.open();
            jsonWriterGame.write(gameData);
            jsonWriterGame.close();
            jsonWriterState.open();
            jsonWriterState.write(gameState);
            jsonWriterState.close();
        } catch (FileNotFoundException e) {
            gui.showMessage("Unable to save game: file not found.");
        }
    }

    // EFFECTS: saves the game to file during reward selection.
    private void saveGameReward() {
        Rewards reward = new Rewards(rewards);
        States gameState = new States(state);

        try {
            jsonWriterReward.open();
            jsonWriterReward.write(reward);
            jsonWriterReward.close();
            jsonWriterState.open();
            jsonWriterState.write(gameState);
            jsonWriterState.close();
        } catch (FileNotFoundException e) {
            gui.showMessage("Unable to save game: file not found.");
        }
    }

    // MODIFIES: this
    // EFFECTS: load the state of the game before load the game.
    private void loadGameState() {
        try {
            States state = jsonReaderState.read();
            this.state = state.getState();
        } catch (IOException e) {
            gui.showMessage("Unable to load game: file not found.");
        }
    }

    // MODIFIES: this
    // EFFECTS: load the game from the file depending on the game state.
    public void loadGame() {
        if (state == GameState.BATTLE || state == GameState.INITIAL) {
            loadGameBattle();
        } else if (state == GameState.REWARD) {
            loadGameReward();
        } else {
            gui.showMessage("Unable to load game: unknown state.");
        }
    }

    // MODIFIES: this
    // EFFECTS: loads the game from file if GameState is battle.
    private void loadGameBattle() {
        try {
            GameData gameData = jsonReaderGame.read();
            this.deck = gameData.getDeck();
            this.playerHealth = gameData.getPlayerHealth();
            this.level = gameData.getLevel();
            this.mode = gameData.getMode();
            this.currentEnemy = gameData.getEnemy();
        } catch (IOException e) {
            gui.showMessage("Unable to load game: file not found.");
        }
    }

    // MODIFIES: this
    // EFFECTS: loads the game from file if GameState is reward.
    private void loadGameReward() {
        try {
            Rewards jsonRewards = jsonReaderReward.read();
            this.rewards = jsonRewards.getRewards();
            loadGameBattle();
            rewardStatus();
            gui.showMessage("Game loaded successfully.");
        } catch (IOException e) {
            gui.showMessage("Unable to load game: file not found.");
        }
    }

    // MODIFIES: this, gui
    // EFFECT: check the battle status, whether player or enemy is defeated.
    private void checkBattleStatus() {
        if (currentEnemy.getHealth() <= 0) {
            gui.showMessage("Enemy defeated. Congrats!");
            rewardStatus();
        } else if (playerHealth <= 0) {
            gui.showMessage("Game over. Player defeated!");
            gui.handleGameOver();
        } else {
            gui.updateGameStatus();
        }
    }

}
