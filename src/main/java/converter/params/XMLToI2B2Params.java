/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package converter.params;

import converter.Util;
import java.io.File;

/**
 * @see converter.Params.iParameterSet
 * @author Kyle
 */
public class XMLToI2B2Params implements iParameterSet
{
    //<editor-fold defaultstate="collapsed" desc="Member Variables">
    //The name of this parameter set with path and extension stripped off.
    private String name;

    //Actual File Parameters
    private File xmlFile;
    private File txtFile;

    //Utility for file management
    private Util util = new Util();

    //Color for html toString method
    private String color;
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Constructor">
    /**
     * Constructor
     * @param name - the name of this set of parameters with the extensions and any
     * path information stripped off.
     */
    public XMLToI2B2Params(String name)
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
        //For holding tool tips
        String tips = "";

        //For any missing files add a line for the tooltip
        if (xmlFile == null)
        {
            tips += "Missing xml file<br>";
        }
        if (txtFile == null)
        {
            tips += "Missing text file<br>";
        }

        //If no tooltips just return null
        if (tips.equals(""))
        {
            return null;
        }

        //Add some html tagging and return
        tips = "<html>" + tips + "</html>";
        return tips;
    }

    /**
     * Implemented from iParmeterSet interface.
     * @see converter.Params.iParameterSet#flagProblems()
     */
    public void flagProblems()
    {
        //Set the color to red if we're missing some required files.
        if (txtFile == null)
        {
            color = "Red";
        }
        else if (xmlFile == null)
        {
            color = "Red";
        }
        //Set the text color to black if we have all required files.
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
        //If missing text file or xml files then return false
        if (txtFile == null)
        {
            return false;
        }
        if (xmlFile == null)
        {
            return false;
        }
        //If we aren't missing any files return true.
        return true;
    }

    /**
     * Implemented from iParmeterSet interface.
     * @see converter.Params.iParameterSet#addFile(File)
     */
    public void addFile(File file)
    {
        //Get filename with extensions and path info stripped off
        String thisName = file.getName();
        String stripped = util.getNameWithoutExtension(thisName);

        //If the stripped filename matches the name of our parameter set then see
        // if it matches extensions needed for this param set.
        if (stripped.equals(this.name))
        {
            //If it's a text file then set the textFile parameter
            if (util.getExtension(file).toLowerCase().equals("txt"))
            {
                txtFile = file;
            }
            //if its an xml file then set the xml file parameter
            else if (util.getExtension(file).toLowerCase().equals("xml"))
            {
                xmlFile = file;
            }
        }
    }

    /**
     * Implemented from iParmeterSet interface.
     * @see converter.Params.iParameterSet#checkFileShouldBeInside(File)
     */
    public boolean checkFileShouldBeInside(File file)
    {
        //Get filename with extension and path info stripped off.
        String thisName = file.getName();
        String stripped = util.getNameWithoutExtension(thisName);

        //If the stripped filename matches the name of our parameter set then see
        //if it matches an extension
        if (stripped.equals(this.name))
        {
            //If it matches a required extension return true.
            if (util.getExtension(file).toLowerCase().equals("xml")
                    || util.getExtension(file).toLowerCase().equals("txt"))
            {
                return true;
            }
            //return false if it doesn't match an extension
            return false;
        }
        //Return false if it didn't match the param set name
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
     * @return the xmlFile
     */
    public File getXmlFile()
    {
        return xmlFile;
    }

    /**
     * @param xmlFile the xmlFile to set
     */
    public void setXmlFile(File xmlFile)
    {
        this.xmlFile = xmlFile;
    }

    /**
     * @return the txtFile
     */
    public File getTxtFile()
    {
        return txtFile;
    }

    /**
     * @param txtFile the txtFile to set
     */
    public void setTxtFile(File txtFile)
    {
        this.txtFile = txtFile;
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Overriden from Object Class">
    @Override
    public String toString()
    {
        return "<html><font color = \"" + color + "\">" + name + "</font></html>";
    }
    //</editor-fold>
}
