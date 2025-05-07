package com.github.t1.openapi.ui.app;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import org.eclipse.microprofile.openapi.annotations.ExternalDocumentation;
import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.extensions.Extension;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.eclipse.microprofile.openapi.annotations.servers.Server;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@OpenAPIDefinition(
        info = @Info(
                version = "1.0.0",
                title = "Demo Product API for the OpenAPI UI"),
        servers = {
                // TODO server descriptions can contain Markdown!
                @Server(url = "http://localhost:8080"),
                @Server(description = "qa", url = "https://qa.example.com"),
                @Server(description = "prod", url = "https://api.example.com"),
        },
        tags = {
                @Tag(name = "product", description = "Product operations"),
                @Tag(name = "reading", description = "Side-effect free operations (except for logging/monitoring/etc.)",
                        externalDocs = @ExternalDocumentation(description = "More about reading", url = "https://example.com")),
                @Tag(name = "writing", description = "Operations that change some data",
                        extensions = {
                                @Extension(name = "x-writing", value = "true"),
                                @Extension(name = "x-read-only", value = "false"),
                        }),
        })
@ApplicationPath("/") // WILDFLY: this must be "/api", so the static resources are served from "/"
public class Rest extends Application {
}
