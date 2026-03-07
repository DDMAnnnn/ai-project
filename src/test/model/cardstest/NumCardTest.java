package model.cardstest;

import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;

import model.cards.CardType;
import model.cards.NumberCard;

public class NumCardTest {
    private NumberCard testNumCard1;
    private NumberCard testNumCard2;
    private NumberCard testNumCard3;
     
    @BeforeEach
    void runBefore() {
        testNumCard1 = new NumberCard(5);
        testNumCard2 = new NumberCard(5);
        testNumCard3 = new NumberCard(6);
    }

    @Test
    void testConstructor() {
        assertEquals(testNumCard1.getValue(), 5);
        assertEquals(testNumCard1.getCardType(), CardType.NUMBER);
        assertEquals(testNumCard1.getDescription(), "A number card with value 5.");
    }

    @Test
    void testcardEquals() {
        assertTrue(testNumCard1.cardEquals(testNumCard2));
        assertFalse(testNumCard1.cardEquals(testNumCard3));
    }

    @Test
    void testToJson() {

        NumberCard card = new NumberCard(1);

        JSONObject json = card.toJson();

        assertEquals(card.getName(), json.getString("name"));
        assertEquals(card.getValue(), json.getInt("value"));
        assertEquals(card.getCardType().toString(), json.getString("type"));
        assertEquals(card.getDescription(), json.getString("description"));
    }
}
