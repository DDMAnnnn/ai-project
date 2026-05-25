package ui;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

import model.Deck;
import model.cards.*;
import model.enemies.*;
import persistence.*;

// EFFECTS: Represents the cardGame.
public class ConsoleGame {
    private Deck deck;
    private List<Card> drawingPile;
    private List<Card> hand;
    private List<Card> discardPile;
    private Enemy currentEnemy;
    private int playerHealth;
    private static final int INITIAL_HEALTH = 100;
    public static final int MAX_HEALTH = 500;
    private int level;
    private static final int INITIAL_HAND_SIZE = 5;
    private Card lastCard;
    private int calcValue;
    private char operation;
    private int shield;
    private static Scanner scanner = new Scanner(System.in);
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
    private CoreConsoleGame coreConsole;

    // ------------------------------------------------------------------
    // constructor: Construct the card game.
    // ------------------------------------------------------------------

    // MODIFIES: this
    // EFFECTS: construct a console-based game.
    public ConsoleGame(boolean autostart) throws IOException {
        deck = new Deck(true);
        drawingPile = new ArrayList<>(deck.getDeck());
        hand = new ArrayList<>();
        discardPile = new ArrayList<>();
        rewards = new ArrayList<String>();
        playerHealth = INITIAL_HEALTH;
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
        coreConsole = new CoreConsoleGame();
        if (autostart) {
            startGame();
        }
    }

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
    // ------------------------------------------------------------------
    // Game flow: battle -> defeat enemy -> get reward
    // ------------------------------------------------------------------

    // MODIFIES: this
    // EFFECTS: start the game.
    public void startGame() {
        coreConsole.startGame();
    }

    // MODIFIES: this
    // EFFECTS: start the game.
    public void start() {
        coreConsole.start();
    }

    // MODIFIES: this
    // EFFECTS: load the game.
    public void load() {
        coreConsole.loadAndRun();
    }

    // ------------------------------------------------------------------
    // Single Battle Handler: battle, rounds, rewards
    // ------------------------------------------------------------------

    // MODIFIES: this
    // EFFECTS: handle battle rounds. if enemy defeated, get reward and start a new
    // battle.
    private void battle() {
        while (currentEnemy.getHealth() > 0 && playerHealth > 0) {
            shuffle();
            startRound();
        }
        if (isDefeated()) {
            endRound();
            System.out.println("Enemy defeated. Congrats!");
            rewardStatus();
        } else {
            System.out.println("Game over. Player defeated!");
        }
    }

    // MODIFIES: this
    // EFFECTS: handle rewards and reset status after a battle is finished.
    private void rewardStatus() {
        heal();
        reward();
        System.out.println("Type 'next' to continue to the next battle.");

        String input = scanner.next();
        while (!input.equalsIgnoreCase("next")) {
            System.out.println("Invalid input. Please type 'next' to continue.");
            input = scanner.next();
        }
        rewards.clear();
        level += 1;
        mode();
        drawingPile = new ArrayList<>(deck.getDeck());
        discardPile = new ArrayList<>();
        round = 0;
        currentEnemy = null;
        newBattle();
    }

    // MODIFIES: this
    // EFFECTS: start a new battle.
    private void newBattle() {
        setState(GameState.BATTLE);
        currentEnemy = createNewEnemy();
        if (EnemyType.BOSS == currentEnemy.getEnemyType()) {
            System.out.println(currentEnemy.getName());
        } else {
            System.out.println("A new enemy appears: " + currentEnemy.getName());
        }
        battle();
        autoSave();
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

    // EFFECTS: check whether enemy is defeated.
    private boolean isDefeated() {
        return currentEnemy.getHealth() <= 0;
    }

    // MODIFIES: this
    // EFFECTS: heal the player.
    private void heal() {
        playerHealth += (1 + level / 5);
        if (playerHealth >= MAX_HEALTH) {
            playerHealth = MAX_HEALTH;
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
        displayOptions(rewards);
        playerChoice(rewards);
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
    // EFFECTS: choose reward by typing number in the console or save&quit.
    private void playerChoice(List<String> options) {
        try {
            String input = scanner.next();
            if (input.equalsIgnoreCase("save")) {
                saveAndQuit();
                System.out.println("Game saved successfully.");
            } else {
                handleReward(input, options);
            }
        } catch (InputMismatchException e) {
            System.err.println("Invalid input, please retry.");
            reward();
        }

    }

    // MODIFIES: this
    // EFFECTS: give the reward based on the choice.
    private void handleReward(String input, List<String> options) {
        if (isValid(input)) {
            int choice = Integer.parseInt(input);
            if (choice > 0 && choice <= options.size()) {
                String chosenOption = options.get(choice - 1);
                if (chosenOption.equals("Skip")) {
                    System.out.println("You chose to skip.");
                } else if (chosenOption.equals("Remove")) {
                    removeCard();
                    System.out.println("Card removed!");
                } else {
                    deck.addCard(optionToCard(chosenOption));
                    System.out.println("Added card: " + chosenOption + " to your deck.");
                }
            } else {
                System.err.println("Invalid choice, please retry.");
                reward();
            }
        } else {
            System.err.println("Invalid input, please retry.");
            reward();
        }
    }

    // REQUIRES: the deck is not empty.
    // MODIFIES: this
    // EFFECTS: remove a card from the deck.
    private void removeCard() {
        System.out.println("Choose a card to remove from your deck:");
        for (int i = 0; i < deck.getDeck().size(); i++) {
            System.out.println((i + 1) + ". " + deck.getDeck().get(i).getName());
        }
        int removeChoice = scanner.nextInt();
        if (removeChoice > 0 && removeChoice <= deck.getDeck().size()) {
            Card removedCard = deck.getDeck().remove(removeChoice - 1);
            System.out.println("Removed card: " + removedCard.getName());
        } else {
            System.err.println("Invalid choice. No card removed.");
        }
    }

    // MODIFIES: this
    // EFFECTS: change modes to at certain level.
    private void mode() {
        switch (level) {
            case 51:
                System.out.println("Congrats! Welcome to hard mode!");
                setMode(Mode.HARD);
                break;

            case 101:
                System.out.println("Welcome to master mode! Enjoy!");
                setMode(Mode.MASTER);
                break;
            case 151:
                System.out.println("Welcome to hell! Be prapared!");
                setMode(Mode.HELL);
                break;
            case 201:
                System.out.println("Inferno mode! Suffer!");
                setMode(Mode.INFERNO);
                break;
            default:
                break;
        }
    }

    // ------------------------------------------------------------------
    // Single Round Handler: start a round, act, endround
    // ------------------------------------------------------------------

    // MODIFIES: this
    // EFFECTS: handle a single round. Reset defense, draw cards, take actions and
    // endround.
    private void startRound() {
        int handSize = Math.min(INITIAL_HAND_SIZE + (level - 1) / 50, 9);
        round += 1;
        System.out.println("Round " + round + "!");
        shield = 0;
        drawCards(handSize);
        defaultInterface();
        roundOptions();
        endRound();
    }

    // MODIFIES: this
    // EFFECTS: play cards in the round or choose to end round.
    private void playRound(Map<String, Runnable> commands) {
        boolean roundEnded = false;
        while (!roundEnded) {
            if (currentEnemy.getHealth() <= 0) {
                roundEnded = true;
                break;
            }

            String choice = scanner.next().toLowerCase();
            if (commands.containsKey(choice)) {
                commands.get(choice).run();
            } else if (choice.equalsIgnoreCase("end")) {
                roundEnded = true;
            } else if (isValid(choice)) {
                choices(choice);
            } else {
                System.err.println("Invalid input. Type 'back' to return.");
            }
        }
    }

    // MODIFIES: this
    // EFFECTS: Choose a card and play.
    private void choices(String choice) {
        int intChoice = Integer.parseInt(choice);
        if (intChoice > 0 && intChoice <= hand.size()) {
            Card chosenCard = hand.get(intChoice - 1);
            playCard(chosenCard);
        } else {
            System.err.println("Invalid choice. Type 'back' to return.");
        }
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
        }
    }

    // MODIFIES: this
    // EFFECTS: contains options can be selected in a round.
    private void roundOptions() {
        Map<String, Runnable> commands = new HashMap<>();
        commands.put("attack", this::playerAttack);
        commands.put("defend", this::playerDefend);
        commands.put("cancel", this::cancel);
        commands.put("save", this::saveAndQuit);
        commands.put("discardpile", this::displayDiscardPile);
        commands.put("drawingpile", this::displayDrawingPile);
        commands.put("deck", this::displaydeck);
        commands.put("mode", this::displayMode);
        commands.put("tutorial", this::displayTutorial);
        commands.put("back", this::displayStatus);
        playRound(commands);
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
                Card drawnCard = drawingPile.remove(0);
                if (hand.size() < 10) {
                    hand.add(drawnCard);
                } else {
                    discardPile.add(drawnCard);
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
                System.out.println("No more card to discard!");
                defaultInterface();
            }
        }
    }

    // MODIFIES: this
    // REQUIRES: drawing pile is empty.
    // EFFECTS:reshuffle discard pile into drawing pile and shuffle drawing pile.
    private void reshuffle() {
        System.out.println("Reshuffling...");
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
    // EFFECTS: check whether the input action is valid.
    private Boolean isValid(String choice) {
        return choice.matches("\\d+");
    }

    // MODIFIES: this
    // EFFECTS: cancel the previous action.
    public void cancel() {
        if (!(lastCard == null)) {
            if (operation == '\0') {
                returnToHand(lastCard);
                discardPile.remove(lastCard);
            } else {
                returnToHand(lastCard);
                NumberCard calCard = new NumberCard(calcValue);
                returnToHand(calCard);
                for (Card card : discardPile) {
                    if (calCard.cardEquals(card)) {
                        discardPile.remove(card);
                        break;
                    }
                }
            }
            reset();
            System.out.println("Action canceled!");
            defaultInterface();
        } else {
            System.err.println("No card played previously. Type 'back' to return.");
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
            System.err.println("Unknown card type.");
        }
        hand.remove(card);
        discardPile.add(card);
        defaultInterface();
    }

    // REQUIRES: 2 number card can't be played consecutively, card value <
    // Integer.MAX_VALUE.
    // MODIFIES: this
    // EFFECTS: play a number card.
    private void playNumberCard(NumberCard card) {
        System.out.println("Playing NumberCard with value: " + card.getValue());
        if (lastCard == null && operation == '\0') {
            calcValue = card.getValue();
            lastCard = card;
        } else if (lastIsNumCard()) {
            System.err.println("2 NumberCard cannot be played consecutively.");
            returnToHand(card);
        } else {
            calculateValue(card);

            System.out.println("New card calculated! " + calcValue);
            returnToHand(new NumberCard(calcValue));
            reset();
        }
    }

    // REQUIRES: a number card must be played right before and after a symbol card.
    // MODIFIES: this
    // EFFECTS: play a symbol card.
    private void playSymbolCard(SymbolCard card) {
        if (hasNumberCard()) {
            if (!(lastIsNumCard())) {
                System.err.println("A SymbolCard can only be played after a NumberCard.");
                returnToHand(card);
                return;
            }
            System.out.println("Current operation: " + card.getSymbol());
            operation = card.getSymbol();
            lastCard = card;
        } else {
            System.err.println("You don't have enough NumberCards!");
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
            System.err.println("Unknown special card action.");
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
                System.err.println("Unknown error, please retry.");
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
            System.err.println("Cannot divide by zero.");
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
    // Speicial Card Handler: handle operations with
    // "merge", "split" and "draw"
    // ------------------------------------------------------------------

    // REQUIRES: a number card must be played right before and after a merge card.
    // MODIFIES: this
    // EFFECTS: play a merge card which merge 2 number cards into 1.
    private void mergeCard(SpecialCard card) {
        SymbolCard specSymCard = new SymbolCard('m');
        if (hasNumberCard()) {
            if (!(lastIsNumCard())) {
                System.err.println("A Merge Card can only be played after a NumberCard.");
                returnToHand(card);
                return;
            }
            System.out.println("Playing SpecialCard: merge");
            operation = specSymCard.getSymbol();
            lastCard = card;
            drawCards(1);
        } else {
            System.err.println("You don't have enough NumberCards!");
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
        if (lastIsNumCard() && !(calcValue == Integer.MIN_VALUE)) {
            NumberCard card = (NumberCard) lastCard;
            String numString = String.valueOf(card.getValue());
            if (canSplitCard()) {
                int part1 = Character.getNumericValue(numString.charAt(0));
                int part2 = Integer.parseInt(numString.substring(1));
                System.out.println("Card splitted!");
                returnToHand(new NumberCard(part1));
                returnToHand(new NumberCard(part2));
                hand.remove(card);
                drawCards(1);
                reset();

            } else {
                System.err.println("Cannot split a single-digit number.");
            }
        } else {
            handleSpecialCardError("split", "Split requires a number card to be played before.");
        }
    }

    // REQUIRES: no more than 5 card drawn one time.
    // MODIFIES: this
    // EFFECTS: draw card based on number played before, maximum 5.
    private void drawCard() {
        if (lastIsNumCard() && !(calcValue == Integer.MIN_VALUE)) {
            NumberCard card = (NumberCard) lastCard;
            int toDraw = Math.min(card.getValue(), 5);
            System.out.println("Drawing " + toDraw + " cards.");
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
        System.err.println(errorMessage);
        SpecialCard specialCard = new SpecialCard(cardType);
        returnToHand(specialCard);
        for (Card card : discardPile) {
            if (specialCard.cardEquals(card)) {
                discardPile.remove(card);
                break;
            }
        }
        reset();
    }

    // ------------------------------------------------------------------
    // Attack&Defned Handler: attacking and defending
    // for player and enemy
    // ------------------------------------------------------------------

    // MODIFIES: this
    // EFFECTS: let enemy attack, player lose HP or shield.
    private void enemyAttack() {
        int damage = currentEnemy.getDamage();
        int unshieldedDamage = damage - shield;
        if (damage <= shield) {
            shield -= damage;
            System.out.println("Damage blocked!");
        } else {
            playerHealth -= unshieldedDamage;
            shield = 0;
            System.out.println("Enemy dealt " + unshieldedDamage + " damage to you! Current health: " + playerHealth);
        }
    }

    // MODIFIES: this
    // EFFECTS: if damage >= enemy current health + 10, the enemy ignore this
    // attack.
    private void enemyTakeDamage(int damage) {
        int modeLimit = (level / 50) + 1;
        if (damage >= 5 + Math.pow(5, modeLimit) + currentEnemy.getHealth()) {
            System.out.println("Enemy ignored this attack, just because he can.");
            return;
        }
        currentEnemy.setHealth(currentEnemy.getHealth() - damage);
        if (currentEnemy.getHealth() < 0) {
            currentEnemy.setHealth(0);
        }
        System.out.println("Enemy took " + damage + " damage. Remaining health: " + currentEnemy.getHealth());
    }

    // REQUIRES: number of numbercard > 0 in hand.
    // MODIFIES: this
    // EFFECTS: choose a number card in hand and deal that much damage to enemy.
    public void playerAttack() {
        if (!canAct("attack")) {
            return;
        }

        displayNumCard("Choose a NumberCard to attack the enemy:");
        try {
            int choice = scanner.nextInt();
            if (validCard(choice)) {
                NumberCard attackCard = (NumberCard) hand.get(choice - 1);
                enemyTakeDamage(attackCard.getValue());
                hand.remove(attackCard);
                discardPile.add(attackCard);
                defaultInterface();
            } else {
                System.err.println("Invalid choice. Type 'back' to return.");
            }
        } catch (InputMismatchException e) {
            System.err.println("Invalid input. Type 'back' to return.");
        }
    }

    // REQUIRES: 1 <= defend <= 999, number of numbercard > 0 in hand.
    // MODIFIES: this
    // EFFECTS: defend enemy's attack by adding temporary health, defend takes
    // logarithmic.
    public void playerDefend() {
        if (!canAct("defend")) {
            return;
        }

        displayNumCard("Choose a NumberCard to defend enemy's attack:");
        try {
            int choice = scanner.nextInt();
            if (validCard(choice)) {
                NumberCard defendcard = (NumberCard) hand.get(choice - 1);
                int shieldedAmount = (int) Math.round(Math.log(defendcard.getValue()));
                shield = Math.max(1, Math.min(shield + shieldedAmount, 999));
                hand.remove(defendcard);
                discardPile.add(defendcard);
                defaultInterface();
            } else {
                System.err.println("Invalid choice. Type 'back' to return.");
            }
        } catch (InputMismatchException e) {
            System.err.println("Invalid input. Type 'back' to return.");
        }
    }

    // EFFECTS: check whether the action (attack or defend) is allowed.
    private boolean canAct(String action) {
        if (lastCard != null) {
            System.err.println("Cannot" + action + "during operation.");
            cancel();
            return false;
        }
        if (!hasNumberCard()) {
            System.err.println("Not enough NumberCard to " + action + " !");
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

    // EFFECTS: check if the card chosen is valid for attack or defend.
    private boolean validCard(int choice) {
        return choice > 0 && choice <= hand.size() && hand.get(choice - 1) instanceof NumberCard;
    }

    // ------------------------------------------------------------------
    // Console Display Functions
    // ------------------------------------------------------------------

    // EFFECTS: display game status.
    private void displayStatus() {
        displayEnemyStatus();
        displayPlayerStatus();
        displayHand();
    }

    // EFFECTS: display enemy's status.
    private void displayEnemyStatus() {
        System.out.println("Enemy Health:" + currentEnemy.getHealth()
                + ", Enemy Damage:" + currentEnemy.getDamage());
    }

    // EFFECTS: display player's status
    private void displayPlayerStatus() {
        System.out.println("Your Health:" + getPlayerHealth() + ", Your Shield:" + getShield());
    }

    // EFFECTS: abstract function for diplay cards.
    private void displayer(List<Card> cards, String pile) {
        System.out.println("your " + pile + ":");
        for (int i = 0; i < cards.size(); i++) {
            System.out.println((i + 1) + ". " + cards.get(i).getName() + " (" + cards.get(i).getCardType() + ")");
        }
        if (!(pile.equalsIgnoreCase("hand"))) {
            System.out.println("Type 'back' to return.");
        }
    }

    // EFFECTS: abstract function for display number cards.
    private void displayNumCard(String string) {
        System.out.println(string);
        for (int i = 0; i < hand.size(); i++) {
            if (hand.get(i) instanceof NumberCard) {
                System.out.println((i + 1) + ". " + hand.get(i).getName() + " (" + hand.get(i).getCardType() + ")");
            }
        }
    }

    // EFFECTS: display reward options.
    private void displayOptions(List<String> options) {
        System.out.println("Congrats! claim your reawrd:");
        for (int i = 0; i < options.size(); i++) {
            System.out.println((i + 1) + ". " + options.get(i));
        }
    }

    // EFFECTS: display the hand.
    private void displayHand() {
        displayer(hand, "hand");
    }

    // EFFECTS: display the discard pile.
    private void displayDiscardPile() {
        List<Card> shuffledPile = new ArrayList<>(discardPile);
        Collections.shuffle(shuffledPile);

        displayer(shuffledPile, "discard pile");
    }

    // EFFECTS: display the drawing pile.
    private void displayDrawingPile() {
        List<Card> shuffledPile = new ArrayList<>(drawingPile);
        Collections.shuffle(shuffledPile);

        displayer(shuffledPile, "drawing pile");
    }

    // EFFECTS: display the deck.
    private void displaydeck() {
        displayer(deck.getDeck(), "deck");
    }

    // EFFECTS: display the current mode.
    private void displayMode() {
        System.out.println("Current Mode: " + mode.name());
        System.out.println("Type 'back' to return.");
    }

    // EFFECTS: display the tutorial.
    private void displayTutorial() {
        System.out.println("\nType the following:");
        System.out.println("\tnumber -> play a card");
        System.out.println("\tattack -> attack enemy");
        System.out.println("\tdefend -> defend enemy's attack");
        System.out.println("\tcancel -> cancel the last action");
        System.out.println("\tsave -> save&quit");
        System.out.println("\tdiscardpile -> view discard pile");
        System.out.println("\tdrawingpile -> view drawing pile");
        System.out.println("\tend -> end the turn");
        System.out.println("\tback -> go back to battle");
        System.out.println("Note: you can't see the order of drawing from drawing pile");
        System.out.println("Type 'back' to return.");
    }

    // EEFECTS: display the defalt user interface.
    private void defaultInterface() {
        displayStatus();
        System.out.println("Play a card, or type 'tutorial' to see the tutorial:");
    }

    // ------------------------------------------------------------------
    // Save&Load functions
    // ------------------------------------------------------------------

    // EFFECTS: manually save the game, will quit upon saving.
    public void saveAndQuit() {
        coreConsole.saveAndQuit();
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
            System.err.println("Unable to save game: file not found.");
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
            System.err.println("Unable to save game: file not found.");
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
            System.err.println("Unable to save game: file not found.");
        }

    }

    // MODIFIES: this
    // EFFECTS: load the state of the game before load the game.
    private void loadGameState() {
        try {
            States state = jsonReaderState.read();
            this.state = state.getState();
        } catch (IOException e) {
            System.err.println("Unable to load game: file not found.");
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
            System.err.println("Unable to load game: file not found.");
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
            System.err.println("Unable to load game: file not found.");
        }
    }

    // MODIFIES: this
    // EFFECTS: loads the game from file if GameState is reward
    private void loadGameReward() {
        try {
            Rewards jsonRewards = jsonReaderReward.read();
            this.rewards = jsonRewards.getRewards();
            loadGameBattle();
            rewardStatus();
            System.out.println("Game loaded successfully.");
        } catch (IOException e) {
            System.err.println("Unable to load game: file not found.");
        }
    }
}
