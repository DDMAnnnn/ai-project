package persistence;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import java.io.IOException;

class JRTestRewards {

    @Test
    void testReaderInvalidFile() {
        JsonReaderReward reader = new JsonReaderReward("./data/NoSuchFile.json");
        try {
            Rewards rewards = reader.read();
            fail("IOException was expected");
        } catch (IOException e) {
            // pass
        }
    }


    @Test
    void testReaderEmptyRewards() {
        JsonReaderReward reader = new JsonReaderReward("./data/testReaderEmptyRewards.json");
        try {
            
            Rewards rewards = reader.read();
            List<String> strRewards = rewards.getRewards();
            assertEquals(strRewards.size(), 0);

        } catch (IOException e) {
            fail("Exception should not have been thrown");
        }
    }

    @Test
    void testWriterGeneralGameData() {
        JsonReaderReward reader = new JsonReaderReward("./data/testReaderGeneralRewards.json");
        try {
            Rewards rewards = reader.read();
            List<String> strRewards = rewards.getRewards();
            assertEquals(strRewards.size(), 5);
            assertEquals("41", strRewards.get(0));
            assertEquals("+", strRewards.get(1));
            assertEquals("draw", strRewards.get(2));
            assertEquals("skip", strRewards.get(3));
            assertEquals("remove", strRewards.get(4));

        } catch (IOException e) {
            fail("Exception should not have been thrown");
        }
    }

}