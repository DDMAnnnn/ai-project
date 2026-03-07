package model.cardstest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import model.cards.CardType;
import model.cards.SpecialCard;

public class SpecCardTest {
    private SpecialCard testSpecCard;

    @BeforeEach
    void runBefore() {
        testSpecCard = new SpecialCard("merge");
    }

    @Test
    void testConstructor() {
        assertEquals(testSpecCard.getName(), "merge");
        assertEquals(testSpecCard.getCardType(), CardType.SPECIAL);
        assertEquals(testSpecCard.getDescription(), "Special card: merge");
    }

}
