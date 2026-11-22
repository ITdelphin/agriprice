package com.agriprice.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import java.sql.Date;

/** Shared JSON serialization rules for API response types. */
public final class JsonFactory {
    private JsonFactory() { }

    public static Gson create() {
        JsonSerializer<Date> sqlDateSerializer = (date, type, context) ->
                date == null ? null : new JsonPrimitive(date.toLocalDate().toString());
        return new GsonBuilder()
                .registerTypeAdapter(Date.class, sqlDateSerializer)
                .create();
    }
}
