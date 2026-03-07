package model.enemiestest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import model.enemies.EnemyType;
import model.enemies.NormalEnemy;

public class NormalEnemyTest {
    private NormalEnemy testNormal;

    @BeforeEach
    void runBefore() {
        testNormal = new NormalEnemy(15);
    }

    @Test
    void testConstructor() {
        assertEquals(testNormal.getEnemyType(), EnemyType.NORMAL);
        assertEquals(testNormal.getHealth(), 30);
        assertEquals(testNormal.getDamage(), 2);
        assertEquals(testNormal.getName(), "Normal Enemy 15");
    }
}
