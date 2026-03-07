package persistence;

import org.json.JSONObject;

import ui.GameState;

// Represents the game state that is needed for the game to distinguish the state before loading.
public class States implements Writable {
    private GameState state;

    // Construct a gameState data for the purpose of saving.
    public States(GameState state) {
        this.state = state;
    }

    // getter
    public GameState getState() {
        return state;
    }


    // setter
    public void setState(GameState state) {
        this.state = state;
    }

    // Converts RewardData to JSON
    @Override
    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("state", state.toString());
        return json;
    }
}
