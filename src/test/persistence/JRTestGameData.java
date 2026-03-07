package persistence;

import model.Deck;
import model.cards.CardType;
import model.enemies.Enemy;
import model.enemies.EnemyType;
import ui.Mode;

import org.junit.jupiter.api.Test;
import java.io.*;
import static org.junit.jupiter.api.Assertions.*;

class JRTestGameData extends JsonTest {

    @Test
    void testReaderInvalidFile() {
        JsonReaderGame reader = new JsonReaderGame("./data/NoSuchFile.json");
        try {
            GameData gameData = reader.read();
            fail("IOException was expected");
        } catch (IOException e) {
            // pass
        }
    }

    @Test
    void testReaderEmptyGameData() {
        JsonReaderGame reader = new JsonReaderGame("./data/testReaderEmptyGameData.json");
        try {
            GameData gameData = reader.read();
            Enemy enemy = gameData.getEnemy();
            checkGameData(enemy, 0, 0, Mode.NORMAL, "Normal Enemy 0",
                    EnemyType.NORMAL, 1, 0, gameData);
        } catch (IOException e) {
            fail("Exception should not have been thrown");
        }
    }

   
    @Test
    void testReaderGeneralGameData() {
        JsonReaderGame reader = new JsonReaderGame("./data/testReaderGeneralGameData.json");
        try {
            GameData gameData = reader.read();
            Enemy enemy = gameData.getEnemy();
            checkGameData(enemy, 110, 5, Mode.NORMAL, "Elite Enemy 5",
                    EnemyType.ELITE, 3, 25, gameData);
            Deck deck = gameData.getDeck();
            assertEquals(deck.getSize(), 3);
            checkCard("41", CardType.NUMBER, "A number card with value 41.", 41, '\0', deck.getCard(0));
            checkCard("*", CardType.SYMBOL, "A symbol card act as *.", Integer.MIN_VALUE, '*', deck.getCard(1));
            checkCard("merge", CardType.SPECIAL, "Special card: merge",
                    Integer.MIN_VALUE, '\0', deck.getCard(2));
        } catch (IOException e) {
            fail("Exception should not have been thrown");
        }
    }

    @Test
    void testReaderGeneralGameData2() {
        JsonReaderGame reader = new JsonReaderGame("./data/testReaderGeneralGameData2.json");
        try {
            GameData gameData = reader.read();
            Enemy enemy = gameData.getEnemy();
            checkGameData(enemy, 110, 10, Mode.NORMAL, "BOSS FIGHT 1!",
                    EnemyType.BOSS, 10, 100, gameData);
            Deck deck = gameData.getDeck();
            assertEquals(deck.getSize(), 3);
            checkCard("41", CardType.NUMBER, "A number card with value 41.", 41, '\0', deck.getCard(0));
            checkCard("*", CardType.SYMBOL, "A symbol card act as *.", Integer.MIN_VALUE, '*', deck.getCard(1));
            checkCard("merge", CardType.SPECIAL, "Special card: merge",
                    Integer.MIN_VALUE, '\0', deck.getCard(2));
        } catch (IOException e) {
            fail("Exception should not have been thrown");
        }
    }

}