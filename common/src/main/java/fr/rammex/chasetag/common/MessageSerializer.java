package fr.rammex.chasetag.common;

import com.google.gson.Gson;

public final class MessageSerializer {

    private static final Gson GSON = new Gson();

    private MessageSerializer() {}

    public static <T> String serialize(T message) {
        return GSON.toJson(message);
    }

    public static <T> T deserialize(String json, Class<T> clazz) {
        return GSON.fromJson(json, clazz);
    }
}