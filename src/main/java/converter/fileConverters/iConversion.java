/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package converter.fileConverters;

import converter.params.iParameterSet;
import java.io.File;
import java.util.Vector;

/**
 * An interface for an object that converts between two filetypes. This interface
 * will provide the methods required to manage the set of conversion files as well
 * as perform a file type conversion.
 * @author Kyle
 */
public interface iConversion
{
    //<editor-fold defaultstate="collapsed" desc="Unimplemented Interface Methods">
    /**
     * This method will return the file extensions that this conversion object accepts.
     * <pre>
     * For example:
     * if this conversion object requires .rel, .con, and .txt files
     * {rel, con, txt} will be returned (in no particular order).
     * </pre>
     * @return
     */
    public Vector<String> getExtensions();
    /**
     * This will return a description of the file types that this conversion object will
     * accept.
     * @return
     */
    public String getFileDescription();
    
    
    /**
     * This method will remove parameter sets from the list of files ready to be converted.
     * @param toRemove - the parameter sets to remove
     * @return - the list with these files removed
     */
    public Vector<iParameterSet> removeFiles(Vector<iParameterSet> toRemove);
    
    /**
     * Add a file to the list of convert files.
     * @param list - the object to add.
     */
    public void addFilesDirectly(File list);

    /**
     * Get all parameterSets that are currently in this conversion
     * object.
     * @return - all parameterSets in this conversion object.
     */
    public Vector<iParameterSet> getAllEntries();

    /**
     * Perform the file conversion.  All parameter sets currently in this object
     * will be converted to the specified file type if they have all of the files
     * needed to perform a conversion.
     * @param output - the directory to put the output files in.
     */
    public void convert(String output);
    //</editor-fold>
}
