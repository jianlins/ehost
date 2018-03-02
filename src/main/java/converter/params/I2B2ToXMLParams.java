/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package converter.params;

import converter.Util;
import java.io.File;

/**
 *
 * @author Kyle
 */
public class I2B2ToXMLParams implements iParameterSet
{
    //<editor-fold defaultstate="collapsed" desc="MemberVariables">
    /**
     * The name of this file with no path or extension information
     */
    private String name;
    /**
     * The .rel file that has a stripped file name that matches name.
     */
    private File relFile;
    /**
     * The .con file that has a stripped file name that matches name.
     */
    private File conFile;
    /**
     * The .ast file that has a stripped file name that matches name.
     */
    private File astFile;
    /**
     * The .txt file that has a stripped file name that matches name.
     */
    private File txtFile;
    /**
     * The color that will be used to return a string representation.
     */
    private String color;
    /*
     * Utility to help with some basic file operations.
     */
    private Util util = new Util();
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Constructor">
    /**
     * Constructor
     * @param name - the name of this set of parameters with no extensions or path
     * information.
     */
    public I2B2ToXMLParams(String name)
    {
        this.name = name;
        color = "Black";
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="iParameterSet Methods">
    /**
     * Implemented from iParmeterSet interface.
     * @see converter.Params.iParameterSet#checkGood()
     */
    public boolean checkGood()
    {

        //Return false if the files are not set
        if (txtFile == null)
        {
            return false;
        }
        if (conFile == null)
        {
            return false;
        }
        if(relFile == null)
            return false;
        if(astFile == null)
            return false;

        //Return true if the parameters are all set
        return true;
    }

    /**
     * Implemented from iParmeterSet interface.
     * @see converter.Params.iParameterSet#addFile(File)
     */
    public void addFile(File file)
    {
        //Get stripped filename
        String thisName = file.getName();
        String stripped = util.getNameWithoutExtension(thisName);

        //If stripped filename equals the name of the parameter set then see
        //if the extension is needed.
        if (stripped.equals(this.name))
        {
            //Set parameters based on extension
            if (util.getExtension(file).toLowerCase().equals("txt"))
            {
                txtFile = file;
            }
            else if (util.getExtension(file).toLowerCase().equals("rel"))
            {
                relFile = file;
            }
            else if (util.getExtension(file).toLowerCase().equals("con"))
            {
                conFile = file;
            }
            else if (util.getExtension(file).toLowerCase().equals("ast"))
            {
                astFile = file;
            }
        }
    }

    /**
     * Implemented from iParmeterSet interface.
     * @see converter.Params.iParameterSet#checkFileShouldBeInside(File)
     */
    public boolean checkFileShouldBeInside(File file)
    {
        //Get filename with path and extension stripped off.
        String thisName = file.getName();
        String stripped = util.getNameWithoutExtension(thisName);

        //If this filename equals our parameter set name then check the extension
        if (stripped.equals(this.name))
        {
            //If the extension matches one that we need then return true
            if (util.getExtension(file).toLowerCase().equals("con")
                    || util.getExtension(file).toLowerCase().equals("ast")
                    || util.getExtension(file).toLowerCase().equals("rel")
                    || util.getExtension(file).toLowerCase().equals("txt"))
            {
                return true;
            }
            //Doesn't match an extension... return true.
            return false;
        }
        //doesn't match file name.. return false.
        return false;
    }

    /**
     * Implemented from iParmeterSet interface.
     * @see converter.Params.iParameterSet#getToolTipText()
     */
    public String getToolTipText()
    {
        //To hold the tool tips
        String tips = "";

        //Check each parameter to see if it is set... if it's not set add a tool
        //tip for it.
        if (conFile == null)
        {
            tips += "Missing con file<br>";
        }
        if (relFile == null)
        {
            tips += "Missing rel file<br>";
        }
        if (txtFile == null)
        {
            tips += "Missing text file<br>";
        }
        if (astFile == null)
        {
            tips += "Missing ast file<br>";
        }
        //If no tool tips then just return null
        if (tips.equals(""))
        {
            return null;
        }
        //Add html tagging and return
        tips = "<html>" + tips + "</html>";
        return tips;
    }

    /**
     * Implemented from iParmeterSet interface.
     * @see converter.Params.iParameterSet#flagProblems()
     */
    public void flagProblems()
    {
        //If any of our parameters are null then set the text color to red.
        if (conFile == null || relFile == null || astFile == null || txtFile == null)
        {
            color = "Red";
        }
        //If all required files are set then set color to black
        else
        {
            color = "Black";
        }
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
     * @return the relFile
     */
    public File getRelFile()
    {
        return relFile;
    }

    /**
     * @param relFile the relFile to set
     */
    public void setRelFile(File relFile)
    {
        this.relFile = relFile;
    }

    /**
     * @return the conFile
     */
    public File getConFile()
    {
        return conFile;
    }

    /**
     * @param conFile the conFile to set
     */
    public void setConFile(File conFile)
    {
        this.conFile = conFile;
    }

    /**
     * @return the astFile
     */
    public File getAstFile()
    {
        return astFile;
    }

    /**
     * @param astFile the astFile to set
     */
    public void setAstFile(File astFile)
    {
        this.astFile = astFile;
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

    //<editor-fold defaultstate="collapsed" desc="Overriden from Object">
    @Override
    public String toString()
    {
        return "<html><font color = \""+color+"\">" +name + "</font></html>";
    }
    //</editor-fold>
}
