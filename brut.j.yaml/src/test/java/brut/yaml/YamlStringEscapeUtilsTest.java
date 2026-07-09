package brut.yaml;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class YamlStringEscapeUtilsTest {

    @Test
    public void testEscapeString() {
        assertEquals("test", YamlStringEscapeUtils.escapeString("test"));
        assertNull(YamlStringEscapeUtils.escapeString(null));

        assertEquals("Hello\\nWorld", YamlStringEscapeUtils.escapeString("Hello\nWorld"));
        assertEquals("Hello\\rWorld", YamlStringEscapeUtils.escapeString("Hello\rWorld"));
        assertEquals("Hello\\tWorld", YamlStringEscapeUtils.escapeString("Hello\tWorld"));

        assertEquals("He said \\\"Hello\\\"", YamlStringEscapeUtils.escapeString("He said \"Hello\""));
        assertEquals("Path\\\\to\\\\file", YamlStringEscapeUtils.escapeString("Path\\to\\file"));
        assertEquals("It's fine", YamlStringEscapeUtils.escapeString("It's fine"));
        assertEquals("a/b/c", YamlStringEscapeUtils.escapeString("a/b/c"));

        // Unicode escapes
        assertEquals("\u00A0", YamlStringEscapeUtils.escapeString("\u00A0")); // Not escaped
        assertEquals("\\u0080", YamlStringEscapeUtils.escapeString("\u0080")); // Escaped
        assertEquals("\u00E1", YamlStringEscapeUtils.escapeString("\u00E1")); // Not escaped
        assertEquals("\\u0001", YamlStringEscapeUtils.escapeString("\u0001")); // Escaped
        assertEquals("\\u001F", YamlStringEscapeUtils.escapeString("\u001F")); // Escaped
        assertEquals("\\uD800", YamlStringEscapeUtils.escapeString("\uD800")); // Escaped
        assertEquals("\uE000", YamlStringEscapeUtils.escapeString("\uE000")); // Not escaped
        assertEquals("\\uFFFE", YamlStringEscapeUtils.escapeString("\uFFFE")); // Escaped
    }

    @Test
    public void testUnescapeString() {
        assertEquals("test", YamlStringEscapeUtils.unescapeString("test"));
        assertNull(YamlStringEscapeUtils.unescapeString(null));

        assertEquals("Hello\nWorld", YamlStringEscapeUtils.unescapeString("Hello\\nWorld"));
        assertEquals("Hello\rWorld", YamlStringEscapeUtils.unescapeString("Hello\\rWorld"));
        assertEquals("Hello\tWorld", YamlStringEscapeUtils.unescapeString("Hello\\tWorld"));

        assertEquals("He said \"Hello\"", YamlStringEscapeUtils.unescapeString("He said \\\"Hello\\\""));
        assertEquals("Path\\to\\file", YamlStringEscapeUtils.unescapeString("Path\\\\to\\\\file"));
        assertEquals("It's fine", YamlStringEscapeUtils.unescapeString("It's fine"));

        // Unicode unescapes
        assertEquals("\u00A0", YamlStringEscapeUtils.unescapeString("\\u00A0"));
        assertEquals("\u00E1", YamlStringEscapeUtils.unescapeString("\\u00E1")); // á
    }
}
