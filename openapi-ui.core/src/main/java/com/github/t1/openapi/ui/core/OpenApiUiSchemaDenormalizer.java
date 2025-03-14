package com.github.t1.openapi.ui.core;

import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.PathItem;
import org.eclipse.microprofile.openapi.models.media.Schema;

import java.util.Optional;

/// replace all references in the OpenAPI document with the actual components;
/// currently only the schema refs in the requestBody and arrays.
class OpenApiUiSchemaDenormalizer {
    private final OpenAPI openApi;

    public OpenApiUiSchemaDenormalizer(OpenAPI openApi) {this.openApi = openApi;}

    public void denormalize() {
        openApi.getPaths().getPathItems().forEach((_, item) -> denormalize(item));
        openApi.getComponents().getSchemas().forEach((_, component) -> denormalize(component));
    }


    private void denormalize(PathItem item) {
        item.getOperations().forEach((_, operation)
                -> Optional.ofNullable(operation.getRequestBody()).ifPresent(requestBody
                -> requestBody.getContent().getMediaTypes().forEach((_, mediaType)
                -> Optional.ofNullable(mediaType.getSchema()).flatMap(schema
                -> Optional.ofNullable(schema.getRef())).ifPresent(ref
                -> mediaType.setSchema(deref(ref))))));
    }

    private void denormalize(Schema component) {
        if (component.getType() == null) return;
        switch (component.getType().getFirst()) {
            case OBJECT -> {
                if (component.getProperties() != null) {
                    component.getProperties().forEach((_, property) -> denormalize(property));
                }
            }
            case ARRAY -> {
                if (component.getItems().getRef() != null) {
                    component.setItems(deref(component.getItems().getRef()));
                }
            }
            case null, default -> {}
        }
    }

    private Schema deref(String ref) {
        assert ref.startsWith("#/components/schemas/");
        var schemaName = ref.substring(21);
        var refSchema = openApi.getComponents().getSchemas().get(schemaName);
        if (refSchema == null) throw new IllegalStateException("Referenced schema not found: " + schemaName);
        return refSchema;
    }
}
