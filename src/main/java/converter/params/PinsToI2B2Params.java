/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package converter.params;

import converter.Util;
import java.io.File;
import java.util.ArrayList;
import preAnnotate.readers.pinsFile;

/**
 * This class represents the parameters necessary to perform a conversion
 * from pins files to i2b2 files.
 * @author Kyle
 */
public class PinsToI2B2Params implements iParameterSet
{
    //<editor-fold defaultstate="collapsed" desc="Member Variables">
    private String name;
    private File[] text;
    private File pins;
    private Util util = new Util();
    private ArrayList<String> containedFileNames;
    private String color;
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Constructor">
    /**
     * Constructor
     * @param name - the name of each file in this set of parameters... with all extensions
     * and path information stripped off.
     */
    public PinsToI2B2Params(String name)
    {
        this.name = name;
        containedFileNames = new ArrayList<String>();
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
        //Used to hold the tool tips
        String tips = "";

        //Loop through all contained filenames to see if we are missing any that
        //are necessary... add a tip if we are.
        for(int i =0; i< containedFileNames.size(); i++)
        {
            if(text[i] == null)
                tips += ("Missing " + containedFileNames.get(i)+"<br>");
        }
        //If we're somehow missing a pins file add a tip for that.
        if (pins == null)
        {
            tips += "Missing pins file<br>";
        }

        //If we don't have any tool tips then just return null
        if (tips.equals(""))
        {
            return null;
        }

        //Add some html tagging and return.
        tips = "<html>" + tips + "</html>";
        return tips;
    }

    /**
     * Implemented from iParmeterSet interface.
     * @see converter.Params.iParameterSet#flagProblems()
     */
    public void flagProblems()
    {
        //If missing required files set the text to red
        if (!checkGood())
        {
            color = "Red";
        }

        //If we have all required files set the text color to black.
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
        //Return false if missing required files
        for(int i =0; i< containedFileNames.size(); i++)
        {
            if(text[i] == null)
                return false;
        }
        if (pins == null)
        {
            return false;
        }

        //If not missing any files return true
        return true;
    }

    /**
     * Implemented from iParmeterSet interface.
     * @see converter.Params.iParameterSet#addFile(File)
     */
    public void addFile(File file)
    {
        //Get filename with extension and path stripped off
        String thisName = file.getName();
        String stripped = util.getNameWithoutExtension(thisName);

        //If the stripped filename is equal to this parameter set name then see
        //if it matches one of the extensions we want
        if (stripped.equals(this.name))
        {
            //If it's a text file then set our text member variable to this file
            /*
            if (util.getExtension(file).toLowerCase().equals("txt"))
            {
                //text = file;
            }
             * 
             */
            //If it is a pins file then set our pins member variable to this file
            // and do some preprocessing.
            if (util.getExtension(file).toLowerCase().equals("pins"))
            {
                pins = file;
                preAnnotate.readers.pinsFile aPinFile = new pinsFile(pins);
                containedFileNames = aPinFile.getAllFileNames();
                text = new File[containedFileNames.size()];
            }
        }
        //Loop through all needed text files to see if the passed in file matches
        //one that we need.
        for(int i = 0; i< containedFileNames.size(); i++)
        {
            String currentName = containedFileNames.get(i);
            String strippedName = currentName.substring(0, currentName.indexOf("."));
            if(strippedName.equals(name))
            {
                text[i] = file;
            }

        }
    }

    /**
     * Implemented from iParmeterSet interface.
     * @see converter.Params.iParameterSet#checkFileShouldBeInside(File)
     */
    public boolean checkFileShouldBeInside(File file)
    {
        //Get filename with path and extensions stripped off.
        String thisName = file.getName();
        String stripped = util.getNameWithoutExtension(thisName);
        /*
        if (stripped.equals(this.name))
        {

            if (util.getExtension(file).toLowerCase().equals("pins"))
            {
                return true;
            }
        }
         * 
         */
        //Loop through all text files that this pins files need and add this file if it is a
        //text file and matches the name of the needed file.
        for(int i = 0; i< containedFileNames.size(); i++)
        {
            //Extract the stripped name of the needed file
            String currentname = containedFileNames.get(i);
            String strippedName = currentname.substring(0, currentname.indexOf("."));

            //If the passed in file matches the needed file and is a .txt file then return true.
            if(strippedName.equals(name) && util.getExtension(file).toLowerCase().equals(".txt"))
            {
                return true;
            }

        }
        //Return false if it isn't a needed file.
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
     * @return the text
     */
    public File[] getText()
    {
        return text;
    }

    /**
     * @param text the text to set
     */
    public void setText(File[] text)
    {
        this.text = text;
    }

    /**
     * @return the pins
     */
    public File getPins()
    {
        return pins;
    }

    /**
     * @param pins the pins to set
     */
    public void setPins(File pins)
    {
        this.pins = pins;
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
