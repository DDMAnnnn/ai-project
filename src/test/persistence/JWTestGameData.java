package persistence;

import ui.Mode;
import model.Deck;
import model.cards.CardType;
import model.cards.NumberCard;
import model.cards.SpecialCard;
import model.cards.SymbolCard;
import model.enemies.BossEnemy;
import model.enemies.EliteEnemy;
import model.enemies.Enemy;
import model.enemies.EnemyType;
import model.enemies.NormalEnemy;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class JWTestGameData extends JsonTest {

    @Test
    void testWriterInvalidFileGame() {
        try {
            GameData gameData = new GameData(new Deck(false), 0, 0, Mode.NORMAL, new NormalEnemy(1));
            JsonWriter writer = new JsonWriter("./data/my\0illegal:fileName.json");
            writer.open();
            fail("IOException was expected");
        } catch (IOException e) {
            // pass
        }
    }

    @Test
    void testWriterEmptyGameData() {
        try {
            Deck deck = new Deck(false);
            NormalEnemy normalEnemy = (NormalEnemy) new NormalEnemy(0);
            GameData gameData = new GameData(deck, 0, 0, Mode.NORMAL, normalEnemy);
            JsonWriter writer = new JsonWriter("./data/testWriterEmptyGameData.json");
            writer.open();
            writer.write(gameData);
            writer.close();

            JsonReaderGame reader = new JsonReaderGame("./data/testWriterEmptyGameData.json");
            gameData = reader.read();
            Enemy enemy = gameData.getEnemy();
            checkGameData(enemy, 0, 0,  Mode.NORMAL, "Normal Enemy 0",
                    EnemyType.NORMAL, 1, 0, gameData);

        } catch (IOException e) {
            fail("Exception should not have been thrown");
        }
    }

    @Test
    void testWriterGeneralGameData() {
        
        try {
            GameData gameData = new GameData(new Deck(false), 110, 5, Mode.NORMAL, new EliteEnemy(5));
            Deck deck = gameData.getDeck();
            deck.addCard(new NumberCard(41));
            deck.addCard(new SymbolCard('*'));
            deck.addCard(new SpecialCard("merge"));

            JsonWriter writer = new JsonWriter("./data/testWriterGeneralGameData.json");
            writer.open();
            writer.write(gameData);
            writer.close();

            JsonReaderGame reader = new JsonReaderGame("./data/testWriterGeneralGameData.json");
            gameData = reader.read();
            Enemy enemy = gameData.getEnemy();
            checkGameData(enemy, 110, 5, Mode.NORMAL, "Elite Enemy 5", EnemyType.ELITE, 3, 25, gameData);
            Deck readDeck = gameData.getDeck();
            assertEquals(readDeck.getSize(), 3);
            checkCard("41", CardType.NUMBER, "A number card with value 41.", 41, '\0', readDeck.getCard(0));
            checkCard("*", CardType.SYMBOL, "A symbol card act as *.", Integer.MIN_VALUE, '*', readDeck.getCard(1));
            checkCard("merge", CardType.SPECIAL, "Special card: merge",
                    Integer.MIN_VALUE, '\0', readDeck.getCard(2));
        } catch (IOException e) {
            fail("Exception should not have been thrown");
        }
    }

    @Test
    void testWriterGeneralGameData2() {
        
        try {
            GameData gameData = new GameData(new Deck(false), 110, 10, Mode.NORMAL, new BossEnemy(10));
            Deck deck = gameData.getDeck();
            deck.addCard(new NumberCard(41));
            deck.addCard(new SymbolCard('*'));
            deck.addCard(new SpecialCard("merge"));

            JsonWriter writer = new JsonWriter("./data/testWriterGeneralGameData2.json");
            writer.open();
            writer.write(gameData);
            writer.close();

            JsonReaderGame reader = new JsonReaderGame("./data/testWriterGeneralGameData2.json");
            gameData = reader.read();
            Enemy enemy = gameData.getEnemy();
            checkGameData(enemy, 110, 10, Mode.NORMAL, "BOSS FIGHT 1!", EnemyType.BOSS, 10, 100, gameData);
            Deck readDeck = gameData.getDeck();
            assertEquals(readDeck.getSize(), 3);
            checkCard("41", CardType.NUMBER, "A number card with value 41.", 41, '\0', readDeck.getCard(0));
            checkCard("*", CardType.SYMBOL, "A symbol card act as *.", Integer.MIN_VALUE, '*', readDeck.getCard(1));
            checkCard("merge", CardType.SPECIAL, "Special card: merge",
                    Integer.MIN_VALUE, '\0', readDeck.getCard(2));
        } catch (IOException e) {
            fail("Exception should not have been thrown");
        }
    }


}
