package persistence;


import model.Deck;
import model.cards.*;
import model.enemies.*;
import ui.Mode;


import org.json.JSONArray;
import org.json.JSONObject;


import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;


// Represents a reader that reads battle data from JSON data stored in file
public class JsonReaderGame {
    private String source;

     // EFFECTS: constructs reader to read from source file
    public JsonReaderGame(String source) {
        this.source = source;
    }


    // EFFECTS: reads battle data from file and returns it;
    // throws IOException if an error occurs reading data from file
    public GameData read() throws IOException {
        String jsonData = readFile(source);
        JSONObject jsonObject = new JSONObject(jsonData);
        return parseGameData(jsonObject);
    }


    // EFFECTS: reads source file as string and returns it.
    private String readFile(String source) throws IOException {
        StringBuilder contentBuilder = new StringBuilder();
        try (Stream<String> stream = Files.lines(Paths.get(source), StandardCharsets.UTF_8)) {
            stream.forEach(s -> contentBuilder.append(s));
        }
        return contentBuilder.toString();
    }

    // EFFECTS: parses gameData from JSON object and returns it
    private GameData parseGameData(JSONObject jsonObject) {
        String type = jsonObject.getJSONObject("enemy").getString("enemyType");
        Enemy tempEnemy = tempEnemy(type);
        GameData gameData =
                new GameData(new Deck(false), 0, 0, Mode.NORMAL, tempEnemy);
        gameData.setDeck(parseDeck(jsonObject.getJSONObject("deck")));
        gameData.setPlayerHealth(jsonObject.getInt("playerHealth"));
        gameData.setLevel(jsonObject.getInt("level"));
        gameData.setMode(Mode.valueOf(jsonObject.getString("mode")));
        gameData.setEnemy(parseEnemy(tempEnemy, jsonObject.getJSONObject("enemy")));
        return gameData;
    }

    // EFFECTS: create a temporary enemy based on the type string returned from json.
    private Enemy tempEnemy(String type) {
        if (type.equalsIgnoreCase("NORMAL")) {
            return new NormalEnemy(1);
        } else if (type.equalsIgnoreCase("ELITE")) {
            return new EliteEnemy(1);
        } else {
            return new BossEnemy(1);
        }
    }


    // EFFECTS: parses deck from JSON object and returns it
    private Deck parseDeck(JSONObject jsonObject) {
        Deck deck = new Deck(false);
        JSONArray deckArray = jsonObject.getJSONArray("deck");
        deck.getDeck().addAll(parseCards(deckArray));
        return deck;
    }


    // EFFECTS: parses a list of cards from JSON object and returns it
    private List<Card> parseCards(JSONArray jsonArray) {
        List<Card> cards = new ArrayList<>();
        for (Object json : jsonArray) {
            JSONObject nextCard = (JSONObject) json;
            cards.add(parseCard(nextCard));
        }
        return cards;
    }


    // EFFECTS: parses a card from JSON object and returns it
    private Card parseCard(JSONObject jsonObject) {
        CardType type = CardType.valueOf(jsonObject.getString("type"));
        
        if (type == CardType.NUMBER) {
            return new NumberCard(jsonObject.getInt("value"));
        } else if (type == CardType.SYMBOL) {
            return new SymbolCard(jsonObject.getString("name").charAt(0));
        } else {
            return new SpecialCard(jsonObject.getString("name"));
        } 
    }

    


    // EFFECTS: parses the current enemy from JSON object and returns it
    private Enemy parseEnemy(Enemy enemy, JSONObject jsonObject) {
        enemy.setDamage(jsonObject.getInt("damage"));
        enemy.setEnemyType(EnemyType.valueOf(jsonObject.getString("enemyType")));
        enemy.setHealth(jsonObject.getInt("health"));
        enemy.setMaxHealth(jsonObject.getInt("maxHealth"));
        enemy.setName(jsonObject.getString("name"));
        return enemy;
    }

}
