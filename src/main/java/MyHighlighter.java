import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Shape;
import java.util.Vector;

import javax.swing.SwingUtilities;
import javax.swing.plaf.TextUI;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Document;
import javax.swing.text.Highlighter;
import javax.swing.text.JTextComponent;
import javax.swing.text.LayeredHighlighter;
import javax.swing.text.Position;
import javax.swing.text.View;

/**
 *
 *
 * @author Jianlin Shi
 * 
 */

public class MyHighlighter extends DefaultHighlighter {
    private final static Highlight[] noHighlights =
            new Highlight[0];
    private Vector<HighlightInfo> highlights = new Vector<HighlightInfo>();
    private JTextComponent component;
    private boolean drawsLayeredHighlights;
    private SafeDamager safeDamager = new SafeDamager();
	/**
     * Adds a highlight to the view.  Returns a tag that can be used
     * to refer to the highlight.
     *
     * @param p0   the start offset of the range to highlight &gt;= 0
     * @param p1   the end offset of the range to highlight &gt;= p0
     * @param p    the painter to use to actually render the highlight
     * @return     an object that can be used as a tag
     *   to refer to the highlight
     * @exception BadLocationException if the specified location is invalid
     */
    public Object addHighlight(int p0, int p1, HighlightPainter p) throws BadLocationException {
        if (p0 < 0) {
            throw new BadLocationException("Invalid start offset", p0);
        }

        if (p1 < p0) {
            throw new BadLocationException("Invalid end offset", p1);
        }

        Document doc = component.getDocument();
        HighlightInfo i = (getDrawsLayeredHighlights() &&
                           (p instanceof LayerPainter)) ?
                          new LayeredHighlightInfo() : new HighlightInfo();
        i.painter = p;
        i.p0 = doc.createPosition(p0);
        i.p1 = doc.createPosition(p1);
        highlights.addElement(i);
        safeDamageRange(p0, p1);
        return i;
    }
    
    /**
     * Queues damageRange() call into event dispatch thread
     * to be sure that views are in consistent state.
     */
    private void safeDamageRange(final Position p0, final Position p1) {
        safeDamager.damageRange(p0, p1);
    }

    /**
     * Queues damageRange() call into event dispatch thread
     * to be sure that views are in consistent state.
     */
    private void safeDamageRange(int a0, int a1) throws BadLocationException {
        Document doc = component.getDocument();
        safeDamageRange(doc.createPosition(a0), doc.createPosition(a1));
    }
    class HighlightInfo implements Highlight {

        public int getStartOffset() {
            return p0.getOffset();
        }

        public int getEndOffset() {
            return p1.getOffset();
        }

        public HighlightPainter getPainter() {
            return painter;
        }

        Position p0;
        Position p1;
        HighlightPainter painter;
    }


    /**
     * LayeredHighlightPainter is used when a drawsLayeredHighlights is
     * true. It maintains a rectangle of the region to paint.
     */
    class LayeredHighlightInfo extends HighlightInfo {

        void union(Shape bounds) {
            if (bounds == null)
                return;

            Rectangle alloc;
            if (bounds instanceof Rectangle) {
                alloc = (Rectangle)bounds;
            }
            else {
                alloc = bounds.getBounds();
            }
            if (width == 0 || height == 0) {
                x = alloc.x;
                y = alloc.y;
                width = alloc.width;
                height = alloc.height;
            }
            else {
                width = Math.max(x + width, alloc.x + alloc.width);
                height = Math.max(y + height, alloc.y + alloc.height);
                x = Math.min(x, alloc.x);
                width -= x;
                y = Math.min(y, alloc.y);
                height -= y;
            }
        }

        /**
         * Restricts the region based on the receivers offsets and messages
         * the painter to paint the region.
         */
        void paintLayeredHighlights(Graphics g, int p0, int p1,
                                    Shape viewBounds, JTextComponent editor,
                                    View view) {
            int start = getStartOffset();
            int end = getEndOffset();
            // Restrict the region to what we represent
            p0 = Math.max(start, p0);
            p1 = Math.min(end, p1);
            // Paint the appropriate region using the painter and union
            // the effected region with our bounds.
            union(((LayerPainter)painter).paintLayer
                  (g, p0, p1, viewBounds, editor, view));
        }

        int x;
        int y;
        int width;
        int height;
    }

    /**
     * This class invokes <code>mapper.damageRange</code> in
     * EventDispatchThread. The only one instance per Highlighter
     * is cretaed. When a number of ranges should be damaged
     * it collects them into queue and damages
     * them in consecutive order in <code>run</code>
     * call.
     */
    class SafeDamager implements Runnable {
        private Vector<Position> p0 = new Vector<Position>(10);
        private Vector<Position> p1 = new Vector<Position>(10);
        private Document lastDoc = null;

        /**
         * Executes range(s) damage and cleans range queue.
         */
        public synchronized void run() {
            if (component != null) {
                TextUI mapper = component.getUI();
                if (mapper != null && lastDoc == component.getDocument()) {
                    // the Document should be the same to properly
                    // display highlights
                    int len = p0.size();
                    for (int i = 0; i < len; i++){
                        mapper.damageRange(component,
                                p0.get(i).getOffset(),
                                p1.get(i).getOffset());
                    }
                }
            }
            p0.clear();
            p1.clear();

            // release reference
            lastDoc = null;
        }

        /**
         * Adds the range to be damaged into the range queue. If the
         * range queue is empty (the first call or run() was already
         * invoked) then adds this class instance into EventDispatch
         * queue.
         *
         * The method also tracks if the current document changed or
         * component is null. In this case it removes all ranges added
         * before from range queue.
         */
        public synchronized void damageRange(Position pos0, Position pos1) {
            if (component == null) {
                p0.clear();
                lastDoc = null;
                return;
            }

            boolean addToQueue = p0.isEmpty();
            Document curDoc = component.getDocument();
            if (curDoc != lastDoc) {
                if (!p0.isEmpty()) {
                    p0.clear();
                    p1.clear();
                }
                lastDoc = curDoc;
            }
            p0.add(pos0);
            p1.add(pos1);

            if (addToQueue) {
                SwingUtilities.invokeLater(this);
            }
        }
    }

}
