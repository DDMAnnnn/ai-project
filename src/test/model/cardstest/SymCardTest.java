package model.cardstest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import model.cards.CardType;
import model.cards.SymbolCard;

public class SymCardTest {
    private SymbolCard testSymCard;

    @BeforeEach
    void runBefore() {
        testSymCard = new SymbolCard('+');
    }

    @Test
    void testConstructor() {
        assertEquals(testSymCard.getSymbol(), '+');
        assertEquals(testSymCard.getCardType(), CardType.SYMBOL);
        assertEquals(testSymCard.getDescription(), "A symbol card act as +.");
    }

    
    @Test
    void testToJson() {

        SymbolCard card = new SymbolCard('+');

        JSONObject json = card.toJson();

        assertEquals(card.getName(), json.getString("name"));
        assertEquals(card.getSymbol(), json.getInt("symbol"));
        assertEquals(card.getCardType().toString(), json.getString("type"));
        assertEquals(card.getDescription(), json.getString("description"));
    }
}
