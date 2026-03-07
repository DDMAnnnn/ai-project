package model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.*;

import model.cards.*;

public class DeckTest {
    private Deck testDeck;
    List<Card> expectedDeck;
    private Card newCard0 = new NumberCard(0);
    private Card newCard1 = new NumberCard(1);
    private Card newCard2 = new NumberCard(2);
    private Card newCard3 = new NumberCard(3);
    private Card newCard4 = new NumberCard(4);
    private Card newCard5 = new NumberCard(5);
    private Card newCard6 = new NumberCard(6);
    private Card newCard7 = new NumberCard(7);
    private Card newCard8 = new NumberCard(8);
    private Card newCard9 = new NumberCard(9);
    private Card newCardPlus = new SymbolCard('+');
    private Card newCardMinus = new SymbolCard('-');
    private Card newCardMult = new SymbolCard('*');
    private Card newCardDivide = new SymbolCard('/');
    private Card newCardMerge = new SpecialCard("merge");
    private Card newCard = new NumberCard(12);

    @BeforeEach
    void runBefore() {
        testDeck = new Deck(true);
        expectedDeck = new ArrayList<>();
        expectedDeck.add(newCard0);
        expectedDeck.add(newCard1);
        expectedDeck.add(newCard2);
        expectedDeck.add(newCard3);
        expectedDeck.add(newCard4);
        expectedDeck.add(newCard5);
        expectedDeck.add(newCard6);
        expectedDeck.add(newCard7);
        expectedDeck.add(newCard8);
        expectedDeck.add(newCard9);
        expectedDeck.add(newCardPlus);
        expectedDeck.add(newCardMinus);
        expectedDeck.add(newCardMult);
        expectedDeck.add(newCardDivide);
        expectedDeck.add(newCardMerge);

    }
    
    public List<String> convertDeck(List<Card> deck) {
        List<String> strDeck = new ArrayList<>();
        Iterator<Card> iterator = deck.iterator();
        
        while (iterator.hasNext()) {
            Card card = iterator.next();
            strDeck.add(card.getName());
            iterator.remove(); 
        }
        
        return strDeck;
    }


    @Test
    void testConstructor() {   
        List<String> strTestDeck = convertDeck(testDeck.getDeck());
        List<String> strExpectedDeck = convertDeck(expectedDeck);
        Collections.sort(strTestDeck);
        Collections.sort(strExpectedDeck);
        assertEquals(strTestDeck,strExpectedDeck);
    }

    @Test
    void testaddCard() {
        assertEquals(15, testDeck.getSize());
        testDeck.addCard(newCard);
        assertEquals(16, testDeck.getSize());
        assertTrue(testDeck.deckContains(newCard));
    }

    @Test
    void testGetCard() {
        NumberCard card = (NumberCard) testDeck.getCard(1);
        assertEquals(card.getCardType(), CardType.NUMBER);
        assertEquals(card.getDescription(), "A number card with value 1.");
        assertEquals(card.getName(), "1");
        assertEquals(card.getValue(), 1);
    }

    @Test
    public void testToJson() {
        Deck testDeck = new Deck(false);
        NumberCard card1 = new NumberCard(1);
        SymbolCard card2 = new SymbolCard('+');
        testDeck.addCard(card1);
        testDeck.addCard(card2);

        JSONObject json = testDeck.toJson();

        JSONArray jsonDeck = json.getJSONArray("deck");
        assertEquals(2, jsonDeck.length());

        JSONObject jsonCard1 = jsonDeck.getJSONObject(0);
        assertEquals(card1.getName(), jsonCard1.getString("name"));
        assertEquals(card1.getValue(), jsonCard1.getInt("value"));
        assertEquals(card1.getCardType().toString(), jsonCard1.getString("type"));
        assertEquals(card1.getDescription(), jsonCard1.getString("description"));

        JSONObject jsonCard2 = jsonDeck.getJSONObject(1);
        assertEquals(card2.getName(), jsonCard2.getString("name"));
        assertEquals(card2.getSymbol(), jsonCard2.getInt("symbol"));
        assertEquals(card2.getCardType().toString(), jsonCard2.getString("type"));
        assertEquals(card2.getDescription(), jsonCard2.getString("description"));
    }
}