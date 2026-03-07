package model;

import java.util.*;
import model.cards.*;
import org.json.JSONArray;
import org.json.JSONObject;
import persistence.Writable;

//Represent a deck that contains the cards, which does not change in a single battle.
public class Deck implements Writable {
    private List<Card> deck;

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            EventLog eventLog = EventLog.getInstance();
            for (Event event : eventLog) {
                System.out.println(event);
            }
        }));
    }

    // Construct a deck, with initial cards in it.
    public Deck(Boolean isStarter) {
        deck = new ArrayList<>();
        if (isStarter) {
            initializeDeck();
        }

    }

    // EFFECTS: initialize the deck with one card each from 0 to 9, one each for
    // +,-,*,/,"merge"
    public void initializeDeck() {
        for (int i = 0; i <= 9; i++) {
            deck.add(new NumberCard(i));
        }

        char[] symbols = { '+', '-', '*', '/' };
        for (char symbol : symbols) {
            deck.add(new SymbolCard(symbol));
        }

        deck.add(new SpecialCard("merge"));
    }

    // getters
    public List<Card> getDeck() {
        return deck;
    }

    public int getSize() {
        return deck.size();
    }

    // EFFECTS: add a card to the desk.
    public void addCard(Card card) {
        deck.add(card);
        EventLog.getInstance().logEvent(new Event("A " + card.getCardType().toString().toLowerCase() 
                + " card is added to the deck."));
    }

    // EFFECTS: check if the deck contains a certain card.
    public boolean deckContains(Card card) {
        return deck.contains(card);
    }

    // EFFECTS: return the card at index in the deck.
    public Card getCard(int index) {
        return deck.get(index);
    }

     // EFFECTS: converts the deck into a JSON object
    @Override
    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("deck", cardsToJson(deck));
        return json;
    }

    // EFFECTS: converts a list of cards to a JSON array
    private JSONArray cardsToJson(List<Card> cards) {
        JSONArray jsonArray = new JSONArray();
        for (Card card : cards) {
            jsonArray.put(card.toJson());
        }
        return jsonArray;
    }
}
