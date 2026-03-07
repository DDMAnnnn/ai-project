package persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

import java.util.*;
import java.io.IOException;

public class JWTestRewards {

    @Test
    void testWriterInvalidFileReward() {
        try {
            List<String> emptyList = new ArrayList<>();
            Rewards rewards = new Rewards(emptyList);
            JsonWriter writer = new JsonWriter("./data/my\0illegal:fileName.json");
            writer.open();
            fail("IOException was expected");
        } catch (IOException e) {
            // pass
        }
    }

    @Test
    void testWriterEmptyRewards() {
        try {
            List<String> emptyList = new ArrayList<>();
            Rewards rewards = new Rewards(emptyList);
            JsonWriter writer = new JsonWriter("./data/testWriterEmptyRewards.json");
            writer.open();
            writer.write(rewards);
            writer.close();

            JsonReaderReward reader = new JsonReaderReward("./data/testWriterEmptyRewards.json");
            rewards = reader.read();
            List<String> strRewards = rewards.getRewards();
            assertEquals(strRewards.size(), 0);


        } catch (IOException e) {
            fail("Exception should not have been thrown");
        }
    }

    @Test
    void testWriterGeneralRewards() {
        try {
            List<String> list = new ArrayList<>();
            list.add("41");
            list.add("+");
            list.add("draw");
            list.add("skip");
            list.add("remove");
            
            Rewards rewards = new Rewards(list);
            JsonWriter writer = new JsonWriter("./data/testWriterGeneralRewards.json");
            writer.open();
            writer.write(rewards);
            writer.close();

            JsonReaderReward reader = new JsonReaderReward("./data/testWriterGeneralRewards.json");
            rewards = reader.read();
            assertEquals(rewards.getRewards().size(), 5);
            assertEquals("41", rewards.getRewards().get(0));
            assertEquals("+", rewards.getRewards().get(1));
            assertEquals("draw", rewards.getRewards().get(2));
            assertEquals("skip", rewards.getRewards().get(3));
            assertEquals("remove", rewards.getRewards().get(4));

        } catch (IOException e) {
            fail("Exception should not have been thrown");
        }
    }

}
