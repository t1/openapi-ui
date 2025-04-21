package test;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Tracing;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static com.microsoft.playwright.options.AriaRole.BUTTON;
import static test.CustomAssertions.clipboard;
import static test.CustomAssertions.enter;
import static test.CustomAssertions.fail;
import static test.CustomAssertions.then;

class EndToEndIT {
    static final boolean debug = ConfigProvider.getConfig().getOptionalValue("debug", boolean.class).orElse(false);

    static final Path OUTPUT_DIR = Path.of("target/playwright/");
    static final String PRODUCT_JSON = """
            {
              "id": "123",
              "name": "Tabula Rasa #123",
              "description": "s100",
              "price": 12300,
              "ratings": [
                {
                  "user": "s101",
                  "stars": 102,
                  "comment": "s103"
                },
                {
                  "user": "s104",
                  "stars": 105,
                  "comment": "s106"
                },
                {
                  "user": "s107",
                  "stars": 108,
                  "comment": "s109"
                }
              ]
            }""";

    static Playwright playwright;
    static Browser browser;

    @BeforeAll static void launchBrowser() {
        playwright = Playwright.create();
        browser = playwright.webkit().launch(debug ?
                new BrowserType.LaunchOptions().setHeadless(false).setSlowMo(1000) : null);
    }

    @AfterAll static void closeBrowser() {playwright.close();}

    BrowserContext context;
    Page page;
    boolean tracingStarted;

    Locator pageTitle;
    Locator pageSubtitle;
    Locator serversSelect;
    Locator getPopupAction;
    Locator httpieAction;
    Locator curlAction;
    Locator toast;

    ProductsPath productsPath;
    ProductsIdPath productsIdPath;


    @BeforeEach void createContextAndPage() {
        context = browser.newContext();
        page = context.newPage();
        page.onConsoleMessage(message -> {
            switch (message.type()) {
                case "error" -> fail("console error: " + message.text());
                case "warning" -> fail("console warning: " + message.text());
                default -> System.out.println("console " + message.type() + ": " + message.text());
            }
        });
        context.tracing().start(new Tracing.StartOptions()
                .setScreenshots(true)
                .setSnapshots(true)
                .setSources(System.getenv("PLAYWRIGHT_JAVA_SRC") != null));
        tracingStarted = true;

        pageTitle = element("h1.title");
        pageSubtitle = element("h2.subtitle");
        serversSelect = element("select[name=servers]");
        getPopupAction = button("");
        httpieAction = button("httpie");
        curlAction = button("curl");
        toast = element("article.toast.notification");
        productsPath = new ProductsPath();
        productsIdPath = new ProductsIdPath();
    }

    @AfterEach void closeContext() {
        if (tracingStarted) context.tracing().stop(new Tracing.StopOptions().setPath(OUTPUT_DIR.resolve("trace.zip")));
        context.close();
    }

    void screenshot(@SuppressWarnings("SameParameterValue") String fileName) {
        page.screenshot(new Page.ScreenshotOptions().setPath(OUTPUT_DIR.resolve("screenshots/" + fileName + ".png")));
    }

    Locator element(String selector) {return page.locator(selector);}

    Locator button(String name) {return page.getByRole(BUTTON, new Page.GetByRoleOptions().setName(name));}

    @Test void shouldWork() {
        start();
        productsPath.get();
        productsPath.post();
        productsPath.close();
        productsIdPath.get();
        then(toast).not().isVisible(); // toasts disappear after 3s; we don't check earlier to speed up the test
    }

    void start() {
        page.navigate("http://localhost:8080/openapi/ui");
        screenshot("start");

        then(page.title()).isEqualTo("Demo Product API for the OpenAPI UI");
        then(pageTitle).containsText("Demo Product API for the OpenAPI UI");
        then(pageSubtitle).containsText("version: 1.0.0");
        then(serversSelect).containsText("http://localhost:8080");
        then(productsPath.path).containsText("/products");
        then(productsPath.path).isFocused();
        then(productsPath.synopsis).not().isVisible();
        then(productsIdPath.path).containsText("/products/{id}");
        then(productsIdPath.synopsis).not().isVisible();
    }

    class ProductsPath {
        final Locator path;
        final Locator pathClose;
        final Locator synopsis;
        final Locator getOp;
        final Locator acceptSelect;
        final Locator callAction;
        final Locator postOp;
        final Locator requestContentType;
        final Locator requestBody;
        final Locator responseStatus;
        final Locator responseHeaders;
        final Locator responseBody;

        ProductsPath() {
            path = element("article#products");
            synopsis = element("#products-synopsis");
            getOp = element("#products-get");
            acceptSelect = element("select[name=products-accept-media-types]");
            callAction = element("#products-call-button");
            postOp = element("#products-post");
            requestContentType = element("select[name=products-request-content-types]");
            requestBody = element("#products-request-body");
            responseStatus = element("#products-response-status");
            responseHeaders = element("#products-response-headers");
            responseBody = element("#products-response-body");
            pathClose = element("#products-path-close");
        }

        void get() {
            path.press("ArrowRight");
            then(getOp).isFocused();
            then(synopsis).containsText("Get all products");
            then(synopsis).containsText("You could use many words here to describe this, but it's actually quite simple. Currently, there are no filters, no sorting, and no pagination. But that could come later.");
            then(synopsis).containsText("Currently, it's just a list of all products and this text just has to be loong ;-)");
            then(acceptSelect).containsText("application/json application/xml");
            then(callAction).containsText("call GET");
            then(getPopupAction).isVisible();
            then(httpieAction).isVisible();
            then(curlAction).isVisible();

            var popup = page.waitForPopup(() -> getPopupAction.click());
            then(popup.content()).containsIgnoringWhitespaces("&lt;products&gt;", "&lt;product&gt;",
                    "&lt;description&gt;", "A clean table", "&lt;/description&gt;");
            popup.close();

            httpieAction.click();
            then(toast).containsText("copied httpie statement to clipboard");
            then(clipboard()).isEqualTo("http --verbose GET :8080/products \"Accept:application/json\"");
        }

        void post() {
            getOp.focus();
            getOp.press("ArrowRight");
            then(postOp).isFocused();
            postOp.press("ArrowRight"); // again
            then(postOp).isFocused(); // still
            then(synopsis).containsText("Create a new or update an existing product; the id must be unique and specified by the client");
            then(requestContentType).containsText("application/json application/xml");
            then(acceptSelect).containsText("application/json");
            then(acceptSelect).not().containsText("application/xml");
            then(callAction).containsText("call POST");
            then(getPopupAction).not().isVisible();
            then(httpieAction).isVisible();
            then(curlAction).isVisible();
            then(path).containsText("Request Body");
            then(requestBody).hasValue(PRODUCT_JSON);
            enter(requestBody, "ControlOrMeta+ArrowLeft", "ControlOrMeta+ArrowUp", "ArrowDown",
                    "ControlOrMeta+ArrowRight", "ArrowLeft", "ArrowLeft", "Backspace", "4");
            var updatedJson = PRODUCT_JSON.replace("\"id\": \"123\"", "\"id\": \"124\"");
            then(requestBody).hasValue(updatedJson);
            pathClose.click();
            then(synopsis).not().isVisible();
            then(requestBody).not().isVisible();
            postOp.click(); // come back
            then(requestBody).hasValue(updatedJson); // body restored from local storage
            callAction.click();
            then(responseStatus).containsText("200 OK");
            then(responseHeaders).containsText("content-type: application/json");
            then(responseBody).containsText("\"id\": \"124\"");
            postOp.click(); // reloads the POST op
            then(responseStatus).not().isVisible();
        }

        void close() {
            getOp.focus();
            then(getOp).isFocused();
            getOp.press("ArrowLeft");
            then(path).isFocused();
            then(synopsis).not().isVisible();
            then(productsIdPath.synopsis).not().isVisible();
        }
    }

    class ProductsIdPath {
        ProductsIdPath() {
            path = element("article#products-«id»");
            synopsis = element("#products-«id»-synopsis");
            getOp = element("#products-«id»-get");
            putOp = element("#products-«id»-put");
            pathParamId = element("input[name=products-«id»-param-id]");
        }

        final Locator path;
        final Locator synopsis;
        final Locator getOp;
        final Locator putOp;
        final Locator pathParamId;

        void get() {
            then(productsPath.path).isFocused();
            productsPath.path.press("ArrowDown");
            then(path).isFocused();
            then(synopsis).not().isVisible();
            path.press("ArrowRight");
            then(getOp).isFocused();
            then(synopsis).containsText("Get a product by its id");
            then(pathParamId).hasValue("");
            pathParamId.fill("32"); // "fill" doesn't trigger our local-store mechanism
            pathParamId.press("1");
            then(pathParamId).hasValue("321");
            putOp.click();
            then(synopsis).containsText("Store a product by its id, and if it already exists, replace it completely.");
            then(pathParamId).hasValue("321"); // path param restored from local storage
        }
    }
}
