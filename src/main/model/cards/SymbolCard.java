package model.cards;

import org.json.JSONObject;

//Represent a symbol card, which work as a symbol when played.
public class SymbolCard extends Card {
    private char symbol;

    // EFFECTS: Construct a SymbolCard with name, type and description.
    public SymbolCard(char symbol) {
        super(String.valueOf(symbol), CardType.SYMBOL, "A symbol card act as " + symbol + ".");
        this.symbol = symbol;

    }

    // getter
    public char getSymbol() {
        return symbol;
    }

    // EFFECTS: converts a SymbolCard into a JSON object
    @Override
    public JSONObject toJson() {
        JSONObject json = super.toJson();
        json.put("symbol", symbol);
        return json;
    }
}
