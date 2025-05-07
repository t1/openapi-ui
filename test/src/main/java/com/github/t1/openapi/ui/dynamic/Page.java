package com.github.t1.openapi.ui.dynamic;

import com.github.t1.openapi.ui.core.OpenApiUiGenerator;
import com.github.t1.openapi.ui.core.OpenApiUiPages;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.models.OpenAPI;

import java.net.URI;

import static jakarta.ws.rs.core.MediaType.TEXT_HTML;

/// The [OpenAPI Spec suggests](https://download.eclipse.org/microprofile/microprofile-open-api-4.0/microprofile-openapi-spec-4.0.html#_user_interface)
/// that an OpenAPI UI should be available at `/openapi/ui`, but at least WildFly returns the OpenAPI document also for sub-paths of `/openapi`.
@Path("/openapi/ui") // WILDFLY: this works as is only because the `@ApplicationPath` is `/api`
@Produces(TEXT_HTML)
public class Page {
    // TODO is there a better source for the OpenAPI document?
    private final OpenAPI openApi = OpenApiUiGenerator.parse(URI.create(
            "http://localhost:8080/q/openapi.yaml")); // WILDFLY: without the `/q`
    private final OpenApiUiPages page = new OpenApiUiPages(openApi,
            URI.create("/webjars/"), // WILDFLY: add a `/api` prefix
            URI.create("/openapi/ui/snippets")); // WILDFLY: add a `/api` prefix

    @Operation(hidden = true)
    @GET public String index() {return page.index().content();}

    @Operation(hidden = true)
    @Path("/snippets/{path:.*}.html")
    @GET public String snippet(@PathParam("path") String path) {return page.snippet("/" + path).content();}
}
