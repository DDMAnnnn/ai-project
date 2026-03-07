package model.cards;

import org.json.JSONObject;

//Represent a number card, which work as a number when played.
public class NumberCard extends Card {
    private int value;

    // EFFECTS: Construct a NumberCard with value, type and description.
    public NumberCard(int value) {
        super(String.valueOf(value), CardType.NUMBER, "A number card with value " + value + ".");
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    // EFFECTS: converts a NumberCard into a JSON object
    @Override
    public JSONObject toJson() {
        JSONObject json = super.toJson();
        json.put("value", value);
        return json;
    }
    
}