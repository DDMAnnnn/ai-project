package persistence;


import org.json.JSONArray;
import org.json.JSONObject;


import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;



// Represents a reader that reads reward data from JSON data stored in file
public class JsonReaderReward {
    private String source;

     // EFFECTS: constructs reader to read from source file
    public JsonReaderReward(String source) {
        this.source = source;
    }


    // EFFECTS: reads Rewards data from file and returns it;
    // throws IOException if an error occurs reading data from file
    public Rewards read() throws IOException {
        String jsonData = readFile(source);
        JSONObject jsonObject = new JSONObject(jsonData);
        return parseRewardData(jsonObject);
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
    private Rewards parseRewardData(JSONObject jsonObject) {
        Rewards rewards = new Rewards(new ArrayList<String>());
        rewards.setRewards(parseRewards(jsonObject.getJSONArray("rewards")));
        return rewards;
    }


    // EFFECTS: parses list of rewards in string from JSON object and returns it
    private List<String> parseRewards(JSONArray jsonArray) {
        List<String> rewards = new ArrayList<>();
       
        for (Object json : jsonArray) {
            rewards.add((String) json);
        }
       
        return rewards;
    }


}
