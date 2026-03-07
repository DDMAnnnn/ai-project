package persistence;

import ui.GameState;

import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Stream;




// Represents a reader that reads state data from JSON data stored in file
public class JsonReaderState {
    private String source;

     // EFFECTS: constructs reader to read from source file
    public JsonReaderState(String source) {
        this.source = source;
    }


    // EFFECTS: reads state data from file and returns it;
    // throws IOException if an error occurs reading data from file
    public States read() throws IOException {
        String jsonData = readFile(source);
        JSONObject jsonObject = new JSONObject(jsonData);
        return parseStateData(jsonObject);
    }

    // EFFECTS: reads source file as string and returns it.
    private String readFile(String source) throws IOException {
        StringBuilder contentBuilder = new StringBuilder();
        try (Stream<String> stream = Files.lines(Paths.get(source), StandardCharsets.UTF_8)) {
            stream.forEach(s -> contentBuilder.append(s));
        }
        return contentBuilder.toString();
    }


    // EFFECTS: parses rewardData from JSON object and returns it
    private States parseStateData(JSONObject jsonObject) {
        States states = new States(GameState.INITIAL);
        states.setState(GameState.valueOf(jsonObject.getString("state")));
        return states;
    }

}