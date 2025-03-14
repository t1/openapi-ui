package com.github.t1.openapi.ui.core;

import com.github.t1.bulmajava.basic.AbstractElement;
import com.github.t1.bulmajava.basic.Basic;
import com.github.t1.bulmajava.basic.Renderable;
import com.github.t1.bulmajava.components.Message;
import com.github.t1.bulmajava.elements.Title;
import com.github.t1.bulmajava.form.Select;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.servers.Server;

import java.net.URI;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Stream;

import static com.github.t1.bulmajava.basic.Body.body;
import static com.github.t1.bulmajava.basic.Html.html;
import static com.github.t1.bulmajava.columns.Column.column;
import static com.github.t1.bulmajava.columns.Columns.columns;
import static com.github.t1.bulmajava.elements.Title.subtitle;
import static com.github.t1.bulmajava.form.Select.select;
import static com.github.t1.bulmajava.layout.Section.section;
import static com.github.t1.openapi.ui.core.OpenApiUiGenerator.readResource;
import static com.github.t1.openapi.ui.core.OpenApiUiOperation.CLOSED;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.joining;

/// Produces all the HTML {@link #files()} for the OpenAPI UI.
/// You can also get the {@link #index()} and every {@link #snippet(String)} individually.
///
/// @param openApi      the OpenAPI document to render
/// @param resourceBase the base URI for the resources (stylesheets, scripts)
/// @param snippetBase  the base URI for the snippets (html files)
public record OpenApiUiPages(OpenAPI openApi, URI resourceBase, URI snippetBase) {
    public OpenApiUiPages {
        new OpenApiUiSchemaDenormalizer(openApi).denormalize();
    }

    public record OpenApiUiFile(Path path, String content) {
        OpenApiUiFile(Path path, Renderable renderable) {this(path, renderable.render());}

        Path relativeTo(Path root) {
            var fileName = path.toString();
            if (fileName.startsWith("/")) fileName = fileName.substring(1);
            return root.resolve(fileName);
        }
    }

    public Stream<OpenApiUiFile> files() {
        return Stream.of(
                        Stream.of(index()),
                        paths().flatMap(OpenApiUiPath::operations).map(OpenApiUiPages::details),
                        paths().map(OpenApiUiPath::closedFile))
                .flatMap(identity());
    }

    public OpenApiUiFile index() {
        //noinspection JSUnusedLocalSymbols
        return new OpenApiUiFile(Path.of("index.html"), html(openApi.getInfo().getTitle())
                .stylesheet(resourceBase.resolve("bulma/css/bulma.css"))
                .stylesheet(resourceBase.resolve("fortawesome__fontawesome-free/css/all.css"))
                // TODO improve bulma-java
                .head(Basic.element("style").content(readResource("openapi-ui.css")))
                .script(resourceBase.resolve("htmx.org/dist/htmx.js"))
                .script(resourceBase.resolve("htmx-ext-debug/debug.js"))
                .javaScriptCode(CliCodeType.stream()
                        .map(type -> "function " + type.methodName() + "(method, url, id) {\n" + type.command() + "}")
                        .collect(joining("\n\n")))
                .javaScriptCode(readResource("openapi-ui.js"))
                .content(body().content(section()
                        //.attr("hx-ext", "debug")
                        .content(indexInfo())
                        .content(indexPaths()))));
    }

    private Renderable indexInfo() {
        var columns = columns();
        columns.column(Title.title(openApi.getInfo().getTitle()),
                subtitle("version: " + openApi.getInfo().getVersion()));
        if (hasServers()) columns.content(column().narrow().content(servers()));
        return columns;
    }

    private Stream<Message> indexPaths() {
        return paths().map(OpenApiUiPath::message)
                .gather(new FirstGatherer<>(AbstractElement::autofocus));
    }

    private boolean hasServers() {return openApi.getServers() != null && !openApi.getServers().isEmpty();}

    private Select servers() {
        var select = select("servers");
        openApi.getServers().forEach(server -> select.option(server.getUrl(), getDescription(server)));
        return select;
    }

    private static String getDescription(Server server) {
        var description = server.getDescription();
        if (description == null || description.isBlank()) description = server.getUrl();
        return description;
    }

    public OpenApiUiFile snippet(String path) {
        return closedSnippet(path)
                .or(() -> operationSnippet(path))
                .orElseThrow();
    }

    private Optional<OpenApiUiFile> closedSnippet(String path) {
        return paths().filter(p -> path.equals(p.pathTemplate() + "-" + CLOSED))
                .findFirst()
                .map(OpenApiUiPath::closedFile);
    }

    private Optional<OpenApiUiFile> operationSnippet(String path) {
        return paths().flatMap(OpenApiUiPath::operations)
                .filter(op -> op.snippetName().equals(path))
                .findFirst()
                .map(OpenApiUiPages::details);
    }

    private Stream<OpenApiUiPath> paths() {return OpenApiUiPath.stream(snippetBase, openApi.getPaths());}

    private static OpenApiUiFile details(OpenApiUiOperation operation) {
        return new OpenApiUiFile(Path.of(operation.snippetPath()), operation.detailsMessageBody());
    }
}
