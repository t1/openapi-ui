// noinspection JSUnusedGlobalSymbols

function createElement(tagName, options) {
    return Object.assign(document.createElement(tagName), options ?? {});
}

function serverUrl() {
    return document.querySelector('select[name="servers"]')?.value
        ?? URL.parse(document.location.href).origin;
}

function acceptHeader(id) {
    let select = document.querySelector('select[name="' + id + '"]');
    let values = Array.from(select.selectedOptions).map(option => option.value);
    return values.length ? values.join(', ') : '*/*';
}

function showToast(message, level = 'success') {
    const toast = createElement('article', {
        className: `notification is-${level} toast`,
        popover: 'manual',
        innerHTML: message
    });
    toast.addEventListener('toggle', (event) => {
        if (event.newState === 'open') moveToastsUp();
    });
    document.body.appendChild(toast);
    toast.showPopover();

    setTimeout(() => {
        toast.hidePopover();
        toast.remove();
    }, 3000);
}

function moveToastsUp() {
    let bottom = 20 - window.scrollY;
    Array.from(document.querySelectorAll('.toast')).reverse().forEach((toast) => {
        toast.style.bottom = `${bottom}px`;
        bottom = window.innerHeight - toast.offsetTop - 20;
    });
}

function handleTryoutFetchResponse(response, pathId, startTime) {
    toText(response)
        .then(text => {
            const headers = Array.from(response.headers.entries());
            let responseArea = document.getElementById(pathId + '-response-area');
            if (!responseArea) {
                responseArea = createElement('div', {id: pathId + '-response-area', className: 'box'});
                document.getElementById(pathId + '-body').appendChild(responseArea);
            }
            responseArea.replaceChildren(
                withChildren(block(), status(response, startTime)),
                disabledTextAreaBlock(headers.map(header => `${header[0]}: ${header[1]}`).join('\n'), headers.length),
                disabledTextAreaBlock(text, Math.max(text.split('\n').length, 3)));
        })
        .catch(error => {
            console.error(error);
            showToast(error.message, 'danger');
        });
}

async function toText(response) {
    const contentType = response.headers.get('Content-Type')?.split(';')[0];
    if (is(contentType, 'json')) {
        const json = await response.json();
        return JSON.stringify(json, null, 2);
    } else if (is(contentType, 'xml')) {
        return formatXml(await response.text());
    } else {
        return await response.text();
    }
}

function is(contentType, subtype) {
    return contentType && contentType.startsWith('application/')
        && (contentType.endsWith('/' + subtype) || contentType.endsWith('+' + subtype));
}

function formatXml(xml) {
    const xmlDoc = new DOMParser().parseFromString(xml, "application/xml");
    const errors = xmlDoc.getElementsByTagName("parsererror");
    if (errors.length > 0) {
        const message = "XML Parse Error: " + errors[0].querySelector('div')?.textContent;
        console.error(message);
        showToast(message, 'danger');
        return xml;
    }
    const xsltProcessor = new XSLTProcessor();
    xsltProcessor.importStylesheet(new DOMParser().parseFromString([
        '<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">',
        '  <xsl:strip-space elements="*"/>',
        '  <xsl:template match="node()|@*">',
        '    <xsl:copy><xsl:apply-templates select="node()|@*"/></xsl:copy>',
        '  </xsl:template>',
        '  <xsl:output method="xml" indent="yes"/>',
        '</xsl:stylesheet>'
    ].join('\n'), 'application/xml'));
    return new XMLSerializer().serializeToString(xsltProcessor.transformToDocument(xmlDoc));
}

function status(response, startTime) {
    const duration = (performance.now() - startTime).toFixed(0);
    return withChildren(createElement('div', {className: 'notification',}),
        createElement('div', {
            className: 'tag is-family-code ' + tagColor(response),
            textContent: response.status + ' ' + response.statusText
        }),
        createElement('div', {
            className: 'is-family-code is-pulled-right',
            textContent: duration + 'ms'
        }));
}

function tagColor(response) {
    if (response.status < 400) return 'is-success';
    if (response.status >= 400 && response.status < 500) return 'is-warning';
    return 'is-danger';
}

function disabledTextAreaBlock(text, rows) {
    return withChildren(block(), disabledTextArea(text, rows));
}

function block() {
    return createElement('div', {className: 'block'});
}

function disabledTextArea(text, rows) {
    if (rows && rows > 20) rows = 20;
    return withChildren(createElement('div', {className: 'control'}),
        createElement('textarea', {
            className: 'textarea is-family-code',
            disabled: true,
            textContent: text,
            rows: rows ?? 3,
            style: 'cursor: auto;'
        }));
}

function withChildren(element, ...children) {
    for (const child of children) {
        element.appendChild(child);
    }
    return element;
}
