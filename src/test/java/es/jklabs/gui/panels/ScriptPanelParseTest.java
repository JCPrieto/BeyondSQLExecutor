package es.jklabs.gui.panels;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScriptPanelParseTest {

    @Test
    void parseStatementsMysqlSupportsDelimiterAndComments() throws Exception {
        String sql = ""
                + "delimiter $$\n"
                + "CREATE PROCEDURE test_proc()\n"
                + "BEGIN\n"
                + "  SELECT 1; -- inside proc\n"
                + "END$$\n"
                + "delimiter ;\n"
                + "select '-- not comment' as val; /* block\n"
                + "comment */\n"
                + "select 2;\n";

        List<String> statements = invokeParseStatements(sql, true);

        assertEquals(3, statements.size());
        assertTrue(statements.get(0).contains("CREATE PROCEDURE test_proc()"));
        assertTrue(statements.get(1).contains("select '-- not comment' as val"));
        assertTrue(statements.get(2).contains("select 2"));
    }

    @Test
    void parseStatementsPostgresSupportsDollarQuotedBlocks() throws Exception {
        String sql = ""
                + "CREATE FUNCTION test_fn()\n"
                + "RETURNS void AS $$\n"
                + "BEGIN\n"
                + "  -- comment inside function\n"
                + "  PERFORM 1;\n"
                + "END;\n"
                + "$$ LANGUAGE plpgsql;\n"
                + "\n"
                + "SELECT 1;\n";

        List<String> statements = invokeParseStatements(sql, false);

        assertEquals(2, statements.size());
        assertTrue(statements.get(0).contains("CREATE FUNCTION test_fn()"));
        assertTrue(statements.get(1).contains("SELECT 1"));
    }

    @Test
    void parseStatementsMysqlSupportsDelimiterResetAcrossMultipleBlocks() throws Exception {
        String sql = ""
                + "DELIMITER //\n"
                + "CREATE PROCEDURE first_proc()\n"
                + "BEGIN\n"
                + "  SELECT 1;\n"
                + "END//\n"
                + "DELIMITER $$\n"
                + "CREATE PROCEDURE second_proc()\n"
                + "BEGIN\n"
                + "  SELECT 2;\n"
                + "END$$\n"
                + "DELIMITER ;\n"
                + "SELECT 3;\n";

        List<String> statements = invokeParseStatements(sql, true);

        assertEquals(3, statements.size());
        assertTrue(statements.get(0).contains("CREATE PROCEDURE first_proc()"));
        assertTrue(statements.get(1).contains("CREATE PROCEDURE second_proc()"));
        assertEquals("SELECT 3", statements.get(2));
    }

    @Test
    void parseStatementsMysqlIgnoresDelimiterKeywordOutsideLineStart() throws Exception {
        String sql = ""
                + "SELECT 'delimiter $$ should stay literal';\n"
                + "SELECT 2;\n";

        List<String> statements = invokeParseStatements(sql, true);

        assertEquals(List.of(
                "SELECT 'delimiter $$ should stay literal'",
                "SELECT 2"
        ), statements);
    }

    @Test
    void parseStatementsPostgresSupportsTaggedDollarQuotes() throws Exception {
        String sql = ""
                + "CREATE FUNCTION tagged_fn()\n"
                + "RETURNS text AS $body$\n"
                + "BEGIN\n"
                + "  RETURN '; -- /* still inside body */';\n"
                + "END;\n"
                + "$body$ LANGUAGE plpgsql;\n"
                + "SELECT '$body$ outside function';\n";

        List<String> statements = invokeParseStatements(sql, false);

        assertEquals(2, statements.size());
        assertTrue(statements.get(0).contains("RETURNS text AS $body$"));
        assertEquals("SELECT '$body$ outside function'", statements.get(1));
    }

    @Test
    void parseStatementsSkipsEmptyStatementsCreatedByRepeatedDelimiters() throws Exception {
        String sql = "SELECT 1;;\n;\nSELECT 2;";

        List<String> statements = invokeParseStatements(sql, false);

        assertEquals(List.of("SELECT 1", "SELECT 2"), statements);
    }

    @Test
    void parseStatementsDropsStandaloneCommentsBetweenStatements() throws Exception {
        String sql = ""
                + "SELECT 1;\n"
                + "-- standalone line comment\n"
                + "/* standalone block comment */\n"
                + "SELECT 2;\n";

        List<String> statements = invokeParseStatements(sql, false);

        assertEquals(List.of("SELECT 1", "SELECT 2"), statements);
    }

    @Test
    void parseStatementsDoesNotTreatCommentTokensInsideQuotedStringsAsComments() throws Exception {
        String sql = ""
                + "SELECT '-- still text', '/* also text */';\n"
                + "SELECT \"-- quoted identifier\";\n";

        List<String> statements = invokeParseStatements(sql, false);

        assertEquals(List.of(
                "SELECT '-- still text', '/* also text */'",
                "SELECT \"-- quoted identifier\""
        ), statements);
    }

    @Test
    void parseStatementsMysqlIgnoresDelimiterDirectiveTextInsideProcedureBody() throws Exception {
        String sql = ""
                + "DELIMITER //\n"
                + "CREATE PROCEDURE test_proc()\n"
                + "BEGIN\n"
                + "  SELECT 'DELIMITER $$';\n"
                + "  SELECT 1;\n"
                + "END//\n"
                + "DELIMITER ;\n"
                + "SELECT 2;\n";

        List<String> statements = invokeParseStatements(sql, true);

        assertEquals(2, statements.size());
        assertTrue(statements.get(0).contains("SELECT 'DELIMITER $$';"));
        assertEquals("SELECT 2", statements.get(1));
    }

    @Test
    void parseStatementsPostgresSupportsEmptyDollarQuotesContainingSqlLikeText() throws Exception {
        String sql = ""
                + "DO $$\n"
                + "BEGIN\n"
                + "  PERFORM ';';\n"
                + "  PERFORM 'SELECT 1;';\n"
                + "END;\n"
                + "$$;\n"
                + "SELECT 2;\n";

        List<String> statements = invokeParseStatements(sql, false);

        assertEquals(2, statements.size());
        assertTrue(statements.get(0).startsWith("DO $$"));
        assertEquals("SELECT 2", statements.get(1));
    }

    @SuppressWarnings("unchecked")
    private List<String> invokeParseStatements(String sql, boolean mysql) throws Exception {
        ScriptPanel panel = new ScriptPanel(null);
        Method method = ScriptPanel.class.getDeclaredMethod("parseStatements", String.class, boolean.class);
        method.setAccessible(true);
        return (List<String>) method.invoke(panel, sql, mysql);
    }
}
