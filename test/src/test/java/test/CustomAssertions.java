package test;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.assertions.LocatorAssertions;
import com.microsoft.playwright.assertions.PlaywrightAssertions;
import org.assertj.core.api.BDDAssertions;

import java.awt.*;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.stream.Stream;

import static java.awt.datatransfer.DataFlavor.stringFlavor;

public class CustomAssertions extends BDDAssertions {
    public static LocatorAssertions then(Locator locator) {return PlaywrightAssertions.assertThat(locator);}

    public static String clipboard() {
        try {
            return (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(stringFlavor);
        } catch (UnsupportedFlavorException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void enter(Locator locator, String... key) {
        locator.focus();
        Stream.of(key).forEach(locator::press);
    }
}
