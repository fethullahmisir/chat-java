package com.dbe.distributed.systems.shared.messages;

import com.dbe.distributed.systems.server.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class MessageConverter {
    private static ObjectMapper MAPPER = new ObjectMapper();

    private final static Map<String, Class<?>> TYPES;

    static {
        var map = Map.of(JoinMessage.class.getSimpleName(), JoinMessage.class,
                JoinAckMessage.class.getSimpleName(), JoinAckMessage.class,
                MemberMessage.class.getSimpleName(), MemberMessage.class,
                HealthMessage.class.getSimpleName(), HealthMessage.class,
                ChatMessage.class.getSimpleName(), ChatMessage.class,
                GetMasterMessage.class.getSimpleName(), GetMasterMessage.class,
                GetMasterMessageResponse.class.getSimpleName(), GetMasterMessageResponse.class,
                GetMissingChatMessages.class.getSimpleName(), GetMissingChatMessages.class,
                LeaderElectionMessage.class.getSimpleName(), LeaderElectionMessage.class,
                LeaderElectedMessage.class.getSimpleName(), LeaderElectedMessage.class
        );
        var second = Map.of(SequenceMessage.class.getSimpleName(), SequenceMessage.class);
        var merged = new HashMap<String, Class<?>>();
        merged.putAll(map);
        merged.putAll(second);
        TYPES = Collections.unmodifiableMap(merged);
    }

    public static BaseMessage deserialize(String content) {
        try {
            JsonNode jsonNode = MAPPER.readTree(content);
            JsonNode type = jsonNode.path("type");
            if (type == null || type.textValue() == null || type.textValue().trim().isEmpty()) {
                throw new IllegalArgumentException("Type attribute is required.");
            }
            if (!TYPES.containsKey(type.textValue())) {
                throw new IllegalArgumentException("Type " + type.textValue() + " not known. Known types are: " + TYPES.keySet());
            }
            return (BaseMessage) MAPPER.treeToValue(jsonNode, TYPES.get(type.textValue()));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse value: " + content, e);
        }
    }

    public static String serialize(BaseMessage message) {
        try {
            return MAPPER.writer().writeValueAsString(message);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize message: " + message.getType(), e);
        }
    }
}
