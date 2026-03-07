package persistence;

import org.junit.jupiter.api.Test;

import ui.GameState;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;

class JWTestStates {

    @Test
    void testWriterInvalidFileStates() {
        try {
            States states = new States(GameState.INITIAL);
            JsonWriter writer = new JsonWriter("./data/my\0illegal:fileName.json");
            writer.open();
            fail("IOException was expected");
        } catch (IOException e) {
            // pass
        }
    }

    @Test
    void testWriterStates() {
        try {
            States states = new States(GameState.BATTLE);
            JsonWriter writer = new JsonWriter("./data/testWriterState.json");
            writer.open();
            writer.write(states);
            writer.close();

            JsonReaderState reader = new JsonReaderState("./data/testWriterState.json");
            states = reader.read();
            assertEquals(states.getState(), GameState.BATTLE);

        } catch (IOException e) {
            fail("Exception should not have been thrown");
        }
    }

}