package model.enemiestest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import model.enemies.EliteEnemy;
import model.enemies.EnemyType;

public class EliteEnemyTest {
    private EliteEnemy testElite;

    @BeforeEach
    void runBefore() {
        testElite = new EliteEnemy(9);
    }

    @Test
    void testConstructor() {
        assertEquals(testElite.getEnemyType(), EnemyType.ELITE);
        assertEquals(testElite.getHealth(), 45);
        assertEquals(testElite.getDamage(), 4);
        assertEquals(testElite.getName(), "Elite Enemy 9");
    }
}
