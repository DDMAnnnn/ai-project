package persistence;

import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.io.PrintWriter;

// Represents a writer that writes JSON representation of workroom to file
public class JsonWriter {
    private static final int TAB = 4;
    private PrintWriter writer;
    private String destination;

    // EFFECTS: constructs writer to write to destination file
    public JsonWriter(String destination) {
        this.destination = destination;
    }


    // MODIFIES: this
    // EFFECTS: opens writer; throws FileNotFoundException if destination file cannot
    // be opened for writing
    public void open() throws FileNotFoundException {
        writer = new PrintWriter(destination);
    }


    // MODIFIES: this
    // EFFECTS: writes JSON representation of gameData to file
    public void write(GameData gameData) {
        JSONObject json = gameData.toJson();
        saveToFile(json.toString(TAB));
    }


    // MODIFIES: this
    // EFFECTS: writes JSON representation of rewardData to file
    public void write(Rewards rewardData) {
        JSONObject json = rewardData.toJson();
        saveToFile(json.toString(TAB));

    }

    // MODIFIES: this
    // EFFECTS: writes JSON representation of stateData to file
    public void write(States stateData) {
        JSONObject json = stateData.toJson();
        saveToFile(json.toString(TAB));

    }


    // MODIFIES: this
    // EFFECTS: closes writer
    public void close() {
        writer.close();
    }


    // MODIFIES: this
    // EFFECTS: writes string to file
    private void saveToFile(String json) {
        writer.print(json);
    }
}
