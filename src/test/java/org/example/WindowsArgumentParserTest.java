package org.example;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WindowsArgumentParserTest {
    private final WindowsArgumentParser parser = new WindowsArgumentParser();

    @Test
    void returnsEmptyListForBlankInput() {
        assertEquals(List.of(), parser.parse(null));
        assertEquals(List.of(), parser.parse("   "));
    }

    @Test
    void splitsSimpleArguments() {
        assertEquals(
                List.of("--profile", "work", "--level", "5"),
                parser.parse("--profile work --level 5")
        );
    }

    @Test
    void keepsWhitespaceInsideQuotes() {
        assertEquals(
                List.of("--profile", "Work User", "--file", "C:\\My Documents\\report.txt"),
                parser.parse("--profile \"Work User\" --file \"C:\\My Documents\\report.txt\"")
        );
    }

    @Test
    void supportsEscapedQuoteInsideQuotedArgument() {
        assertEquals(
                List.of("value with \"quote\""),
                parser.parse("\"value with \\\"quote\\\"\"")
        );
    }

    @Test
    void rejectsUnclosedQuote() {
        assertThrows(
                IllegalArgumentException.class,
                () -> parser.parse("--name \"unfinished")
        );
    }
}
