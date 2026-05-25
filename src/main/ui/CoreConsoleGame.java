package ui;

import ai_access.ActionResult;
import ai_access.CardGameCore;
import model.cards.Card;
import model.cards.NumberCard;
import persistence.GameData;
import persistence.JsonReaderGame;
import persistence.JsonReaderReward;
import persistence.JsonReaderState;
import persistence.JsonWriter;
import persistence.Rewards;
import persistence.States;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

// Console interface backed by CardGameCore.
public class CoreConsoleGame {
    private static final String JSON_STORE_GAME = "./data/gameData.json";
    private static final String JSON_STORE_REWARD = "./data/rewardData.json";
    private static final String JSON_STORE_STATE = "./data/stateData.json";

    private final CardGameCore core;
    private final Scanner scanner;
    private final JsonWriter jsonWriterGame;
    private final JsonReaderGame jsonReaderGame;
    private final JsonWriter jsonWriterReward;
    private final JsonReaderReward jsonReaderReward;
    private final JsonWriter jsonWriterState;
    private final JsonReaderState jsonReaderState;

    public CoreConsoleGame() {
        core = new CardGameCore();
        scanner = new Scanner(System.in);
        jsonWriterGame = new JsonWriter(JSON_STORE_GAME);
        jsonReaderGame = new JsonReaderGame(JSON_STORE_GAME);
        jsonWriterReward = new JsonWriter(JSON_STORE_REWARD);
        jsonReaderReward = new JsonReaderReward(JSON_STORE_REWARD);
        jsonWriterState = new JsonWriter(JSON_STORE_STATE);
        jsonReaderState = new JsonReaderState(JSON_STORE_STATE);
    }

    public void startGame() {
        System.out.println("\nWelcome to mathcard!");
        System.out.println("\tstart -> new game");
        System.out.println("\tload  -> load saved game");

        String option = scanner.nextLine().trim();
        if (option.equalsIgnoreCase("load")) {
            load();
        } else {
            core.start();
        }
        runLoop();
    }

    public void start() {
        core.start();
        runLoop();
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
            } else {
                core.loadBattle(
                        gameData.getDeck(),
                        gameData.getPlayerHealth(),
                        gameData.getLevel(),
                        gameData.getMode(),
                        gameData.getEnemy());
            }
            System.out.println("Game loaded successfully.");
        } catch (IOException e) {
            System.err.println("Unable to load game. Starting a new game.");
            core.start();
        }
    }

    public void loadAndRun() {
        load();
        runLoop();
    }

    public void saveAndQuit() {
        autoSave();
        System.out.println("Game saved successfully. See you next time!");
        System.exit(0);
    }

    private void runLoop() {
        while (!core.isGameOver() && !core.isGameWon()) {
            if (core.getState() == GameState.REWARD) {
                chooseReward();
            } else {
                playBattleAction();
            }
        }

        if (core.isGameWon()) {
            System.out.println("Victory! You cleared level " + core.getWinLevel() + ".");
        } else if (core.isGameOver()) {
            System.out.println("Game over. Player defeated!");
        }
    }

    private void playBattleAction() {
        displayStatus();
        System.out.println("Commands: play, attack, defend, end, cancel, save, quit");
        String command = scanner.nextLine().trim().toLowerCase();

        if (command.equals("play")) {
            handleAction(core.playCardAt(promptHandIndex("Choose a card to play:")));
        } else if (command.equals("attack")) {
            handleAction(core.attackWithCardAt(promptNumberCardIndex("Choose a NumberCard to attack:")));
        } else if (command.equals("defend")) {
            handleAction(core.defendWithCardAt(promptNumberCardIndex("Choose a NumberCard to defend:")));
        } else if (command.equals("end")) {
            handleAction(core.endRound());
        } else if (command.equals("cancel")) {
            handleAction(core.cancel());
        } else if (command.equals("save")) {
            saveAndQuit();
        } else if (command.equals("quit")) {
            System.exit(0);
        } else {
            System.out.println("Unknown command.");
        }
    }

    private void chooseReward() {
        List<String> rewards = core.getRewards();
        System.out.println("Choose your reward:");
        for (int i = 0; i < rewards.size(); i++) {
            System.out.println((i + 1) + ". " + rewards.get(i));
        }

        int rewardIndex = readChoice() - 1;
        if (rewardIndex < 0 || rewardIndex >= rewards.size()) {
            System.out.println("Invalid reward choice.");
            return;
        }

        if ("Remove".equals(rewards.get(rewardIndex))) {
            removeCard();
        }
        handleAction(core.chooseRewardAt(rewardIndex));
    }

    private int promptHandIndex(String message) {
        System.out.println(message);
        for (int i = 0; i < core.getHand().size(); i++) {
            System.out.println((i + 1) + ". " + core.getHand().get(i).getName());
        }
        return readChoice() - 1;
    }

    private int promptNumberCardIndex(String message) {
        List<Integer> numberCardIndexes = new ArrayList<>();
        System.out.println(message);
        for (int i = 0; i < core.getHand().size(); i++) {
            Card card = core.getHand().get(i);
            if (card instanceof NumberCard) {
                numberCardIndexes.add(i);
                System.out.println(numberCardIndexes.size() + ". " + card.getName());
            }
        }

        int choice = readChoice() - 1;
        if (choice < 0 || choice >= numberCardIndexes.size()) {
            return -1;
        }
        return numberCardIndexes.get(choice);
    }

    private void removeCard() {
        System.out.println("Choose a card to remove from your deck:");
        for (int i = 0; i < core.getDeck().getDeck().size(); i++) {
            System.out.println((i + 1) + ". " + core.getDeck().getDeck().get(i).getName());
        }

        ActionResult result = core.removeCardAt(readChoice() - 1);
        if (!result.isLegal()) {
            System.out.println(result.getMessage());
        }
    }

    private int readChoice() {
        try {
            return Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private void handleAction(ActionResult result) {
        if (!result.isLegal()) {
            System.out.println(result.getMessage());
        }
    }

    private void displayStatus() {
        System.out.println("\nLevel " + core.getLevel() + "/" + core.getWinLevel()
                + " | Mode " + core.getMode());
        System.out.println("Player HP: " + core.getPlayerHealth()
                + " | Shield: " + core.getShield()
                + " | Round: " + core.getRound());
        if (core.getCurrentEnemy() != null) {
            System.out.println("Enemy: " + core.getCurrentEnemy().getName()
                    + " HP " + core.getCurrentEnemy().getHealth()
                    + "/" + core.getCurrentEnemy().getMaxHealth()
                    + " DMG " + core.getCurrentEnemy().getDamage());
        }
        System.out.println("Hand:");
        for (int i = 0; i < core.getHand().size(); i++) {
            System.out.println((i + 1) + ". " + core.getHand().get(i).getName());
        }
    }

    private void autoSave() {
        if (core.getState() == GameState.REWARD) {
            saveGameReward();
        } else if (core.getState() == GameState.BATTLE) {
            saveGameBattle();
        } else {
            System.err.println("Unable to save game: unknown state.");
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
            System.err.println("Unable to save game: file not found.");
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
            System.err.println("Unable to save reward: file not found.");
        }
    }
}
