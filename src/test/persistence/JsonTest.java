package persistence;

import model.cards.*;
import model.enemies.*;
import ui.Mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class JsonTest {
    protected void checkGameData(Enemy enemy, int playerHealth, int level, Mode mode, String enemyName,
            EnemyType type, int damage, int health, GameData gameData) {
        assertEquals(gameData.getPlayerHealth(), playerHealth);
        assertEquals(gameData.getLevel(), level);
        assertEquals(gameData.getMode(), mode);
        assertEquals(enemy.getName(), enemyName);
        assertEquals(enemy.getEnemyType(), type);
        assertEquals(enemy.getDamage(), damage);
        assertEquals(enemy.getHealth(), health);
    }

    protected void checkCard(String name, CardType type, String des, int value, char symbol, Card card) {

        assertEquals(name, card.getName());
        assertEquals(type, card.getCardType());
        assertEquals(des, card.getDescription());
        if (card instanceof NumberCard) {
            NumberCard numcard = (NumberCard) card;
            assertEquals(value, numcard.getValue());
        } else {
            assertEquals(value, Integer.MIN_VALUE);
        }
        if (card instanceof SymbolCard) {
            SymbolCard symcard = (SymbolCard) card;
            assertEquals(symbol, symcard.getSymbol());
        } else {
            assertEquals(symbol, '\0');
        }

    }

}
