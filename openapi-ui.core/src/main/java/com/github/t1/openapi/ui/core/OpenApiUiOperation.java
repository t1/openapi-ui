package com.github.t1.openapi.ui.core;

import com.github.t1.bulmajava.basic.Color;
import com.github.t1.bulmajava.basic.Element;
import com.github.t1.bulmajava.basic.Renderable;
import com.github.t1.bulmajava.elements.Block;
import com.github.t1.bulmajava.elements.Button;
import com.github.t1.bulmajava.elements.Tag;
import com.github.t1.bulmajava.elements.Title;
import com.github.t1.bulmajava.form.Select;
import org.eclipse.microprofile.openapi.models.Operation;
import org.eclipse.microprofile.openapi.models.media.Content;
import org.eclipse.microprofile.openapi.models.parameters.Parameter;
import org.eclipse.microprofile.openapi.models.parameters.RequestBody;
import org.eclipse.microprofile.openapi.models.responses.APIResponse;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.github.t1.bulmajava.basic.Basic.div;
import static com.github.t1.bulmajava.basic.Basic.nbsp;
import static com.github.t1.bulmajava.basic.FontFamily.CODE;
import static com.github.t1.bulmajava.basic.FontWeight.BOLD;
import static com.github.t1.bulmajava.basic.Renderable.ConcatenatedRenderable.concat;
import static com.github.t1.bulmajava.basic.Size.NORMAL;
import static com.github.t1.bulmajava.basic.Size.SMALL;
import static com.github.t1.bulmajava.basic.Style.BLACK;
import static com.github.t1.bulmajava.basic.Style.DARK;
import static com.github.t1.bulmajava.components.Message.messageBody;
import static com.github.t1.bulmajava.elements.Block.block;
import static com.github.t1.bulmajava.elements.Button.buttonsAddon;
import static com.github.t1.bulmajava.elements.MenuActivationType.HOVERABLE;
import static com.github.t1.bulmajava.elements.Title.title;
import static com.github.t1.bulmajava.form.Field.field;
import static com.github.t1.bulmajava.form.Input.input;
import static com.github.t1.bulmajava.form.InputType.TEXT;
import static com.github.t1.bulmajava.form.Select.select;
import static com.github.t1.bulmajava.form.Textarea.textarea;
import static com.github.t1.bulmajava.layout.Level.level;
import static com.github.t1.openapi.ui.core.OpenApiUiGenerator.safeJS;
import static java.util.Comparator.comparing;
import static java.util.Locale.ROOT;
import static org.eclipse.microprofile.openapi.models.PathItem.HttpMethod;
import static org.eclipse.microprofile.openapi.models.PathItem.HttpMethod.GET;
import static org.eclipse.microprofile.openapi.models.parameters.Parameter.In.PATH;

@SuppressWarnings("JSDeprecatedSymbols")
record OpenApiUiOperation(OpenApiUiPath path, Method method, Operation operation) {
    static final String CLOSED = "closed";

    private static final int TITLE_LEVEL = 6;

    static Stream<OpenApiUiOperation> stream(OpenApiUiPath path, Map<HttpMethod, Operation> operations) {
        return operations.entrySet().stream()
                .map(entry -> new OpenApiUiOperation(path, new Method(entry.getKey()), entry.getValue()))
                .sorted(comparing(OpenApiUiOperation::method));
    }

    Tag tag() {
        // tabs would normally be a better choice, but we want them to have colors
        return method.tag().id(path.id() + "-" + method.key())
                .is(HOVERABLE).tabindex(0)
                .on("keyup", """
                        switch(event.key) {
                            case 'Enter':
                            case ' ':
                                click();
                                event.stopPropagation();
                                break;
                            case 'ArrowLeft':
                                if (previousElementSibling) {
                                    previousElementSibling.focus();
                                    previousElementSibling.click();
                                    event.stopPropagation();
                                }
                                break;
                            case 'ArrowRight':
                                if (nextElementSibling && !nextElementSibling.id.endsWith('-close')) {
                                    nextElementSibling.focus();
                                    nextElementSibling.click();
                                }
                                event.stopPropagation();
                                break;
                        }
                        """)
                .attr("hx-get", snippetPath())
                .attr("hx-target", "#" + path.id())
                .attr("hx-swap", "beforeend transition:true");
    }

    String snippetPath() {return snippetPath(method.name());}

    String snippetPath(String op) {return path.snippetBase() + snippetName(op) + ".html";}

    String snippetName() {return snippetName(method.name());}

    String snippetName(String op) {return path.pathTemplate() + "-" + op;}

    Renderable detailsMessageBody() {
        return concat(
                path.closeTag(true)
                        .attr("hx-get", snippetPath(CLOSED)) // when clicked, replace
                        .attr("hx-target", "#" + path.id() + "-path-close")
                        .attr("hx-swap-oob", "true"), // but replace with this directly on load
                path.deleteBody(), // delete the old body, if it exists
                messageBody().id(path.id() + "-body")
                        .attr("hx-on:htmx:load", loadAllFieldValues())
                        .content(details()));
    }

    /// We store the values of the path parameters in local storage and restore them when the path body is loaded.
    private String loadAllFieldValues() {
        return "{" +
               parametersIn(PATH)
                       .map(parameter -> inputValue(parameter) + " = localStorage.getItem('" + storageKey(parameter) + "');")
                       .collect(Collectors.joining(" "))
               + requestBodyInit()
               + "}";
    }

    private String inputValue(Parameter parameter) {
        return "document.querySelector('input[name=" + pathParamName(parameter) + "]').value";
    }

    private String requestBodyInit() {
        if (operation.getRequestBody() == null) return "";
        return "var textArea = document.getElementById('" + path.id() + "-request-body" + "');" +
               "var stored = localStorage.getItem(" + requestStoreName() + ");" +
               "textArea.value = stored ? stored : textArea.dataset.example;";
    }

    private Stream<Block> details() {
        return Stream.of(
                synopsis(),
                pathParameters(),
                requestBody(),
                actions());
    }

    private Block synopsis() {
        var block = block().id(path.id() + "-synopsis");
        Optional.ofNullable(operation.getSummary())
                .ifPresent(content -> block.content(title(TITLE_LEVEL, content)));
        Optional.ofNullable(operation.getDescription())
                .ifPresent(description -> block.content(Stream.of(description.split("\n"))
                        .map(text -> Title.subtitle(TITLE_LEVEL, text))));
        return block;
    }

    private Block pathParameters() {
        if (parametersIn(PATH).findAny().isEmpty()) return null;
        return block().id(path.id() + "-path-parameters").content(
                title(TITLE_LEVEL, "Path Parameters"),
                div().content(parametersIn(PATH).map(this::pathParameter)));
    }

    private Stream<Parameter> parametersIn(@SuppressWarnings("SameParameterValue") Parameter.In in) {
        return Optional.ofNullable(operation.getParameters()).stream().flatMap(Collection::stream)
                .filter(parameter -> parameter.getIn() == in);
    }

    private Renderable pathParameter(Parameter parameter) {
        // horizontal would be nice, but that's complex in bulma-java, esp. if you want to have a help.
        // should be fixed there
        var field = field().label(safeName(parameter), NORMAL, CODE)
                .control(input(TEXT).is(CODE).name(pathParamName(parameter))
                        .required(/*TODO add boolean to bulma-java => parameter.getRequired()*/)
                        .on("keyup", "{" +
                                     "localStorage.setItem('" + storageKey(parameter) + "', event.target.value);" +
                                     "switch(event.key) {" +
                                     "  case 'Enter':" +
                                     "    document.getElementById('" + path.id() + "-call-button').click();" +
                                     "    event.stopPropagation();" +
                                     "    break;" +
                                     "  case 'ArrowUp':" +
                                     "  case 'ArrowDown':" +
                                     "  case 'ArrowLeft':" +
                                     "  case 'ArrowRight':" +
                                     "    event.stopPropagation();" +
                                     "    break;" +
                                     "}" +
                                     "}"));
        Optional.ofNullable(parameter.getDescription()).ifPresent(field::help);
        return field;
    }

    private static String safeName(Parameter parameter) {return safeJS(parameter.getName());}

    private String pathParamName(Parameter parameter) {return path.id() + "-param-" + safeName(parameter);}

    private static String storageKey(Parameter parameter) {return "OpenApiUi:path-param:" + safeName(parameter);}

    private Block requestBody() {
        var requestBody = operation.getRequestBody();
        if (requestBody == null) return null;
        return block().id(path.id() + "-body").content(
                level()
                        .left(title(TITLE_LEVEL, "Request Body"))
                        .right(requestContentTypes(requestBody)),
                textarea().id(path.id() + "-request-body")
                        .is(CODE)
                        // TODO can this move to bulma-java?
                        .attr("required")
                        .rows(10)
                        .attr("data-example", requestBodyExample(requestBody))
                        .on("keyup", "localStorage.setItem(" + requestStoreName() + ", event.target.value); " +
                                     "event.stopPropagation();"));
    }

    private String requestStoreName() {
        // TODO if we could find out what ref name the operation has, we could use that for the store name.
        //  Then all `Product` bodies would be the same.
        return "'OpenApiUi:request-body:" + path.id() + "'";
    }

    private Select requestContentTypes(RequestBody requestBody) {
        var select = select(path.id() + "-request-content-types")
                .on("keyup", """
                        switch(event.key) {
                            case 'ArrowLeft':
                            case 'ArrowRight':
                                event.stopPropagation();
                                break;
                        }
                        """);
        // TODO can this be pushed into bulma-java?
        select.contentAs(Element.class).is(CODE).attr("required");
        mediaTypes(requestBody.getContent()).forEach(select::option);
        return select;
    }

    private String requestBodyExample(RequestBody requestBody) {
        var mediaType = mediaTypes(requestBody.getContent()).getFirst();
        var schema = requestBody.getContent().getMediaType(mediaType).getSchema();
        return new OpenApiUiExample(schema).toString();
    }

    private Block actions() {
        return block().content(level()
                .left(acceptMediaTypes())
                .right(actionButtons()));
    }

    private Select acceptMediaTypes() {
        return successResponse().map(apiResponse -> {
            var select = select(path.id() + "-accept-media-types").attr("required")
                    .on("keyup", """
                            switch(event.key) {
                                case 'Enter':
                                case ' ':
                                    document.getElementById('__CALL_BUTTON__').click();
                                    event.stopPropagation();
                                    break;
                                case 'ArrowUp':
                                    event.stopPropagation();
                                    break;
                                case 'ArrowDown':
                                    event.stopPropagation();
                                    break;
                                case 'ArrowLeft':
                                    event.stopPropagation();
                                    break;
                                case 'ArrowRight':
                                    document.getElementById('__CALL_BUTTON__').focus();
                                    event.stopPropagation();
                                    break;
                            }
                            """.replace("__CALL_BUTTON__", path.id() + "-call-button"));
            var mediaTypes = mediaTypes(apiResponse.getContent());
            select.multiple(mediaTypes.size());
            // TODO can this be pushed into bulma-java?
            select.contentAs(Element.class).is(CODE).attr("required");
            var first = true;
            for (var mediaType : mediaTypes) {
                select.option(mediaType);
                if (first) {
                    select.selected();
                    first = false;
                }
            }
            return select;
        }).orElse(null);
    }

    private static List<String> mediaTypes(Content content) {
        return (content == null) ? List.of("*/*") : content.getMediaTypes().keySet().stream().sorted().toList();
    }

    private Optional<APIResponse> successResponse() {
        var responses = operation.getResponses().getAPIResponses();
        if (responses == null || responses.isEmpty()) return Optional.empty();
        var firstCode = responses.keySet().stream().sorted().toList().getFirst();
        return firstCode.startsWith("2") ? Optional.of(responses.get(firstCode)) : Optional.empty();
    }

    private Element actionButtons() {
        var buttons = buttonsAddon();
        buttons.content(callButton());
        if (method.is(GET)) buttons.content(openInNewTabButton());
        buttons.content(CliCodeType.stream().map(this::cliCodeButton));
        return buttons;
    }

    private Button callButton() {
        return actionButton().id(path.id() + "-call-button").is(BLACK)
                .on("click", "{" +
                             "    let startTime = performance.now();\n" +
                             "    fetch(" + urlExpression() + ", {" +
                             "        method: '" + method.name() + "'," +
                             "        headers: {" +
                             "            Accept: acceptHeader('" + path.id() + "-accept-media-types')" +
                             ((operation.getRequestBody() == null) ? "" :
                                     ",   'Content-Type': document.querySelector('select[name=" + path.id() + "-request-content-types]').value") +
                             "        }," +
                             ((operation.getRequestBody() == null) ? "" :
                                     "body: document.getElementById('" + path.id() + "-request-body').value") +
                             "    }).then(response => {" +
                             "        handleTryoutFetchResponse(response, '" + path.id() + "', startTime)" +
                             "    });" +
                             "}")
                .content("call").content(nbsp())
                .content(div().hasText(method.color()).content(method.name()));
    }

    /// The JS expression to get the full, resolved url
    private String urlExpression() {
        var out = new StringBuilder("serverUrl() + '")
                .append(path.pathTemplate()).append("'");
        parametersIn(PATH).forEach(parameter -> out.append(parameterExpression(parameter)));
        return out.toString();
    }

    private String parameterExpression(Parameter parameter) {
        return ".replace('{" + safeName(parameter) + "}', " + inputValue(parameter) + ")";
    }

    private Button actionButton() {
        return Button.button().is(SMALL, BOLD)
                .on("keyup", """
                            switch (event.key) {
                                case 'Enter':
                                case ' ':
                                    event.stopPropagation();
                                    break;
                                case 'ArrowUp':
                                    closest('.message').focus();
                                    event.stopPropagation();
                                    break;
                                case 'ArrowLeft':
                                    if (previousElementSibling) {
                                        previousElementSibling.focus();
                                    } else {
                                        document.querySelector('select[name=__ACCEPT_MEDIA_TYPES__]').focus();
                                    }
                                    event.stopPropagation();
                                    break;
                                case 'ArrowRight':
                                    nextElementSibling?.focus();
                                    event.stopPropagation();
                                    break;
                            }
                        """.replace("__ACCEPT_MEDIA_TYPES__", path.id() + "-accept-media-types"));
    }

    private Button cliCodeButton(CliCodeType type) {
        return actionButton().is(DARK).content(type.name())
                .on("click", "navigator.clipboard.writeText(" + type.methodName()
                             + "(`" + method.name() + "`, " + urlExpression() + ", '" + path.id() + "'))" +
                             ".then(() => showToast('copied <code>" + type.name() + "</code> statement to clipboard'))");
    }

    private Button openInNewTabButton() {
        return actionButton().icon("arrow-up-right-from-square")
                .on("click", "window.open(" + urlExpression() + ", '_blank')");
    }

    record Method(HttpMethod method) implements Comparable<Method> {
        @Override public int compareTo(Method that) {return Integer.compare(this.order(), that.order());}

        boolean is(@SuppressWarnings("SameParameterValue") HttpMethod httpMethod) {return httpMethod == method;}

        String key() {return name().toLowerCase(ROOT);}

        String name() {return method.name();}

        Integer order() {
            return switch (method) {
                case GET -> 1;
                case POST -> 2;
                case PUT -> 3;
                case PATCH -> 4;
                case DELETE -> TITLE_LEVEL;
                case HEAD -> 6;
                case OPTIONS -> 7;
                case TRACE -> 8;
            };
        }

        Tag tag() {return Tag.tag(method.name()).is(color());}

        private Color color() {
            return switch (method) {
                case GET -> Color.PRIMARY; // normal read
                case HEAD, OPTIONS, TRACE -> Color.INFO; // magic read
                case POST, PUT, PATCH -> Color.LINK; // normal write
                case DELETE -> Color.WARNING; // dangerous write
            };
        }
    }
}
