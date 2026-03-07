package ui;

import java.io.IOException;

import javax.swing.SwingUtilities;

import ui.userinterface.CardGameGUI;

//Holds the main method.
public class Main {

    //Starts the game.
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                new CardGameGUI();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}