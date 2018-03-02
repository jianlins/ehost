import javax.swing.JTextArea;
import javax.swing.text.Document;

/**
 *
 *
 * @author Jianlin Shi
 * 
 */

public class MyJTextArea extends JTextArea {
	private transient MyHighlighter highlighter;

	public MyJTextArea() {
		// TODO Auto-generated constructor stub
	}

	public MyJTextArea(String text) {
		super(text);
		// TODO Auto-generated constructor stub
	}

	public MyJTextArea(Document doc) {
		super(doc);
		// TODO Auto-generated constructor stub
	}

	public MyJTextArea(int rows, int columns) {
		super(rows, columns);
		// TODO Auto-generated constructor stub
	}

	public MyJTextArea(String text, int rows, int columns) {
		super(text, rows, columns);
		// TODO Auto-generated constructor stub
	}

	public MyJTextArea(Document doc, String text, int rows, int columns) {
		super(doc, text, rows, columns);
		// TODO Auto-generated constructor stub
	}
	
	   /**
     * Fetches the object responsible for making highlights.
     *
     * @return the highlighter
     */
    public MyHighlighter getHighlighter() {
        return highlighter;
    }
    
    /**
     * Sets the highlighter to be used.  By default this will be set
     * by the UI that gets installed.  This can be changed to
     * a custom highlighter if desired.  The highlighter can be set to
     * <code>null</code> to disable it.
     * A PropertyChange event ("highlighter") is fired
     * when a new highlighter is installed.
     *
     * @param h the highlighter
     * @see #getHighlighter
     * @beaninfo
     *  description: object responsible for background highlights
     *        bound: true
     *       expert: true
     */
    public void setHighlighter(MyHighlighter h) {
        if (highlighter != null) {
            highlighter.deinstall(this);
        }
        MyHighlighter old = highlighter;
        highlighter = h;
        if (highlighter != null) {
            highlighter.install(this);
        }
        firePropertyChange("highlighter", old, h);
    }

}
