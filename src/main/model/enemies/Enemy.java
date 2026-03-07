package model.enemies;

import org.json.JSONObject;

import persistence.Writable;

//Represent an abstract enemy, which has 3 different types: normal, elite and boss.
public abstract class Enemy  implements Writable  {
    private int health;
    private int maxHealth;
    private int damage;
    private EnemyType enemyType;
    private String name;

    //Construct an abstract enemy.
    public Enemy(EnemyType type, int health, int maxHealth, int damage, String name) {
        this.enemyType = type;
        this.health = health;
        this.maxHealth = maxHealth;
        this.damage = damage;
        this.name = name;
    }

    // Getters
    public EnemyType getEnemyType() {
        return enemyType;
    }

    public int getHealth() {
        return health;
    }

    public int getDamage() {
        return damage;
    }

    public String getName() {
        return name;
    }

    public int getMaxHealth() {
        return maxHealth;
    }

    // setters
    public void setDamage(int damage) {
        this.damage = damage;
    }

    public void setEnemyType(EnemyType enemyType) {
        this.enemyType = enemyType;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setHealth(int health) {
        this.health = health;
    }

    public void setMaxHealth(int maxHealth) {
        this.maxHealth = maxHealth;
    }

     // EFFECTS: converts a Card into a JSON object
    @Override
    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("name", name);
        json.put("health", health);
        json.put("maxHealth", maxHealth);
        json.put("damage", damage);
        json.put("enemyType", enemyType.toString());
        return json;
    }
}

