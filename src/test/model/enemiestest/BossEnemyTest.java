package model.enemiestest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import model.enemies.BossEnemy;
import model.enemies.EnemyType;

public class BossEnemyTest {
    private BossEnemy testBoss;

    @BeforeEach
    void runBefore() {
        testBoss = new BossEnemy(10);
    }

    @Test
    void testConstructor() {
        assertEquals(testBoss.getEnemyType(), EnemyType.BOSS);
        assertEquals(testBoss.getHealth(), 100);
        assertEquals(testBoss.getDamage(), 10);
        assertEquals(testBoss.getName(), "BOSS FIGHT 1!");
    }

    @Test
    void testSetHealth() {
        assertEquals(testBoss.getHealth(), 100);
        testBoss.setHealth(50);
        assertEquals(testBoss.getHealth(), 50);
    }

    @Test
    void testSetDamage() {
        assertEquals(testBoss.getDamage(), 10);
        testBoss.setDamage(5);
        assertEquals(testBoss.getDamage(), 5);
    }

    @Test
    void testSetEnemyType() {
        assertEquals(testBoss.getEnemyType(), EnemyType.BOSS);
        testBoss.setEnemyType(EnemyType.NORMAL);
        assertEquals(testBoss.getEnemyType(), EnemyType.NORMAL);
    }

    @Test
    void testSetName() {
        assertEquals(testBoss.getName(), "BOSS FIGHT 1!");
        testBoss.setName("testName");
        assertEquals(testBoss.getName(), "testName");
    }

    @Test
    void testGetMaxHealth() {
        assertEquals(testBoss.getMaxHealth(), 100);
        testBoss.setMaxHealth(1);
        assertEquals(testBoss.getMaxHealth(), 1);
    }

    @Test
    void testToJson() {

        BossEnemy enemy = new BossEnemy(10);

        JSONObject json = enemy.toJson();

        assertEquals(enemy.getName(), json.getString("name"));
        assertEquals(enemy.getEnemyType().toString(), json.getString("enemyType"));
        assertEquals(enemy.getDamage(), json.getInt("damage"));
        assertEquals(enemy.getHealth(), json.getInt("health"));
    }
}
