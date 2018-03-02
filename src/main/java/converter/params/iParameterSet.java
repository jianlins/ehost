/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package converter.params;

import java.io.File;

/**
 * This interface will be used for sets of parameters that will be used to Convert
 * between file types.  These parameters will consist of all files that are required
 * for a conversion between file types.
 * @author Kyle
 */
public interface iParameterSet {
    //<editor-fold defaultstate="collapsed" desc="Unimplemented Interface Methods">
    /**
     * This method will add files to the set of parameters.  Files should only be
     * added if the checkFileShouldBeInside method returned true, otherwise no change
     * will be made.
     *
     * @param toAdd - the File to be added to the parameter set.  If the file name does
     * not match the getName() then no change will be made. The file extension also must
     * match an extension that the parameter accepts.
     */
    public void addFile(File toAdd);
    /**
     * This method will check to see if a set of parameters is ready for an extraction.
     * Required files must be contained in the set of parameters to return true.
     *
     * @return true, if the parameter set is ready to go, false otherwise.
     */
    public boolean checkGood();
    /**
     * This method will return the name of this parameter set which will be the name
     * of a file with the extensions and path information stripped off.
     *
     * @return - the name of this parameter set.
     */
    public String getName();
    /**
     * Check if a file should be inside of this parameter set.  To be inside of this
     * parameter set, the file name must match the getName() method and must have an
     * extension that this parameter set requires.
     *
     * @param file - the file to check
     * @return - true if the file belongs in this parmeter set, false otherwise.
     */
    public boolean checkFileShouldBeInside(File file);
    /**
     * This method will check for any problems with the parameter set.
     */
    public void flagProblems();
    /**
     * This method will return any tool tips associated with this parameter set.
     * This includes tips to indicate that users are missing files.
     *
     * @return - tool tip for this parameter set.
     */
    public String getToolTipText();
    //</editor-fold>

}
