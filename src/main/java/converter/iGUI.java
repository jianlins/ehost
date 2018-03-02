/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package converter;

/**
 * This interface is a interface for GUI's that need to output text and set
 * a progress bar.
 * @author Kyle
 */
public interface iGUI
{
    //<editor-fold defaultstate="collapsed" desc="Unimplemented Interface Methods">
    /**
     * This method will output text.
     * @param output - The text to output.
     */
    public void Output(String output);
    /**
     * This method will set the progress of this object.
     * @param progress - progress percentage
     * @param process - a description of the process
     */
    public void setProgress(int progress, String process);
    //</editor-fold>
}
