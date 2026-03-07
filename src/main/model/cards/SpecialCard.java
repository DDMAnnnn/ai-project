package model.cards;

//Represent a number card, which has a special functions= when played.
public class SpecialCard extends Card {
    
    // EFFECTS: Construct a SpecialCard with name, type and description.
    public SpecialCard(String name) {
        super(name, CardType.SPECIAL, "Special card: " + name);
    }

}