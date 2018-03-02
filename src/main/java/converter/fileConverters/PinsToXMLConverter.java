/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package converter.fileConverters;

import converter.params.PinsToXMLParams;
import converter.params.iParameterSet;
import resultEditor.annotations.Article;
import resultEditor.save.OutputToXML;
import resultEditor.io.PinFile;
import converter.Util;
import converter.params.ParamList;
import converter.iGUI;
import java.io.File;
import java.util.Vector;

/**
 * This class will be used to convert from pins files to XML files.
 * <pre>
 * input: pins files
 * output: xml files
 * </pre>
 * @author Kyle
 */
public class PinsToXMLConverter implements iConversion
{
    //<editor-fold defaultstate="collapsed" desc="Strings for Determining Allowable File Extensions">
    //The allowable file extensions
    private static String[] allowableSourceExts = new String[]{""};
    private static String[] allowableConvertExts = new String[]{"pins"};
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Extensions">
    private iGUI father;
    private ParamList list;
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Constructor">
    /**
     * Constructor
     * @param father - Mostly used to output results from this process.
     */
    public PinsToXMLConverter(iGUI father)
    {
        this.father = father;
        list = new ParamList(ParamList.Type.pinsNoText);
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Private Method">
    /**
     * This method will perform the conversion from pins to xml files and write the
     * xml files to the passed in output directory.
     * @param outputDirectory - the path for the output directory
     */
    private void pinsToXML(String outputDirectory)
    {
        Vector<iParameterSet> theList = list.getAllEntries();
        for (int i =0; i< theList.size(); i++)
        {
            iParameterSet param = theList.get(i);
            PinsToXMLParams info = (PinsToXMLParams)param;
            String name = info.getTxtFile().getAbsolutePath();
            //Extract the extension
            String extension = "";
            if (name.contains("."))
            {
                extension = name.substring(name.lastIndexOf("."));
            }
            boolean canRead = false;

            //If it matches a file extension of our readable formats then we can read it
            // so set canRead to true.

            if (".pins".toLowerCase().equals(extension.toLowerCase()))
            {
                canRead = true;
            }


            //If we can read it then process all of the annotators classes, and fileNames so they
            //are ready for use and add it to the list of files.
            if (canRead)
            {
                father.Output("Reading pins File: " + info.getTxtFile().getAbsolutePath() + "\n");
                PinFile toAdd = new PinFile(info.getTxtFile());
                toAdd.startParsing(false);
                toAdd.getAllAnnotators();
                toAdd.getAllClasses();
                toAdd.getAllFileNames();
                for (Article article : toAdd.articles)
                {
                    OutputToXML outputXML = new OutputToXML(article.filename, outputDirectory, article);
                    boolean worked = outputXML.write();
                    if (worked)
                    {
                        father.Output(article.filename + " written successfully!\n");
                    }
                }
            }
            father.setProgress((i+1)*100/theList.size(), "Converting: " + param.getName());

        }
        father.Output("Process Complete!\n");
    }
    //</editor-fold>
    
    //<editor-fold defaultstate="collapsed" desc="Methods from iConversion">
    //Implements the abstract class from conversion class.(Refer to conversion class for documentation)
    public void convert(String output)
    {
        pinsToXML(output);
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
