package persistence;

import org.json.JSONObject;

// Represent data who need to be saved.
public interface Writable {
    // EFFECTS: returns this as JSON object
    JSONObject toJson();
}