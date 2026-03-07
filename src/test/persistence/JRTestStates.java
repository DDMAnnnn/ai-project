package persistence;

import org.junit.jupiter.api.Test;

import ui.GameState;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;

class JRTestStates {

    @Test
    void testReaderInvalidFile() {
        JsonReaderState reader = new JsonReaderState("./data/NoSuchFile.json");
        try {
            States states = reader.read();
            fail("IOException was expected");
        } catch (IOException e) {
            // pass
        }
    }

    @Test
    void testReaderStates() {
        JsonReaderState reader = new JsonReaderState("./data/testReaderState.json");
        try {

            States states = reader.read();
            assertEquals(states.getState(), GameState.BATTLE);

        } catch (IOException e) {
            fail("Exception should not have been thrown");
        }
    }

}