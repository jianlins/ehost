/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package converter.params;

import converter.Util;
import java.io.File;

/**
 * This class will contain the parameters required for a pins to xml extraction.
 *
 * @author Kyle
 */
public class PinsToXMLParams implements iParameterSet
{
    //<editor-fold defaultstate="collapsed" desc="Member Variables">
    //The name of this parameter set... should be derived from a file name with
    //no path information or extensions.
    private String name;

    //The text file, the first and only parameter for this parameter set.
    private File pinsFile;

    //Util to help with file management
    private Util util = new Util();

    //The color of the text that will be returned with the toString method.
    private String color;
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Constructor">
    /**
     * Constructor
     * @param name - the name of this set of parameters with no extensions or path
     * information.
     */
    public PinsToXMLParams(String name)
    {
        this.name = name;
        color = "Black";
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Implemented from iParameterSet">
    /**
     * Implemented from iParmeterSet interface.
     * @see converter.Params.iParameterSet#getToolTipText()
     */
    public String getToolTipText()
    {
        //To hold the tool tip
        String tips = "";

        //If the text file is null then add a tool tip(Acually shouldn't ever happen)
        if (pinsFile == null)
        {
            tips += "Missing pins file<br>";
        }
        //If there is no tool tip then just return null.
        if (tips.equals(""))
        {
            return null;
        }
        //Add some html tagging.
        tips = "<html>" + tips + "</html>";

        //return tool tips
        return tips;
    }
    /**
     * Implemented from iParmeterSet interface.
     * @see converter.Params.iParameterSet#flagProblems()
     */
    public void flagProblems()
    {
        //If we're missing files then set the color to red
        if (pinsFile == null)
        {
            color = "Red";
        }
        //If we're not missing any files then set the text color to black
        else
        {
            color = "Black";
        }
    }
    /**
     * Implemented from iParmeterSet interface.
     * @see converter.Params.iParameterSet#checkGood()
     */
    public boolean checkGood()
    {
        //If we're missing required files then return false
        if (pinsFile == null)
        {
            return false;
        }
        //We have required files so return true
        return true;
    }
    /**
     * Implemented from iParmeterSet interface.
     * @see converter.Params.iParameterSet#addFile(File)
     */
    public void addFile(File file)
    {
        //Get file name
        String thisName = file.getName();

        //Strip off file extension
        String stripped = util.getNameWithoutExtension(thisName);

        //If the stripped name matches the name of this param set then see
        //if the extension matches an extension we want.
        if (stripped.equals(this.name))
        {
            //If it is a txt file then set our pinsFile to this file.
            if (util.getExtension(file).toLowerCase().equals("pins"))
            {
                pinsFile = file;
            }
        }
    }
    /**
     * Implemented from iParmeterSet interface.
     * @see converter.Params.iParameterSet#checkFileShouldBeInside(File)
     */
    public boolean checkFileShouldBeInside(File file)
    {
        //Get the name of this file
        String thisName = file.getName();

        //Get the file name with extension stripped off.
        String stripped = util.getNameWithoutExtension(thisName);

        //If the file name matches the name of this param set
        if (stripped.equals(this.name))
        {
            //If its a pins file then return true
            if (util.getExtension(file).toLowerCase().equals("pins"))
            {
                return true;
            }

            //return false if it does not match an extension that we want.
            return false;
        }

        //Return if the filename does not match the name of this param set.
        return false;
    }

    /**
     * @return the name
     */
    public String getName()
    {
        return name;
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Getters and Setters">
    /**
     * @param name the name to set
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * @return the txtFile
     */
    public File getTxtFile()
    {
        return pinsFile;
    }

    /**
     * @param txtFile the txtFile to set
     */
    public void setTxtFile(File txtFile)
    {
        this.pinsFile = txtFile;
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Public Methods">
    @Override
    public String toString()
    {
        return "<html><font color = \"" + color + "\">" + name + "</font></html>";
    }
    //</editor-fold>
}
