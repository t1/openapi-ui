package com.github.t1.openapi.ui.core;

import com.github.t1.openapi.ui.core.OpenApiUiPages.OpenApiUiFile;
import org.eclipse.microprofile.openapi.models.OpenAPI;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.charset.StandardCharsets.UTF_8;

public record OpenApiUiGenerator(Path root) {
    public static void main(String[] args) {
        var generator = new OpenApiUiGenerator(Path.of("dist"));
        var openApi = parse(generator.root.resolve("openapi.yaml").toUri());
        new OpenApiUiPages(openApi, URI.create("."), URI.create("."))
                .files()
                .forEach(generator::write);
    }

    /// Makes using an unsafe string safe as a string in JavaScript by replacing quotes and newlines with `-`.
    public static String safeJS(String js) {
        return js.replace("'", "-")
                .replace("\"", "-")
                .replace("`", "-")
                .replace("\n", "-")
                .replace("\r", "");
    }

    public static String readResource(String resourcePath) {
        // we read a resource from the classpath, not from the application itself, so we must use the class loader
        // and not prefix the path with '/'
        try (var stream = OpenApiUiGenerator.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (stream == null) throw new RuntimeException("can't find resource " + resourcePath);
            return new String(stream.readAllBytes(), UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static OpenAPI parse(URI uri) {
        // we don't want to have a compile-time dependency on the Smallrye OpenApiParser
        try {
            var cls = Class.forName("io.smallrye.openapi.runtime.io.OpenApiParser");
            var method = cls.getMethod("parse", URL.class);
            return (OpenAPI) method.invoke(null, uri.toURL());
        } catch (IOException | ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private void write(OpenApiUiFile file) {
        var path = file.relativeTo(root);
        try {
            System.out.println("writing " + file.path() + " to " + path);
            Files.createDirectories(path.getParent());
            Files.writeString(path, file.content());
        } catch (IOException e) {
            throw new RuntimeException("can't write " + file.path() + " to " + path, e);
        }
    }
}
