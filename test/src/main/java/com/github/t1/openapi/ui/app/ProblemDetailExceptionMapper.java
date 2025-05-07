package com.github.t1.openapi.ui.app;

import jakarta.json.bind.JsonbException;
import jakarta.json.stream.JsonParsingException;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.UUID;
import java.util.regex.Pattern;

import static jakarta.ws.rs.core.Response.Status.TEMPORARY_REDIRECT;
import static java.util.Locale.ROOT;

@Provider
public class ProblemDetailExceptionMapper implements ExceptionMapper<Throwable> {
    private static final Logger log = LoggerFactory.getLogger(ProblemDetailExceptionMapper.class);

    @Override public Response toResponse(Throwable exception) {
        var problemDetail = ProblemDetail.of(exception);
        if (problemDetail.status == TEMPORARY_REDIRECT.getStatusCode() && exception instanceof WebApplicationException e) {
            return e.getResponse();
        }
        log.error(problemDetail.toString(), exception);
        return Response.status(problemDetail.status)
                .entity(problemDetail)
                .type("application/problem+json")
                .build();
    }

    public record ProblemDetail(
            URI type,
            String title,
            int status,
            String details,
            URI instance) {
        public static ProblemDetail of(Throwable exception) {
            if (exception instanceof ProcessingException) exception = exception.getCause();
            if (exception instanceof JsonbException && exception.getCause() != null) exception = exception.getCause();
            URI type = URI.create("urn:problem-type:" + typeId(exception));
            String details;
            if (exception.getMessage() == null) {
                details = null;
            } else {
                var matcher = ERROR_CODE.matcher(exception.getMessage());
                if (matcher.matches()) {
                    type = URI.create("urn:problem-type:" + matcher.group("code").toLowerCase(ROOT));
                    details = matcher.group("message");
                } else {
                    details = exception.getMessage();
                }
            }
            int status = (exception instanceof WebApplicationException webAppEx)
                    ? webAppEx.getResponse().getStatus()
                    : (exception instanceof JsonParsingException)
                    ? 400
                    : 500;
            return new ProblemDetail(
                    type,
                    splitCamel(exception.getClass().getSimpleName(), ' '),
                    status,
                    details,
                    URI.create("urn:uuid:" + UUID.randomUUID()));
        }

        private static String typeId(Throwable exception) {
            return splitCamel(exception.getClass().getSimpleName(), '-').toLowerCase(ROOT);
        }

        private static String splitCamel(String input, char delimiter) {
            var out = new StringBuilder();
            input.codePoints().forEach(c -> {
                if (Character.isUpperCase(c) && !out.isEmpty()) out.append(delimiter);
                out.appendCodePoint(c);
            });
            return out.toString();
        }

        private static final Pattern ERROR_CODE = Pattern.compile("(?<code>[A-Z]{3,8}\\d{2,8}): (?<message>.*)");
    }
}
