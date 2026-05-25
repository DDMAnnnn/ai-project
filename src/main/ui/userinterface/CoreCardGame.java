package ui.userinterface;

import ai_access.ActionResult;
import ai_access.CardGameCore;
import model.Deck;
import model.cards.Card;
import model.cards.NumberCard;
import model.enemies.Enemy;
import persistence.GameData;
import persistence.JsonReaderGame;
import persistence.JsonReaderReward;
import persistence.JsonReaderState;
import persistence.JsonWriter;
import persistence.Rewards;
import persistence.States;
import ui.GameState;
import ui.Mode;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// Connects the GUI to CardGameCore without duplicating game rules.
public class CoreCardGame {
    private static final String JSON_STORE_GAME = "./data/gameData.json";
    private static final String JSON_STORE_REWARD = "./data/rewardData.json";
    private static final String JSON_STORE_STATE = "./data/stateData.json";

    private final CardGameCore core;
    private final CardGameGUI gui;
    private final JsonWriter jsonWriterGame;
    private final JsonReaderGame jsonReaderGame;
    private final JsonWriter jsonWriterReward;
    private final JsonReaderReward jsonReaderReward;
    private final JsonWriter jsonWriterState;
    private final JsonReaderState jsonReaderState;

    public CoreCardGame(boolean autostart, CardGameGUI gui) throws IOException {
        this.gui = gui;
        core = new CardGameCore();
        jsonWriterGame = new JsonWriter(JSON_STORE_GAME);
        jsonReaderGame = new JsonReaderGame(JSON_STORE_GAME);
        jsonWriterReward = new JsonWriter(JSON_STORE_REWARD);
        jsonReaderReward = new JsonReaderReward(JSON_STORE_REWARD);
        jsonWriterState = new JsonWriter(JSON_STORE_STATE);
        jsonReaderState = new JsonReaderState(JSON_STORE_STATE);

        if (autostart) {
            start();
        }
    }

    public Deck getDeck() {
        return core.getDeck();
    }

    public List<Card> getDrawingPile() {
        return core.getDrawingPile();
    }

    public List<Card> getHand() {
        return core.getHand();
    }

    public List<Card> getDiscardPile() {
        return core.getDiscardPile();
    }

    public Enemy getCurrentEnemy() {
        return core.getCurrentEnemy();
    }

    public int getPlayerHealth() {
        return core.getPlayerHealth();
    }

    public int getShield() {
        return core.getShield();
    }

    public int getLevel() {
        return core.getLevel();
    }

    public Mode getMode() {
        return core.getMode();
    }

    public int getMaxHealth() {
        return core.getMaxHealth();
    }

    public int getWinLevel() {
        return core.getWinLevel();
    }

    public void start() {
        core.start();
        gui.updateGameStatus();
    }

    public void load() {
        try {
            States savedState = jsonReaderState.read();
            GameData gameData = jsonReaderGame.read();

            if (savedState.getState() == GameState.REWARD) {
                Rewards savedRewards = jsonReaderReward.read();
                core.loadReward(
                        gameData.getDeck(),
                        gameData.getPlayerHealth(),
                        gameData.getLevel(),
                        gameData.getMode(),
                        gameData.getEnemy(),
                        savedRewards.getRewards());
                gui.updateGameStatus();
                gui.showRewardOptions(new ArrayList<>(core.getRewards()));
            } else {
                core.loadBattle(
                        gameData.getDeck(),
                        gameData.getPlayerHealth(),
                        gameData.getLevel(),
                        gameData.getMode(),
                        gameData.getEnemy());
                gui.updateGameStatus();
            }
        } catch (IOException e) {
            gui.showMessage("Unable to load game: file not found.");
            start();
        }
    }

    public void handleReward(String chosenOption, List<String> options) {
        int rewardIndex = options.indexOf(chosenOption);
        if (rewardIndex < 0) {
            gui.showMessage("Invalid choice, please retry.");
            gui.showRewardOptions(new ArrayList<>(core.getRewards()));
            return;
        }

        if ("Remove".equals(chosenOption)) {
            removeCard();
        } else if ("Skip".equals(chosenOption)) {
            gui.showMessage("You chose to skip.");
        } else {
            gui.showMessage("Added card: " + chosenOption + " to your deck.");
        }

        ActionResult result = core.chooseRewardAt(rewardIndex);
        handleActionResult(result);
    }

    public void playerAction(String action) {
        List<Integer> numberCardIndexes = numberCardIndexes();
        if (numberCardIndexes.isEmpty()) {
            gui.showMessage("No NumberCards available to " + action + ".");
            return;
        }

        String[] cardOptions = new String[numberCardIndexes.size()];
        for (int i = 0; i < numberCardIndexes.size(); i++) {
            Card card = core.getHand().get(numberCardIndexes.get(i));
            cardOptions[i] = (i + 1) + ". " + card.getName();
        }

        String input = gui.promptChoice("Choose a NumberCard to " + action + ":", cardOptions);
        if (input == null) {
            gui.showMessage("No card selected.");
            return;
        }

        try {
            int selectedOption = Integer.parseInt(input.split("\\.")[0]) - 1;
            int handIndex = numberCardIndexes.get(selectedOption);
            ActionResult result = "attack".equals(action)
                    ? core.attackWithCardAt(handIndex)
                    : core.defendWithCardAt(handIndex);
            handleActionResult(result);
        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            gui.showMessage("Invalid input.");
        }
    }

    public void endRound() {
        handleActionResult(core.endRound());
    }

    public void cancel() {
        handleActionResult(core.cancel());
    }

    public void playCard(Card card) {
        int handIndex = core.getHand().indexOf(card);
        if (handIndex < 0) {
            gui.showMessage("Card is no longer in hand.");
            return;
        }

        handleActionResult(core.playCardAt(handIndex));
    }

    public void saveAndQuit() {
        autoSave();
        gui.showMessage("Game saved successfully. See you next time!");
        System.exit(0);
    }

    private List<Integer> numberCardIndexes() {
        List<Integer> indexes = new ArrayList<>();
        for (int i = 0; i < core.getHand().size(); i++) {
            if (core.getHand().get(i) instanceof NumberCard) {
                indexes.add(i);
            }
        }
        return indexes;
    }

    private void removeCard() {
        List<Card> deckCards = core.getDeck().getDeck();
        String[] cardOptions = new String[deckCards.size()];
        for (int i = 0; i < deckCards.size(); i++) {
            cardOptions[i] = (i + 1) + ". " + deckCards.get(i).getName();
        }

        String chosenCard = gui.promptChoice("Choose a card to remove from your deck:", cardOptions);
        if (chosenCard == null) {
            gui.showMessage("No card selected. No card removed.");
            return;
        }

        try {
            int removeChoice = Integer.parseInt(chosenCard.split("\\.")[0]) - 1;
            ActionResult result = core.removeCardAt(removeChoice);
            if (result.isLegal()) {
                gui.showMessage("Card removed.");
            } else {
                gui.showMessage(result.getMessage());
            }
        } catch (NumberFormatException e) {
            gui.showMessage("Invalid input. No card removed.");
        }
    }

    private void handleActionResult(ActionResult result) {
        if (!result.isLegal()) {
            gui.showMessage(result.getMessage());
        }

        gui.updateGameStatus();

        if (core.isGameWon()) {
            gui.showMessage("You cleared level " + core.getWinLevel() + ". Victory!");
            gui.handleGameWon();
        } else if (core.isGameOver()) {
            gui.showMessage("Game over. Player defeated!");
            gui.handleGameOver();
        } else if (core.getState() == GameState.REWARD) {
            gui.showMessage("Enemy defeated. Congrats!");
            gui.showRewardOptions(new ArrayList<>(core.getRewards()));
        }
    }

    private void autoSave() {
        if (core.getState() == GameState.REWARD) {
            saveGameReward();
        } else if (core.getState() == GameState.BATTLE) {
            saveGameBattle();
        } else {
            gui.showMessage("Unable to save game: unknown state.");
        }
    }

    private void saveGameBattle() {
        GameData gameData = new GameData(
                core.getDeck(),
                core.getPlayerHealth(),
                core.getLevel(),
                core.getMode(),
                core.getCurrentEnemy());
        States gameState = new States(core.getState());
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

    private void saveGameReward() {
        saveGameBattle();
        Rewards reward = new Rewards(new ArrayList<>(core.getRewards()));
        try {
            jsonWriterReward.open();
            jsonWriterReward.write(reward);
            jsonWriterReward.close();
        } catch (FileNotFoundException e) {
            gui.showMessage("Unable to save reward: file not found.");
        }
    }
}
