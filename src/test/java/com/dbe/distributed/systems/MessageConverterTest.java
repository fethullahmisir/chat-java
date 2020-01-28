package com.dbe.distributed.systems;

import com.dbe.distributed.systems.shared.messages.BaseMessage;
import com.dbe.distributed.systems.shared.messages.MessageConverter;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class MessageConverterTest {

    @Test
    void testValidMessage() throws JSONException {
        String content = "{" +
                "\"type\": \"MembersMessage\"," +
                "\"members\": [{" +
                "\"address\": \"127.0.0.1\"," +
                "\"port\": 10" +
                "}]" +
                "}";
        BaseMessage membersMessage = MessageConverter.deserialize(content);
        assertEquals("MembersMessage", membersMessage.getType());

        String serializedMembersMessage = MessageConverter.serialize(membersMessage);
        JSONAssert.assertEquals(content, serializedMembersMessage, JSONCompareMode.LENIENT);
    }
}
