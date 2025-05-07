package com.github.t1.openapi.ui.core;

import com.github.t1.bulmajava.basic.AbstractElement;
import com.github.t1.bulmajava.basic.Renderable;
import com.github.t1.bulmajava.components.Message;
import org.eclipse.microprofile.openapi.models.PathItem;
import org.eclipse.microprofile.openapi.models.Paths;

import java.net.URI;
import java.nio.file.Path;
import java.util.stream.Stream;

import static com.github.t1.bulmajava.basic.Basic.div;
import static com.github.t1.bulmajava.basic.Basic.span;
import static com.github.t1.bulmajava.basic.FontFamily.CODE;
import static com.github.t1.bulmajava.basic.Renderable.ConcatenatedRenderable.concat;
import static com.github.t1.bulmajava.elements.Icon.icon;
import static com.github.t1.bulmajava.elements.MenuActivationType.HOVERABLE;
import static com.github.t1.bulmajava.elements.Tag.tag;
import static com.github.t1.bulmajava.elements.Tag.tagsAddon;
import static com.github.t1.openapi.ui.core.OpenApiUiGenerator.safeJS;
import static com.github.t1.openapi.ui.core.OpenApiUiOperation.CLOSED;
import static com.github.t1.openapi.ui.core.OpenApiUiPages.OpenApiUiFile;

record OpenApiUiPath(URI snippetBase, String pathTemplate, PathItem item) {
    static Stream<OpenApiUiPath> stream(URI snippetBase, Paths paths) {
        return paths.getPathItems().entrySet().stream()
                .map(entry -> new OpenApiUiPath(snippetBase, entry.getKey(), entry.getValue()));
    }

    /// Return the #pathTemplate as a valid CSS selector, without the leading slash.
    ///
    /// @see <a href="https://developer.mozilla.org/en-US/docs/Web/CSS/ident">css ident spec</a>
    String id() {
        return safeJS((pathTemplate.startsWith("/") ? pathTemplate.substring(1) : pathTemplate)
                .replace("/", "-")
                .replace("{", "«")
                .replace("}", "»"));
    }

    Message message() {
        // we can't use a `card`, as we can't properly put tags in a card's header
        return Message.message().id(id()).tabindex(0)
                .on("keyup", """
                        var tags = querySelector('.tags');
                        switch(event.key) {
                            case 'Enter':
                            case ' ':
                                click();
                                tags.firstElementChild.focus();
                                break;
                            case 'ArrowLeft':
                                focus();
                                tags.lastElementChild.click();
                                break;
                            case 'ArrowRight':
                                var firstOp = tags.firstElementChild;
                                firstOp.focus();
                                firstOp.click();
                                break;
                            case 'ArrowUp':
                                previousElementSibling?.focus();
                                break;
                            case 'ArrowDown':
                                nextElementSibling?.focus();
                                break;
                        }
                        """)
                .header(
                        span(pathTemplate).is(CODE),
                        tagsAddon().isPulledRight()
                                .content(operations().map(OpenApiUiOperation::tag))
                                .content(closeTag(false)));
    }

    AbstractElement<?> closeTag(boolean open) {
        var tag = tag().id(id() + "-path-close")
                .content(icon("angle-" + (open ? "down" : "right")));
        if (open) tag.is(HOVERABLE)
                .on("keyup", """
                        switch(event.key) {
                            case 'Enter':
                            case ' ':
                                click();
                                closest('.message').focus();
                                event.stopPropagation();
                                break;
                            case 'ArrowRight':
                                event.stopPropagation();
                                break;
                        }
                        """);
        return tag;
    }

    Stream<OpenApiUiOperation> operations() {return OpenApiUiOperation.stream(this, item.getOperations());}

    /// A div that deletes the body with an oob swap. The same id can be in the same snippet to replace it.
    Renderable deleteBody() {return div().id(id() + "-body").attr("hx-swap-oob", "delete");}

    OpenApiUiFile closedFile() {
        return new OpenApiUiFile(Path.of(pathTemplate + "-" + CLOSED + ".html"), concat(
                closeTag(false).attr("hx-swap-oob", "true"),
                deleteBody()));
    }
}
