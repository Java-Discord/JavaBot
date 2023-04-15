package net.javadiscord.javabot.util;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Test for the {@link IndentationHelper} class.
 */
public class IndentationHelperTest {
    /**
     * Tests the {@link IndentationHelper#formatIndentation(String, IndentationHelper.IndentationType)} method
     */
    @Test
    public void testFormatIndentation() {
        String[] unformatted = null;
        String[] formatted = null;
        try {
            unformatted = Files.readString(Path.of(IndentationHelper.class.getResource("/Unformatted Strings.txt").toURI())).split("----");
            formatted = Files.readString(Path.of(IndentationHelper.class.getResource("/Formatted Strings.txt").toURI())).split("----");
        } catch (NullPointerException | URISyntaxException e) {
            fail("Files to run the test not present");
        } catch (IOException e) {
            fail("IO Exception occurred");
        }

        try {
            for(int i = 0,k=0; i< unformatted.length; i++) {
                assertEquals(formatted[k++],   IndentationHelper.formatIndentation(unformatted[i], IndentationHelper.IndentationType.FOUR_SPACES),"Method failed to format a text with four spaces correctly");
                assertEquals(formatted[k++], IndentationHelper.formatIndentation(unformatted[i], IndentationHelper.IndentationType.TWO_SPACES),"Method failed to format a text with two spaces correctly");
                assertEquals(formatted[k++], IndentationHelper.formatIndentation(unformatted[i], IndentationHelper.IndentationType.TABS), "Method failed to format a text with tabs correctly.");
                assertEquals(formatted[k++], IndentationHelper.formatIndentation(unformatted[i], IndentationHelper.IndentationType.NULL), "Method returned a String not matching the input");
            }
        } catch (Exception e) {
            fail("Method threw exception",e.getCause());
        }

    }
}
