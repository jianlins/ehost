/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package converter.fileConverters;

import converter.params.PinsToI2B2Params;
import converter.params.iParameterSet;
import converter.params.ParamList;
import converter.iGUI;
import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;

/**
 * This class will be used to do a file conversion between pins file format to
 * i2b2 file format.
 * <pre>
 * Input: .pins, .txt files
 * Output: .rel, .ast, .con files
 * </pre>
 * @author Kyle
 */
public class PinsToI2B2Converter implements iConversion
{
    //<editor-fold defaultstate="collapsed" desc="Private Strings for Allowable File Extensions">
    //Static strings for allowable extensions for each type of list
    private static String[] allowableSourceExts = new String[]
    {
        "txt"
    };
    private static String[] allowableConvertExts = new String[]
    {
        "pins"
    };
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Member Variables">
    private iGUI father;
    private ParamList list;
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Constructor">
    /**
     * Constructor for a PinsToI2B2Converter object
     * @param father - Where text output from this process should be written to.
     */
    public PinsToI2B2Converter(iGUI father)
    {
        this.father= father;
        list = new ParamList(ParamList.Type.pinsWithText);
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Private Methods">
    /**
     * Get a unique identifier to append to filenames
     * @return - the date and time as one string.
     */
    private String getDateTime()
    {
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        Date date = new Date();
        return dateFormat.format(date);
    }
    //</editor-fold>
    
    //<editor-fold defaultstate="collapsed" desc="Methods from iConversion">
    //Implements the abstract class from conversion class.(Refer to conversion class for documentation)
    public void convert(String output)
    {
        //Get separator
        String separator = env.Parameters.isUnixOS ? "/" : "\\";

        //Make a temporary file to write in between files(knowtator xml)
        File toWrite = new File("temp" + separator + "currentConversion" + separator + getDateTime());
        toWrite.mkdirs();

        //Extract and write xml files to the temporary directory
        PinsToXMLConverter toUse = new PinsToXMLConverter(father);
        Vector<iParameterSet> theList = list.getAllEntries();
        for (int i= 0; i< theList.size(); i++)
        {
            iParameterSet file = theList.get(i);
            PinsToI2B2Params temp = (PinsToI2B2Params)file;
            if(temp.getPins() == null || temp.getText() == null)
            {
                father.Output(temp.getName() + " Missing required files");
                continue;
            }
            toUse.addFilesDirectly(temp.getPins());
            father.setProgress((i+1)*50/theList.size(), "Intermediate Step(to XML): " + temp.getName());
        }
        toUse.convert(toWrite.getAbsolutePath());

        //Extract XML files from the temp folder and write them to i2b2 format.
        KnowtatorToi2b2 useAgain = new KnowtatorToi2b2(father);

        //add source files to xml->i2b2 object.
        for (int i =0; i< theList.size(); i++)
        {
            iParameterSet object = theList.get(i);
            PinsToI2B2Params temp = (PinsToI2B2Params)object;
            if(temp.getPins() == null || temp.getText() == null)
            {
                father.Output(temp.getName() + " Missing required files");
                continue;
            }
            for(File file: temp.getText())
                useAgain.addFilesDirectly(file);
            father.setProgress(((i+1)*50/theList.size()) + 50, "Final Step(to I2B2): " + temp.getName());
        }
        //Add new xml files to knowtator conver files
        for (File file : toWrite.listFiles())
        {
            useAgain.addFilesDirectly(file);
        }
        useAgain.convert(output);
    }
    /**
     * This method will add a listObject to the list of convert files without
     * having to use a file browser.
     * @param list - the object to add.
     */
    public void addFilesDirectly(File list)

    {
        //convertFiles.add(list);
        this.list.addAFile(list);
    }
    /**
     * This method will remove listObjects from our list of files ready to be converted.
     * @param toRemove - the listObjects to remove
     * @return - the list with these files removed
     */
    public Vector<iParameterSet> removeFiles(Vector<iParameterSet> toRemove)

    {
        for(iParameterSet object: toRemove)
        {
            list.removeAParamSet(object);
        }
        return list.getAllEntries();
    }
    public Vector<String> getExtensions()
    {
        Vector<String> lists = new Vector<String>();
        for(String extension : allowableConvertExts)
            lists.add(extension);
        for(String extension: allowableSourceExts)
            lists.add(extension);
        return lists;
    }
    public String getFileDescription()
    {
        String lists = "";
        for(String extension : allowableConvertExts)
            lists += extension + ", ";
        for(String extension: allowableSourceExts)
            lists += extension + ", ";
        lists = lists.substring(0, lists.length()-2);
        return lists;
    }
    public Vector<iParameterSet> getAllEntries()
    {
        return list.getAllEntries();
    }
    //</editor-fold>
    
}
