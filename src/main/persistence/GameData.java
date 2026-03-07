package persistence;


import model.Deck;
import ui.Mode;
import model.enemies.*;
import org.json.JSONObject;

// Represents the battle stats that need to be saved.
public class GameData implements Writable {
    private Deck deck;
    private int playerHealth;
    private int level;
    private Mode mode;
    private Enemy enemy;


    // EFFECTS: construct a gamedata for the purpose of saving.
    public GameData(Deck deck, int playerHealth, int level, Mode mode, Enemy enemy) {
        this.deck = deck;
        this.playerHealth = playerHealth;
        this.level = level;
        this.mode = mode;
        this.enemy = enemy;
    }



    // ------------------------------------------------------------------
    // getters
    // ------------------------------------------------------------------


    public Deck getDeck() {
        return deck;
    }


    public int getPlayerHealth() {
        return playerHealth;
    }


    public int getLevel() {
        return level;
    }


    public Mode getMode() {
        return mode;
    }


    public Enemy getEnemy() {
        return enemy;
    }


    // ------------------------------------------------------------------
    // Setters
    // ------------------------------------------------------------------


    public void setDeck(Deck deck) {
        this.deck = deck;
    }


    public void setPlayerHealth(int playerHealth) {
        this.playerHealth = playerHealth;
    }


    public void setLevel(int level) {
        this.level = level;
    }


    public void setMode(Mode mode) {
        this.mode = mode;
    }


    public void setEnemy(Enemy enemy) {
        this.enemy = enemy;
    }


    // EFFECTS: converts this GameData into a JSON object
    @Override
    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("deck", deck.toJson());
        json.put("playerHealth", playerHealth);
        json.put("level", level);
        json.put("mode", mode.toString());
        json.put("enemy", enemy.toJson());
        return json;
    }


}




