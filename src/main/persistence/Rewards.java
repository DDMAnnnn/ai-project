package persistence;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

// Represents the rewards that need to be saved.
public class Rewards implements Writable {
    private List<String> rewards;


    // EFFECTS: construct a Rewards data for the purpose of saving.
    public Rewards(List<String> rewards) {
        this.rewards = rewards;
    }

    // getter
    public List<String> getRewards() {
        return rewards;
    }

    // setter
    public void setRewards(List<String> rewards) {
        this.rewards = rewards;
    }


    // Converts RewardData to JSON
    @Override
    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        JSONArray rewardsArray = new JSONArray();
        for (String reward : rewards) {
            rewardsArray.put(reward);
        }
        json.put("rewards", rewardsArray);
        return json;
    }

}



