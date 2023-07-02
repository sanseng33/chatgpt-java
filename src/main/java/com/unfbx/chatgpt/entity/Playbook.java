package com.unfbx.chatgpt.entity;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

public enum Playbook {

    SmartSearch,
    NoveltySearch;

    public static class PlaybookSerializer extends StdSerializer<Playbook> {

        public PlaybookSerializer() {
            super(Playbook.class);
        }

        @Override
        public void serialize(Playbook value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeString(value.name());
        }
    }
}
