package model.cards;

import org.json.JSONObject;

import persistence.Writable;

//Represent an abstract card, which has 3 different types: number, special and symbol.
public abstract class Card implements Writable {
    private String name;
    private CardType cardType;
    private String description;

    // EFFECTS: Construct a NumberCard with value, type and description.
    public Card(String name, CardType type, String description) {
        this.name = name;
        this.cardType = type;
        this.description = description;
    }

    // getters
    public String getName() {
        return name;
    }

    public CardType getCardType() {
        return cardType;
    }

    public String getDescription() {
        return description;
    }

    // EFFECTS: returns whether 2 cards are equal.
    public boolean cardEquals(Card card) {
        return this.getName().equalsIgnoreCase(card.getName());
    }

    // EFFECTS: converts a Card into a JSON object
    @Override
    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("name", name);
        json.put("type", cardType.toString());
        json.put("description", description);
        return json;
    }
}
