package io.github.lllichlet;

import org.ini4j.Ini;
import org.junit.jupiter.api.Test;

import java.io.*;

import static org.junit.jupiter.api.Assertions.*;

class Qini4jTest {

    private static Ini load(String content) throws IOException {
        return Qini4j.load(new StringReader(content));
    }

    private static String store(Ini ini) throws IOException {
        StringWriter sw = new StringWriter();
        Qini4j.store(ini, sw);
        return sw.toString();
    }

    /* ---------------------------------------- */

    @Test
    void testBasicLoadAndStore() throws IOException {
        String content = "[section]\nkey = value\n";
        Ini ini = load(content);
        assertEquals("value", ini.get("section", "key"));

        String stored = store(ini);
        assertTrue(stored.contains("[section]"));
        assertTrue(stored.contains("key = value"));
    }

    @Test
    void testRoundTrip() throws IOException {
        Ini ini = new Ini();
        ini.put("s1", "k1", "v1");
        ini.put("s1", "k2", "v2");
        ini.put("s2", "k3", "v3");

        String stored = store(ini);
        Ini loaded = load(stored);

        assertEquals("v1", loaded.get("s1", "k1"));
        assertEquals("v2", loaded.get("s1", "k2"));
        assertEquals("v3", loaded.get("s2", "k3"));
    }

    /* ---------------------------------------- */

    @Test
    void testQuotedValueDouble() throws IOException {
        Ini ini = load("[s]\nkey = \"hello world\"\n");
        assertEquals("hello world", ini.get("s", "key"));
    }

    @Test
    void testQuotedValueSingle() throws IOException {
        Ini ini = load("[s]\nkey = 'hello world'\n");
        assertEquals("hello world", ini.get("s", "key"));
    }

    @Test
    void testQuotedValueWithTrailingComment() throws IOException {
        Ini ini = load("[s]\nkey = \"hello world\" ; comment\n");
        assertEquals("hello world", ini.get("s", "key"));
    }

    @Test
    void testQuotedValueWithHashInside() throws IOException {
        Ini ini = load("[s]\nkey = \"a#b\"\n");
        assertEquals("a#b", ini.get("s", "key"));
    }

    @Test
    void testQuotedValueWithSemicolonInside() throws IOException {
        Ini ini = load("[s]\nkey = \"a;b\"\n");
        assertEquals("a;b", ini.get("s", "key"));
    }

    @Test
    void testEmptyDoubleQuotes() throws IOException {
        Ini ini = load("[s]\nkey = \"\"\n");
        assertEquals("", ini.get("s", "key"));
    }

    @Test
    void testEmptySingleQuotes() throws IOException {
        Ini ini = load("[s]\nkey = ''\n");
        assertEquals("", ini.get("s", "key"));
    }

    @Test
    void testUnclosedDoubleQuote() throws IOException {
        Ini ini = load("[s]\nkey = \"value\n");
        assertEquals("\"value", ini.get("s", "key"));
    }

    @Test
    void testUnclosedSingleQuote() throws IOException {
        Ini ini = load("[s]\nkey = 'value\n");
        assertEquals("'value", ini.get("s", "key"));
    }

    @Test
    void testSingleQuoteNestedDoubleQuote() throws IOException {
        Ini ini = load("[s]\nkey = 'say \"hi\"'\n");
        assertEquals("say \"hi\"", ini.get("s", "key"));
    }

    @Test
    void testDoubleQuoteNestedSingleQuote() throws IOException {
        Ini ini = load("[s]\nkey = \"say 'hi'\"\n");
        assertEquals("say 'hi'", ini.get("s", "key"));
    }

    @Test
    void testQuoteFollowedImmediatelyByComment() throws IOException {
        Ini ini = load("[s]\nkey = \"value\"#comment\n");
        assertEquals("value", ini.get("s", "key"));
    }

    /* ---------------------------------------- */

    @Test
    void testEscapedBackslash() throws IOException {
        Ini ini = load("[s]\nkey = \"a\\\\b\"\n");
        assertEquals("a\\b", ini.get("s", "key"));
    }

    @Test
    void testEscapedTab() throws IOException {
        Ini ini = load("[s]\nkey = \"a\\tb\"\n");
        assertEquals("a\tb", ini.get("s", "key"));
    }

    @Test
    void testEscapedNewline() throws IOException {
        Ini ini = load("[s]\nkey = \"a\\nb\"\n");
        assertEquals("a\nb", ini.get("s", "key"));
    }

    @Test
    void testBackslashBeforeNonQuote() throws IOException {
        Ini ini = load("[s]\nkey = \"a\\xb\"\n");
        assertEquals("a\\xb", ini.get("s", "key"));
    }

    @Test
    void testTrailingBackslash() throws IOException {
        Ini ini = load("[s]\nkey = \"abc\\\\\"\n");
        assertEquals("abc\\", ini.get("s", "key"));
    }

    @Test
    void testConsecutiveBackslashes() throws IOException {
        Ini ini = load("[s]\nkey = \"\\\\\\\\\"\n");
        assertEquals("\\\\", ini.get("s", "key"));
    }

    /* ---------------------------------------- */

    @Test
    void testInlineComment() throws IOException {
        Ini ini = load("[s]\nkey = value # comment\n");
        assertEquals("value", ini.get("s", "key"));

        ini = load("[s]\nkey = value ; comment\n");
        assertEquals("value", ini.get("s", "key"));
    }

    @Test
    void testHashInValueWithoutQuotes() throws IOException {
        Ini ini = load("[s]\nkey = a#b\n");
        assertEquals("a", ini.get("s", "key"));
    }

    @Test
    void testSemicolonInValueWithoutQuotes() throws IOException {
        Ini ini = load("[s]\nkey = a;b\n");
        assertEquals("a", ini.get("s", "key"));
    }

    @Test
    void testCommentAfterEmptyValue() throws IOException {
        Ini ini = load("[s]\nkey = # comment\n");
        assertEquals("", ini.get("s", "key"));
    }

    /* ---------------------------------------- */

    @Test
    void testStoreValueContainingDoubleQuotes() throws IOException {
        Ini ini = new Ini();
        ini.put("s", "key", "He said \"hi\"");
        String stored = store(ini);
        Ini loaded = load(stored);
        assertEquals("He said \"hi\"", loaded.get("s", "key"));
    }

    @Test
    void testStoreFormatWithQuotesIsValid() throws IOException {
        Ini ini = new Ini();
        ini.put("s", "key", "He said \"hi\"");
        String stored = store(ini);
        String line = "";
        for (String l : stored.split("\n")) {
            if (l.trim().startsWith("key")) {
                line = l;
                break;
            }
        }
        int eq = line.indexOf('=');
        assertTrue(eq > 0, "Should contain key = value");
        String valPart = line.substring(eq + 1).trim();
        assertTrue(valPart.startsWith("\"") && valPart.endsWith("\""), "Value should be quoted");
        String inner = valPart.substring(1, valPart.length() - 1);
        for (int i = 0; i < inner.length(); i++) {
            if (inner.charAt(i) == '\"') {
                assertTrue(i > 0 && inner.charAt(i - 1) == '\\',
                    "Double quote at position " + i + " is not escaped");
            }
        }
    }

    @Test
    void testStoreValueWithLeadingTrailingSpaces() throws IOException {
        Ini ini = new Ini();
        ini.put("s", "key", " hello ");
        String stored = store(ini);
        Ini loaded = load(stored);
        assertEquals(" hello ", loaded.get("s", "key"));
    }

    @Test
    void testStoreNullValue() throws IOException {
        Ini ini = new Ini();
        ini.put("s", "key", null);
        String stored = store(ini);
        Ini loaded = load(stored);
        String val = loaded.get("s", "key");
        assertTrue(val == null || val.isEmpty(), "null value should not become string 'null'");
    }

    @Test
    void testValueWithEquals() throws IOException {
        Ini ini = new Ini();
        ini.put("s", "key", "a=b");
        String stored = store(ini);
        Ini loaded = load(stored);
        assertEquals("a=b", loaded.get("s", "key"));
    }

    @Test
    void testValueWithHash() throws IOException {
        Ini ini = new Ini();
        ini.put("s", "key", "a#b");
        String stored = store(ini);
        Ini loaded = load(stored);
        assertEquals("a#b", loaded.get("s", "key"));
    }

    @Test
    void testValueWithSemicolon() throws IOException {
        Ini ini = new Ini();
        ini.put("s", "key", "a;b");
        String stored = store(ini);
        Ini loaded = load(stored);
        assertEquals("a;b", loaded.get("s", "key"));
    }

    @Test
    void testValueWithRealNewlineRoundTrip() throws IOException {
        Ini ini = new Ini();
        ini.put("s", "key", "line1\nline2");
        String stored = store(ini);
        assertFalse(stored.contains("line1\nline2"));
        Ini loaded = load(stored);
        assertEquals("line1\nline2", loaded.get("s", "key"));
    }

    @Test
    void testValueWithRealCarriageReturnRoundTrip() throws IOException {
        Ini ini = new Ini();
        ini.put("s", "key", "a\rb");
        String stored = store(ini);
        Ini loaded = load(stored);
        assertEquals("a\rb", loaded.get("s", "key"));
    }

    @Test
    void testValueWithRealTabRoundTrip() throws IOException {
        Ini ini = new Ini();
        ini.put("s", "key", "a\tb");
        String stored = store(ini);
        Ini loaded = load(stored);
        assertEquals("a\tb", loaded.get("s", "key"));
    }

    /* ---------------------------------------- */

    @Test
    void testBlankValue() throws IOException {
        Ini ini = load("[s]\nkey = \n");
        assertEquals("", ini.get("s", "key"));
    }

    @Test
    void testOnlyWhitespaceValue() throws IOException {
        Ini ini = load("[s]\nkey =    \n");
        assertEquals("", ini.get("s", "key"));
    }

    @Test
    void testKeyWithLeadingTrailingSpaces() throws IOException {
        Ini ini = load("[s]\n  key  = value\n");
        assertEquals("value", ini.get("s", "key"));
    }

    @Test
    void testValueWithMultipleInternalSpaces() throws IOException {
        Ini ini = load("[s]\nkey = a  b   c\n");
        assertEquals("a  b   c", ini.get("s", "key"));
    }

    @Test
    void testValueWithTab() throws IOException {
        Ini ini = load("[s]\nkey = \"a\tb\"\n");
        assertEquals("a\tb", ini.get("s", "key"));
    }

    /* ---------------------------------------- */

    @Test
    void testColonSeparator() throws IOException {
        Ini ini = load("[s]\nkey : value\n");
        assertEquals("value", ini.get("s", "key"));
    }

    @Test
    void testMultipleEqualsInValue() throws IOException {
        Ini ini = load("[s]\nkey = a = b = c\n");
        assertEquals("a = b = c", ini.get("s", "key"));
    }

    @Test
    void testKeyWithColon() throws IOException {
        Ini ini = load("[s]\na:b = value\n");
        assertEquals("b = value", ini.get("s", "a"));
    }

    @Test
    void testLineWithoutSeparatorIsIgnored() throws IOException {
        Ini ini = load("[s]\nno_separator_line\nkey = value\n");
        assertEquals("value", ini.get("s", "key"));
    }

    /* ---------------------------------------- */

    @Test
    void testEmptySection() throws IOException {
        Ini ini = load("[]\nkey = value\n");
        assertEquals("value", ini.get("", "key"));
    }

    @Test
    void testEmptyKey() throws IOException {
        Ini ini = load("[s]\n = value\n");
        assertEquals("value", ini.get("s", ""));
    }

    @Test
    void testSectionNameWithClosingBracket() throws IOException {
        Ini ini = load("[sec]tion]\nkey = value\n");
        assertEquals("value", ini.get("sec]tion", "key"));
    }

    @Test
    void testDuplicateKeys() throws IOException {
        Ini ini = load("[s]\nkey = first\nkey = second\n");
        assertEquals("second", ini.get("s", "key"));
    }

    @Test
    void testDuplicateSections() throws IOException {
        Ini ini = load("[s]\nk1 = a\n[s]\nk2 = b\n");
        assertEquals("a", ini.get("s", "k1"));
        assertEquals("b", ini.get("s", "k2"));
    }

    /* ---------------------------------------- */

    @Test
    void testEmptyFile() throws IOException {
        Ini ini = load("");
        assertTrue(ini.isEmpty());
    }

    @Test
    void testOnlyCommentsAndBlankLines() throws IOException {
        Ini ini = load("# comment\n\n; another\n   \n");
        assertTrue(ini.isEmpty());
    }

    @Test
    void testNoSectionThrowsIOException() {
        IOException ex = assertThrows(IOException.class, () -> load("key = value\n"));
        assertTrue(ex.getMessage().toLowerCase().contains("section"));
    }

    @Test
    void testExceptionContainsLineNumber() {
        IOException ex = assertThrows(IOException.class, () -> load("key = value\n"));
        assertTrue(ex.getMessage().contains("Line 1"));
    }

    /* ---------------------------------------- */

    @Test
    void testNullReaderThrows() {
        assertThrows(NullPointerException.class, () -> Qini4j.load(null));
    }

    @Test
    void testNullWriterThrows() {
        assertThrows(NullPointerException.class, () -> Qini4j.store(new Ini(), null));
    }

    /* ---------------------------------------- */

    @Test
    void testUnicodeValue() throws IOException {
        Ini ini = load("[s]\nkey = 你好世界\n");
        assertEquals("你好世界", ini.get("s", "key"));
    }

    @Test
    void testEmojiValue() throws IOException {
        Ini ini = new Ini();
        ini.put("s", "key", "🚀🔥");
        String stored = store(ini);
        Ini loaded = load(stored);
        assertEquals("🚀🔥", loaded.get("s", "key"));
    }
}
