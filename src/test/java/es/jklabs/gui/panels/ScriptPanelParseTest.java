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

    @SuppressWarnings("unchecked")
    private List<String> invokeParseStatements(String sql, boolean mysql) throws Exception {
        ScriptPanel panel = new ScriptPanel(null);
        Method method = ScriptPanel.class.getDeclaredMethod("parseStatements", String.class, boolean.class);
        method.setAccessible(true);
        return (List<String>) method.invoke(panel, sql, mysql);
    }
}
