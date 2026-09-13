package config.system;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The way the attributes are listed is a system wide preference, it has to
 * survive a restart of eHOST through {@code eHOST.sys}.
 */
class AttributeDisplayConfigTest {

    @TempDir
    Path configHome;

    private String savedConfigureFile;
    private env.Parameters.AttributeDisplay.Order savedOrder;
    private boolean savedHighlight;
    private String savedWorkspace;
    private String savedAnnotator;

    @BeforeEach
    void setUp() {
        savedConfigureFile = SysConf.SYS_CONFIGURE;
        savedOrder = env.Parameters.AttributeDisplay.order;
        savedHighlight = env.Parameters.AttributeDisplay.highlightDifferences;
        savedWorkspace = env.Parameters.WorkSpace.WorkSpace_AbsolutelyPath;
        savedAnnotator = resultEditor.annotator.Manager.getCurrentAnnotator();
    }

    @AfterEach
    void tearDown() {
        SysConf.SYS_CONFIGURE = savedConfigureFile;
        env.Parameters.AttributeDisplay.order = savedOrder;
        env.Parameters.AttributeDisplay.highlightDifferences = savedHighlight;
        env.Parameters.WorkSpace.WorkSpace_AbsolutelyPath = savedWorkspace;
        resultEditor.annotator.Manager.setCurrentAnnotator(savedAnnotator);
    }

    private File configFile() {
        return Paths.get(configHome.toString(), "eHOST.sys").toFile();
    }

    @Test
    @DisplayName("the chosen order and highlighting survive a save and a reload")
    void settingsAreWrittenAndReadBack() throws Exception {
        SysConf.SYS_CONFIGURE = configFile().getAbsolutePath();
        env.Parameters.AttributeDisplay.order = env.Parameters.AttributeDisplay.Order.NAME;
        env.Parameters.AttributeDisplay.highlightDifferences = false;

        SysConf.saveSystemConfigure();

        String written = new String(Files.readAllBytes(configFile().toPath()), "UTF-8");
        assertTrue(written.contains("[ATTRIBUTE_DISPLAY_ORDER]"), written);
        assertTrue(written.contains("[HIGHLIGHT_ATTRIBUTE_DIFFERENCES]"), written);

        // forget everything, then read the file back the way eHOST does at startup
        env.Parameters.AttributeDisplay.order = env.Parameters.AttributeDisplay.Order.SCHEMA;
        env.Parameters.AttributeDisplay.highlightDifferences = true;

        SysConf.loadSystemConfigure(configHome.toString());

        assertEquals(env.Parameters.AttributeDisplay.Order.NAME,
                env.Parameters.AttributeDisplay.order);
        assertFalse(env.Parameters.AttributeDisplay.highlightDifferences);
    }

    @Test
    @DisplayName("a configuration file written by an older eHOST keeps the defaults")
    void olderConfigurationFilesFallBackToTheDefaults() throws Exception {
        Files.write(configFile().toPath(),
                ("[ANNOTATOR]\nsomebody\n\n[RESTFUL_SERVER]\ntrue\n\n").getBytes("UTF-8"));

        env.Parameters.AttributeDisplay.order = env.Parameters.AttributeDisplay.Order.UNSORTED;
        env.Parameters.AttributeDisplay.highlightDifferences = false;

        SysConf.loadSystemConfigure(configHome.toString());

        // nothing in the file says otherwise, so the parameters stay untouched
        assertEquals(env.Parameters.AttributeDisplay.Order.UNSORTED,
                env.Parameters.AttributeDisplay.order);
        assertFalse(env.Parameters.AttributeDisplay.highlightDifferences);
    }

    @Test
    @DisplayName("an unreadable order falls back to the schema order")
    void unknownOrderFallsBackToSchema() throws Exception {
        Files.write(configFile().toPath(),
                ("[ATTRIBUTE_DISPLAY_ORDER]\nnot-an-order\n\n"
                        + "[HIGHLIGHT_ATTRIBUTE_DIFFERENCES]\nmaybe\n\n").getBytes("UTF-8"));

        env.Parameters.AttributeDisplay.order = env.Parameters.AttributeDisplay.Order.NAME;
        env.Parameters.AttributeDisplay.highlightDifferences = false;

        SysConf.loadSystemConfigure(configHome.toString());

        assertEquals(env.Parameters.AttributeDisplay.Order.SCHEMA,
                env.Parameters.AttributeDisplay.order);
        assertTrue(env.Parameters.AttributeDisplay.highlightDifferences,
                "anything but an explicit \"false\" keeps the highlighting on");
    }
}
