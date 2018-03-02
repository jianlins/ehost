/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package converter.params;

import converter.Util;
import java.io.File;
import java.util.Vector;

/**
 *
 * @author Kyle
 */
public class ParamList
{
    //<editor-fold defaultstate="collapsed" desc="Enumeration for types of Parameter Lists">
    /**
     * The type of parameters that this list can handle.  Maybe replace this with
     * Generics? Look into how to do that exactly... and if it will work for
     * this situation.
     */
    public static enum Type  {
        i2b2,
        xml,
        pinsWithText,
        pinsNoText
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Member Variables">
    /*
     * The type of parameters that this list is currently holding.
     */
    private Type paramType;
    /*
     * The List of parameters currently in this list.
     */
    private Vector<iParameterSet> allEntries;
    /*
     * Utility to help with file management
     */
    private Util util = new Util();
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Constructor">
    /**
     * Constructor for this list of parameters
     * @param type - the type of parameters this list is holding.
     */
    public ParamList(Type type)
    {
        allEntries = new Vector<iParameterSet>();
        paramType = type;
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Public Methods">
    /**
     * Add a file to the list of current parameters.  If the parameter set already
     * exists, then the file will be added to it, it the parameter set does not
     * already exist then a new parameter set will be created with the name of the
     * passed in file.
     *
     * @param file - the file to add.
     */
    public void addAFile(File file)
    {
        boolean foundAHome = false;
        for(iParameterSet paramSet: getAllEntries())
        {
            if(paramSet.checkFileShouldBeInside(file))
            {
                foundAHome = true;
                paramSet.addFile(file);
            }
            paramSet.flagProblems();
        }
        if(!foundAHome)
        {
            boolean created = createNewParamSet(file);
            if(created)
                addAFile(file);
        }
    }
    /**
     * Remove a parameter set from the list of parameter sets that we're currently
     * keeping track of.
     * @param parameter - The parameter set to delete.
     */
    public void removeAParamSet(iParameterSet parameter)
    {
        this.allEntries.remove(parameter);
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Private Methods">
    /**
     * This will create a new parameter object if one does not exist that could
     * contain this file.  ***The file must still be added to the parameter set
     * after it is created.***
     * @param file - the file to create the new parameter set for.
     * @return - true if a new set was created, false otherwise.
     */
    private boolean createNewParamSet(File file)
    {
        //Swtich based on what type of list of parameters we're dealing with
        switch(paramType)
        {
            case i2b2:
                allEntries.add(new I2B2ToXMLParams(util.getNameWithoutExtension(file.getName())));
                return true;
            case xml:
                allEntries.add(new XMLToI2B2Params(util.getNameWithoutExtension(file.getName())));
                return true;
            case pinsWithText:
                allEntries.add(new PinsToI2B2Params(util.getNameWithoutExtension(file.getName())));
                return true;
            case pinsNoText:
                allEntries.add(new PinsToXMLParams(util.getNameWithoutExtension(file.getName())));
                return true;
        }
        return false;
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Getters and Setters">
    /**
     * @return the allEntries
     */
    public Vector<iParameterSet> getAllEntries()
    {
        return allEntries;
    }

    /**
     * @param allEntries the allEntries to set
     */
    public void setAllEntries(Vector<iParameterSet> allEntries)
    {
        this.allEntries = allEntries;
    }
    //</editor-fold>
}
