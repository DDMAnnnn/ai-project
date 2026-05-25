package ui.userinterface;

import model.cards.Card;
import model.enemies.Enemy;

import javax.swing.*;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;



// Represents the GUI for the CardGame.
public class CardGameGUI extends JFrame {
    private CoreCardGame game;

    private CardLayout cardLayout;
    private StartMenu startMenuPanel;
    private JPanel gamePanel;
    private JPanel bottomPanel;
    private JPanel mainPanel;
    private JPanel handPanel;
    private JPanel controlPanel;
    private JPanel centerPanel;
    private JPanel settingsPanel;
    private PlayerPanel playerPanel;
    private EnemyPanel enemyPanel;
    private JTextArea handTextArea;

    private JButton attackButton;
    private JButton defendButton;
    private JButton endTurnButton;
    private JButton saveQuitButton;
    private JButton cancelButton;
    private JButton settingsButton;
    private JButton viewDiscardPileButton;
    private JButton viewDrawingPileButton;
    private JButton viewDeckButton;
    private JButton viewModeButton;
    private JButton viewTutorialButton;
    private JButton reoslutionButton;

    // Construct a gui for the card game.
    public CardGameGUI() throws IOException {

        setTitle("MathCard Game");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH);

        Image backgroundImage = new ImageIcon(getClass().getResource("/images/gameBackground.png")).getImage();

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        startMenuPanel = new StartMenu(this);
        gamePanel = new BackgroundPanel(backgroundImage);
        gamePanel.setLayout(new BorderLayout());
        mainPanel.add(startMenuPanel, "StartMenu");
        mainPanel.add(gamePanel, "GamePanel");

        add(mainPanel);

        cardLayout.show(mainPanel, "StartMenu");

        setVisible(true);
    }

    // MODIFIES: this
    // EFFECTS: initialize the game panel
    private void initGamePanel() {
        gamePanel.removeAll();

        initButtons();
        initActionListeners();
        initPanels();
        initHandPanel();
        initControlPanel();
        initBottomPanel();
        initCenterPanel();
        initSettingsPanel();
        addToGamePanel();
        setOpaque();

        updateGameStatus();

        gamePanel.revalidate();
        gamePanel.repaint();
    }

    // EFFECTS: create an empty panel to occupy the empty slot in gridlayout.
    private JPanel createEmptyPanel() {
        JPanel emptyPanel = new JPanel();
        emptyPanel.setOpaque(false);
        return emptyPanel;
    }

    // MODIFIES: this
    // EFFECTS: add panels to game panel.
    private void addToGamePanel() {
        gamePanel.add(bottomPanel, BorderLayout.SOUTH);
        gamePanel.add(centerPanel, BorderLayout.CENTER);
    }

    // MODIFIES: this
    // EFFECTS: initialize buttons, assign function to them.
    private void initButtons() {
        // Set up control panel with buttons
        attackButton = new JButton("Attack");
        defendButton = new JButton("Defend");
        endTurnButton = new JButton("End Turn");
        saveQuitButton = new JButton("Save & Quit");
        cancelButton = new JButton("Cancel");
        settingsButton = new JButton("Open Settings");

        viewDrawingPileButton = new JButton("View Drawing Pile");
        viewDiscardPileButton = new JButton("View Discard Pile");
        viewDeckButton = new JButton("View Deck");
        viewModeButton = new JButton("View Mode&Level");
        viewTutorialButton = new JButton("View Tutorial");
        reoslutionButton = new JButton("Choose Resolution");
    }

    private void initActionListeners() {
        // Add action listeners to buttons
        attackButton.addActionListener(e -> handleAttack());
        defendButton.addActionListener(e -> handleDefend());
        endTurnButton.addActionListener(e -> handleEndTurn());
        saveQuitButton.addActionListener(e -> handleSaveQuit());
        settingsButton.addActionListener(e -> handleSettings());
        cancelButton.addActionListener(e -> handleCancel());
        viewDiscardPileButton.addActionListener(e -> handleViewDiscardPile());
        viewDrawingPileButton.addActionListener(e -> handleViewDrawingPile());
        viewDeckButton.addActionListener(e -> handleViewDeck());
        viewModeButton.addActionListener(e -> handleViewModeAndLevel());
        viewTutorialButton.addActionListener(e -> handleViewTutorial());
        reoslutionButton.addActionListener(e -> handleResolution());
    }

    // MODIFIES: this
    // EFFECTS: create panels in the game panel.
    private void initPanels() {
        playerPanel = new PlayerPanel(game.getPlayerHealth(), game.getMaxHealth(), game.getShield());
        enemyPanel = new EnemyPanel(null);
        centerPanel = new JPanel(new GridLayout(2, 6));
        handPanel = new JPanel(new GridLayout(1, 10, 5, 5));
        controlPanel = new JPanel(new GridLayout(3, 4));
        settingsPanel = new JPanel(new GridLayout(3, 3, 5, 5));
        settingsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

    }

    // MODIFIES: this
    // EFFECTS: initialize hand panel.
    private void initHandPanel() {
        handPanel.setPreferredSize(new Dimension(800, 160));
        handPanel.setBorder(BorderFactory.createTitledBorder("Your Hand"));
        handTextArea = new JTextArea();
        handTextArea.setEditable(false);
        JScrollPane handScrollPane = new JScrollPane(handTextArea);
        handPanel.add(new JLabel("Your Hand:"), BorderLayout.NORTH);
        handPanel.add(handScrollPane, BorderLayout.CENTER);

    }

    // MODIFIES: this
    // EFFECTS: add buttons to control panel.
    private void initControlPanel() {
        controlPanel.add(attackButton);
        controlPanel.add(defendButton);
        controlPanel.add(cancelButton);
        controlPanel.add(endTurnButton);
        controlPanel.add(saveQuitButton);
        controlPanel.add(settingsButton);
        controlPanel.setMinimumSize(new Dimension(0,0));
    }

    // MODIFIES: thsi
    // EFFECTS: initialize center panel, containing player and enemy at (2,2) and
    // (2,4) correspondingly.
    private void initCenterPanel() {

        centerPanel.add(createEmptyPanel());
        centerPanel.add(createEmptyPanel());
        centerPanel.add(createEmptyPanel());
        centerPanel.add(createEmptyPanel());
        centerPanel.add(createEmptyPanel());
        centerPanel.add(createEmptyPanel());

        centerPanel.add(createEmptyPanel());
        centerPanel.add(playerPanel);
        centerPanel.add(createEmptyPanel());
        centerPanel.add(createEmptyPanel());
        centerPanel.add(enemyPanel);
        centerPanel.add(createEmptyPanel());
    }

    // MODIFIES: this
    // EFFECTS: initialzie bottom panel, containing control panel and hand panel,
    // left-right.
    private void initBottomPanel() {
        bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.X_AXIS));

        bottomPanel.add(controlPanel);
        bottomPanel.add(handPanel);

        handPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        controlPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        handPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, handPanel.getPreferredSize().height));
        controlPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, controlPanel.getPreferredSize().height));
    }

    private void initSettingsPanel() {

        settingsPanel.add(reoslutionButton);
        settingsPanel.add(viewDeckButton);
        settingsPanel.add(viewDrawingPileButton);
        settingsPanel.add(viewDiscardPileButton);
        settingsPanel.add(viewModeButton);
        settingsPanel.add(viewTutorialButton);
    }

    // MODIFIES: this
    // EFFECTS: make all panel non-opaque.
    private void setOpaque() {
        enemyPanel.setOpaque(false);
        bottomPanel.setOpaque(false);
        handPanel.setOpaque(false);
        controlPanel.setOpaque(false);
        centerPanel.setOpaque(false);

    }

    // MODIFIES: this
    // EFFECTS: start or load the game based on user choice.
    public void startOrLoad(String choice) throws IOException {

        game = new CoreCardGame(false, this);
        initGamePanel();

        if (choice.equals("start")) {
            game.start();
        } else if (choice.equals("load")) {
            game.load();
        }

        cardLayout.show(mainPanel, "GamePanel");

    }

    // MODIFIES: this
    // EFFECTS: Updates the game status in the UI components.
    public void updateGameStatus() {

        playerPanel.setCurrentHealth(game.getPlayerHealth());
        playerPanel.setShieldValue(game.getShield());

        Enemy currentEnemy = game.getCurrentEnemy();
        if (currentEnemy != null) {
            if (enemyPanel.getEnemy() != currentEnemy) {
                enemyPanel.setEnemy(currentEnemy);
            }
            enemyPanel.setCurrentHealth(currentEnemy.getHealth());
            enemyPanel.setMaxHealth(currentEnemy.getMaxHealth());
            enemyPanel.setAttackDamage(currentEnemy.getDamage());
        }

        updateHandDisplay();
    }

    // MODIFIES: this
    // EFFECTS: Displays a message to the user.
    public void showMessage(String message) {
        JOptionPane.showMessageDialog(this, message);
    }

    // MODIFIES: this
    // EFFECTS: Prompts the user to make a choice from a list of options.
    public String promptChoice(String message, String[] options) {
        if (options.length == 0) {
            showMessage("No options available.");
            return null;
        }
        return (String) JOptionPane.showInputDialog(this, message, "Select an Option", JOptionPane.PLAIN_MESSAGE, null,
                options, options[0]);
    }

    // MODIFIES: this
    // EFFECTS: Handles the game over scenario.
    public void handleGameOver() {
        int choice = JOptionPane.showConfirmDialog(this, "Game Over! Would you like to restart?", "Game Over",
                JOptionPane.YES_NO_OPTION);
        if (choice == JOptionPane.YES_OPTION) {
            try {
                game = new CoreCardGame(false, this);
                game.start();
                updateGameStatus();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            dispose();
        }
    }

    // MODIFIES: this
    // EFFECTS: Handles the victory scenario.
    public void handleGameWon() {
        int choice = JOptionPane.showConfirmDialog(this, "Victory! You cleared level " + game.getWinLevel()
                + ". Would you like to restart?", "Victory", JOptionPane.YES_NO_OPTION);
        if (choice == JOptionPane.YES_OPTION) {
            try {
                game = new CoreCardGame(false, this);
                game.start();
                updateGameStatus();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            dispose();
        }
    }


    // MODIFIES: this
    // EFFECTS: Shows reward options to the user.
    public void showRewardOptions(List<String> options) {
        String[] optionArray = options.toArray(new String[0]);
        String selectedOption = (String) JOptionPane.showInputDialog(
                this,
                "Choose your reward:",
                "Reward Selection",
                JOptionPane.PLAIN_MESSAGE,
                null,
                optionArray,
                optionArray[0]);

        if (selectedOption != null) {
            game.handleReward(selectedOption, options);
            updateGameStatus();
        } else {
            showMessage("No reward selected.");
            game.handleReward("Skip", options);
        }
    }

    // MODIFIES: this
    // EFFECTS: Handles the attack action.
    private void handleAttack() {
        game.playerAction("attack");
        updateGameStatus();
    }

    // MODIFIES: this
    // EFFECTS: Handles the defend action.
    private void handleDefend() {
        game.playerAction("defend");
        updateGameStatus();
    }

    // MODIFIES: this
    // EFFECTS: Handles the end turn action.
    private void handleEndTurn() {
        game.endRound();
        updateGameStatus();
    }

    // MODIFIES: this
    // EFFECTS: Handles the save and quit action.
    private void handleSaveQuit() {
        game.saveAndQuit();
        dispose();
    }

    private void handleCancel() {
        game.cancel();
        updateGameStatus();
    }

    // MODIFIES: this
    // EFFECTS: Handles viewing discard pile.
    private void handleViewDiscardPile() {
        List<Card> discardPile = game.getDiscardPile();
        List<Card> shuffledPile = new ArrayList<>(discardPile);
        Collections.shuffle(shuffledPile);
        showCardGrid(discardPile, "Discard Pile");
    }

    // MODIFIES: this
    // EFFECTS: Handles viewing drawing pile.
    private void handleViewDrawingPile() {
        List<Card> drawingPile = game.getDrawingPile();
        List<Card> shuffledPile = new ArrayList<>(drawingPile);
        Collections.shuffle(shuffledPile);
        showCardGrid(shuffledPile, "Drawing Pile");
    }

    // MODIFIES: this
    // EFFECTS: Handles viewing deck.
    private void handleViewDeck() {
        List<Card> deckCards = game.getDeck().getDeck();
        showCardGrid(deckCards, "Deck");
    }

    // MODIFIES: this
    // EFFECTS: Handles viewing the game mode.
    private void handleViewModeAndLevel() {
        JOptionPane.showMessageDialog(this,
                "Current Mode: " + game.getMode().name() + ", Current level: " + game.getLevel()
                        + "/" + game.getWinLevel(),
                "Game Mode & Level",
                JOptionPane.INFORMATION_MESSAGE);
    }

    // MODIFIES: this
    // EFFECTS: Handles viewing tutorial.
    private void handleViewTutorial() {
        String tutorialText = "\nGame Instructions:\n"
                +
                "- Play Card: Play a card from your hand.\n"
                +
                "- Attack: Attack the enemy using a NumberCard.\n"
                +
                "- Defend: Defend against enemy's attack using a NumberCard.\n"
                +
                "- End Turn: End your turn.\n"
                +
                "- Save & Quit: Save the game and exit.\n"
                +
                "- View Discard Pile: View the discard pile.\n"
                +
                "- View Drawing Pile: View the drawing pile.\n"
                +
                "- View Deck: View your deck.\n"
                +
                "- View Mode: View the current game mode.\n"
                +
                "Note: You can't see the order of drawing from the drawing pile.\n";
        JOptionPane.showMessageDialog(this, tutorialText, "Tutorial", JOptionPane.INFORMATION_MESSAGE);
    }

    // MODIFIES: this
    // EFFECTS: update the hand when the game status is changed.
    private void updateHandDisplay() {
        handPanel.removeAll();
        List<Card> hand = game.getHand();
        int handSize = hand.size();

        for (int i = 0; i < 10; i++) {
            Card card = (i < handSize) ? hand.get(i) : null;
            CardPanel cardPanel = new CardPanel(card);
            if (card != null) {
                int index = i;
                cardPanel.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        handleCardClick(index);
                    }
                });
            }
            handPanel.add(cardPanel);
        }
        handPanel.revalidate();
        handPanel.repaint();
    }

    // MODIFIES: this
    // EFFECTS: play the card upon clicking it.
    private void handleCardClick(int index) {
        Card card = game.getHand().get(index);
        game.playCard(card);
        updateGameStatus();

    }

    // MODIFIES: this
    // EFFECTS: shows a 5 by n card grid.
    private void showCardGrid(List<Card> cards, String title) {
        int columns = 5;
        int rows = (int) Math.ceil(cards.size() / (double) columns);

        JPanel gridPanel = new JPanel(new GridLayout(rows, columns, 5, 5));
        gridPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        for (Card card : cards) {
            CardPanel cardPanel = new CardPanel(card);
            gridPanel.add(cardPanel);
        }

        JScrollPane scrollPane = new JScrollPane(gridPanel);
        scrollPane.setPreferredSize(new Dimension(600, 180 * rows));

        JOptionPane.showMessageDialog(this, scrollPane, title, JOptionPane.PLAIN_MESSAGE);
    }

    // MODIFIES: this
    // EFFECTS: shows a 5 by n card grid.
    public void showCardGrid(List<Card> cards, String title, int columns, int rows) {

        JPanel gridPanel = new JPanel(new GridLayout(rows, columns, 5, 5));
        gridPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        for (Card card : cards) {
            CardPanel cardPanel = new CardPanel(card);
            gridPanel.add(cardPanel);
        }

        JScrollPane scrollPane = new JScrollPane(gridPanel);
        scrollPane.setPreferredSize(new Dimension(120 * columns, 180 * rows));

        JOptionPane.showMessageDialog(this, scrollPane, title, JOptionPane.PLAIN_MESSAGE);
    }

    private void handleSettings() {

        JScrollPane scrollPane = new JScrollPane(settingsPanel);
        scrollPane.setPreferredSize(new Dimension(600, 400));

        JOptionPane.showMessageDialog(this, scrollPane, "Settings", JOptionPane.PLAIN_MESSAGE);
    }

    private void handleResolution() {

        String[] resolutions = {
                "800x600",
                "1024x768",
                "1280x720",
                "1280x800",
                "1366x768",
                "1400x900",
                "1600x900",
                "1680x1050",
                "1920x1080",
                "1920x1200"
        };

        String choice = (String) promptChoice("Choose a resolution:", resolutions).split("\\:")[0];
        String[] resolution = choice.split("x");
        int currentWidth = Integer.parseInt(resolution[0].trim());
        int currentHeight = Integer.parseInt(resolution[1].trim());
        setExtendedState(JFrame.NORMAL);
        setSize(currentWidth, currentHeight);

    }

}
