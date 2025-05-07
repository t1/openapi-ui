package com.github.t1.openapi.ui.core;

import org.eclipse.microprofile.openapi.models.media.Schema;
import org.eclipse.microprofile.openapi.models.media.Schema.SchemaType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.BDDAssertions.then;
import static org.eclipse.microprofile.openapi.OASFactory.createSchema;
import static org.eclipse.microprofile.openapi.models.media.Schema.SchemaType.ARRAY;
import static org.eclipse.microprofile.openapi.models.media.Schema.SchemaType.BOOLEAN;
import static org.eclipse.microprofile.openapi.models.media.Schema.SchemaType.INTEGER;
import static org.eclipse.microprofile.openapi.models.media.Schema.SchemaType.NULL;
import static org.eclipse.microprofile.openapi.models.media.Schema.SchemaType.NUMBER;
import static org.eclipse.microprofile.openapi.models.media.Schema.SchemaType.OBJECT;
import static org.eclipse.microprofile.openapi.models.media.Schema.SchemaType.STRING;

class OpenApiUiExampleTest {
    @Test void shouldBuildNull() {
        var schema = create(NULL);

        var example = new OpenApiUiExample(schema);

        then(example).hasToString("null");
    }

    @Test void shouldBuildInteger() {
        var schema = create(INTEGER);

        var example = new OpenApiUiExample(schema);

        then(example).hasToString("100");
    }

    @Test void shouldBuildIntegerExample() {
        var schema = create(INTEGER).addExample(123);

        var example = new OpenApiUiExample(schema);

        then(example).hasToString("123");
    }

    @Test void shouldBuildNumber() {
        var schema = create(NUMBER);

        var example = new OpenApiUiExample(schema);

        then(example).hasToString("10.0");
    }

    @Test void shouldBuildNumberExample() {
        var schema = create(NUMBER).addExample(123.45);

        var example = new OpenApiUiExample(schema);

        then(example).hasToString("123.45");
    }

    @Test void shouldBuildBoolean() {
        var schema = create(BOOLEAN);

        var example = new OpenApiUiExample(schema);

        then(example).hasToString("false");
    }

    @Test void shouldBuildBooleanExample() {
        var schema = create(BOOLEAN).addExample(true);

        var example = new OpenApiUiExample(schema);

        then(example).hasToString("true");
    }

    @Test void shouldBuildString() {
        var schema = create(STRING);

        var example = new OpenApiUiExample(schema);

        then(example).hasToString("\"s100\"");
    }

    @Test void shouldBuildStringExample() {
        var schema = create(STRING).addExample("foobar");

        var example = new OpenApiUiExample(schema);

        then(example).hasToString("\"foobar\"");
    }

    @Test void shouldBuildStringExampleWithQuotes() {
        var schema = create(STRING).addExample("foo\"bar");

        var example = new OpenApiUiExample(schema);

        then(example).hasToString("\"foo\\\"bar\"");
    }

    @Test void shouldBuildObject() {
        var schema = create(OBJECT).addProperty("foo", create(STRING));

        var example = new OpenApiUiExample(schema);

        then(example).hasToString("""
                {
                  "foo": "s100"
                }""");
    }

    @Test void shouldBuildArray() {
        var schema = create(ARRAY).items(create(STRING));

        var example = new OpenApiUiExample(schema);

        then(example).hasToString("""
                [
                  "s100",
                  "s101",
                  "s102"
                ]""");
    }

    @Test void shouldBuildArrayOfObjects() {
        var schema = create(ARRAY).items(create(OBJECT)
                .addProperty("foo", create(STRING))
                .addProperty("bar", create(NUMBER)));

        var example = new OpenApiUiExample(schema);

        then(example).hasToString("""
                [
                  {
                    "foo": "s100",
                    "bar": 10.1
                  },
                  {
                    "foo": "s102",
                    "bar": 10.3
                  },
                  {
                    "foo": "s104",
                    "bar": 10.5
                  }
                ]""");
    }

    private static Schema create(SchemaType schemaType) {return createSchema().addType(schemaType);}
}
