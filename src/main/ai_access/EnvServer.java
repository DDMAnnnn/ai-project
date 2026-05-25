package ai_access;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import org.json.JSONArray;
import org.json.JSONObject;

// Exposes MathCardEnv over stdin/stdout using one JSON object per line.
public class EnvServer {

    public static void main(String[] args) throws Exception {
        System.setProperty("mathcard.silentEventLog", "true");

        MathCardEnv env = new MathCardEnv();
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        PrintWriter writer = new PrintWriter(System.out, true);

        String line;
        while ((line = reader.readLine()) != null) {
            JSONObject response = handleRequest(env, line);
            writer.println(response.toString());
        }
    }

    private static JSONObject handleRequest(MathCardEnv env, String line) {
        try {
            JSONObject request = new JSONObject(line);
            String command = request.getString("cmd");

            if (command.equals("reset")) {
                return stepResultToJson(env.reset());
            } else if (command.equals("step")) {
                int action = request.getInt("action");
                return stepResultToJson(env.step(action));
            } else {
                return errorJson("unknown command: " + command);
            }
        } catch (Exception e) {
            return errorJson(e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    private static JSONObject stepResultToJson(StepResult result) {
        JSONObject json = new JSONObject();
        json.put("ok", true);
        json.put("reward", result.getReward());
        json.put("done", result.isDone());
        json.put("actionMask", booleanArrayToJson(result.getActionMask()));
        json.put("actionResult", actionResultToJson(result.getActionResult()));
        json.put("observation", observationToJson(result.getObservation()));
        return json;
    }

    private static JSONObject actionResultToJson(ActionResult result) {
        JSONObject json = new JSONObject();
        json.put("legal", result.isLegal());
        json.put("reward", result.getReward());
        json.put("done", result.isDone());
        json.put("message", result.getMessage());
        return json;
    }

    private static JSONObject observationToJson(Observation observation) {
        JSONObject json = new JSONObject();
        json.put("state", observation.getState().name());
        json.put("mode", observation.getMode().name());
        json.put("level", observation.getLevel());
        json.put("playerHealth", observation.getPlayerHealth());
        json.put("shield", observation.getShield());
        json.put("round", observation.getRound());
        json.put("enemyHealth", observation.getEnemyHealth());
        json.put("enemyMaxHealth", observation.getEnemyMaxHealth());
        json.put("enemyDamage", observation.getEnemyDamage());
        json.put("enemyType", observation.getEnemyType());
        json.put("drawingPileSize", observation.getDrawingPileSize());
        json.put("discardPileSize", observation.getDiscardPileSize());
        json.put("deckSize", observation.getDeckSize());
        json.put("calcValue", observation.getCalcValue());
        json.put("operation", String.valueOf(observation.getOperation()));
        json.put("deckCards", new JSONArray(observation.getDeckCards()));
        json.put("handCards", new JSONArray(observation.getHandCards()));
        json.put("rewardOptions", new JSONArray(observation.getRewardOptions()));
        return json;
    }

    private static JSONArray booleanArrayToJson(boolean[] values) {
        JSONArray array = new JSONArray();
        for (boolean value : values) {
            array.put(value);
        }
        return array;
    }

    private static JSONObject errorJson(String message) {
        JSONObject json = new JSONObject();
        json.put("ok", false);
        json.put("error", message);
        return json;
    }
}
