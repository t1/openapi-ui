package com.github.t1.openapi.ui.core;

import org.eclipse.microprofile.openapi.models.media.Schema;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.joining;

record OpenApiUiExample(
        Schema schema,
        int indent,
        // we pass this to nested examples to deterministically produce different values
        AtomicInteger nextValue) {
    public OpenApiUiExample(Schema schema) {this(schema, 0, new AtomicInteger(100));}

    private OpenApiUiExample nested(Schema value) {return new OpenApiUiExample(value, indent + 1, nextValue);}

    private String in(int offset) {return " ".repeat((indent + offset) * 2);}

    private int next() {return nextValue.getAndIncrement();}

    @Override public String toString() {
        if (schema.getExamples() != null && !schema.getExamples().isEmpty()) {
            var example = schema.getExamples().getFirst();
            if (example instanceof String string) return quoted(string);
            return Objects.toString(example);
        }
        if (schema.getType() == null) return "null";
        var type = schema.getType().getFirst(); // TODO why can there be multiple types?
        return switch (type) {
            case INTEGER -> Integer.toString(next());
            case NUMBER -> Double.toString(((double) next()) / 10);
            case BOOLEAN -> Boolean.toString(next() % 2 == 1);
            case STRING -> quoted("s" + next());
            case OBJECT -> toJsonObject(schema.getProperties());
            case ARRAY -> toJsonArray(schema.getItems());
            case NULL -> "null";
        };
    }

    private static String quoted(Object string) {
        return "\"" + string.toString().replace("\"", "\\\"") + "\"";
    }

    private String toJsonObject(Map<String, Schema> properties) {
        return properties.entrySet().stream()
                .map(e -> in(+1) + '"' +
                          e.getKey() + "\": " + nested(e.getValue()))
                .collect(joining(",\n", "{\n", "\n" + in(+0) + "}"));
    }

    private String toJsonArray(Schema items) {
        return IntStream.range(0, 3)
                .mapToObj(_ -> in(+1) + nested(items))
                .collect(joining(",\n", "[\n", "\n" + in(+0) + "]"));
    }
}
