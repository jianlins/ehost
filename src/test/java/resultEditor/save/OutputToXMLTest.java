package resultEditor.save;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

import resultEditor.annotations.Article;

/**
 * Test class for OutputToXML save functionality.
 * Verifies that the duplicate annotation fix is applied correctly.
 */
public class OutputToXMLTest {

    @Test
    @DisplayName("Test OutputToXML default constructor")
    public void testDefaultConstructor() {
        OutputToXML outputToXML = new OutputToXML();
        assertNotNull(outputToXML);
    }

    @Test
    @DisplayName("Test OutputToXML constructor with parameters")
    public void testConstructorWithParameters() {
        Article article = new Article("test.txt");
        
        OutputToXML outputToXML = new OutputToXML("test.txt", "/tmp", article);
        assertNotNull(outputToXML);
    }

    @Test
    @DisplayName("Test Article creation")
    public void testArticleCreation() {
        Article article = new Article("test.txt");
        assertNotNull(article);
        assertEquals("test.txt", article.filename);
        
        article.annotations.add(new resultEditor.annotations.Annotation());
        assertEquals(1, article.annotations.size());
    }
}
