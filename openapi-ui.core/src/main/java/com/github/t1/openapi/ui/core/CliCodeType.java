package com.github.t1.openapi.ui.core;

import java.util.stream.Stream;

enum CliCodeType {
    httpie {
        @Override String command() {
            return ("""
                    var protocol = URL.parse(url).protocol; // includes the colon
                    if (url.startsWith(protocol + '//')) {
                        url = url.substring(protocol.length + 2);
                    }
                    if (url.startsWith('localhost')) {
                        url = url.substring(9);
                        if (!url.startsWith(':')) url = ':' + url; // no port => use ':/path' for port 80/443
                    }
                    var body = document.getElementById(id + '-body-textarea');
                    var headers = '"Accept:' + acceptHeader(id + '-accept-media-types') + '"';
                    var contentTypes = document.querySelector(`select[name='` + id + `-content-types` + `']`);
                    if (contentTypes) headers += ' "Content-Type:' + contentTypes.value + '"';
                    return protocol.substring(0, protocol.length - 1)
                        + ' --verbose'
                        + (body ? ' --raw=\\'' + body.value.replaceAll('\\'', '\\\\\\'') + '\\'' : '')
                        + (method === ' GET' ? '--follow ' : ' ' + method)
                        + ' ' + url
                        + ' ' + headers;
                    """);
        }
    },

    curl {
        @Override String command() {
            return """
                    var headers = '--header "Accept:' + acceptHeader(id + '-accept-media-types') + '" ';
                    var contentTypes = document.querySelector(`select[name='` + id + `-content-types` + `']`);
                    if (contentTypes) headers += '--header "Content-Type:' + contentTypes.value + '"';
                    var body = document.getElementById(id + '-body-textarea');
                    return 'curl --location --include '
                        + (method === 'GET' ? '' : '-X ' + method + ' ')
                        + headers
                        + ' ' + url
                        + (body ? ' --data \\'' + body.value.replaceAll('\\'', '\\\\\\'') + '\\'' : '');
                    """;
        }
    };

    static Stream<CliCodeType> stream() {return Stream.of(CliCodeType.values());}

    String methodName() {return name();}

    /// The JavaScript code to execute this operation in this type of cli command.
    /// The command is the body of a function with two parameters: `method`, and `url`
    abstract String command();
}
