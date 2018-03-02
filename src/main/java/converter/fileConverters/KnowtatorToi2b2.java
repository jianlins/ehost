/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package converter.fileConverters;

import converter.params.iParameterSet;
import converter.params.XMLToI2B2Params;
import converter.Util;
import converter.params.ParamList;
import converter.iGUI;
import java.io.File;
import java.util.ArrayList;
import java.util.Vector;

/**
 * This class will perform the extraction from Knowtator to I2B2 files.
 * <pre>
 * Input files(every xml file should have a matching txt file):
 *  Knowtator XML      (knowtator.xml)
 *  Matching Text Files(.txt)
 *
 * Output Files:
 *  relation files (.rel)
 *  concept files  (.con)
 *  assertion files(.ast)
 * </pre>
 * @author Kyle
 */
public class KnowtatorToi2b2 implements iConversion
{
    //<editor-fold defaultstate="collapsed" desc="Private Strings for Allowable File Extensions">
    //Allowable extensions
    private static String[] allowableSourceExts = new String[]{"txt"};
    private static String[] allowableConvertExts = new String[]{"xml"};
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Member Variables">
    private iGUI father;
    private ParamList list;
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Constructor">
    /**
     * Constructor
     * @param father - Mostly used to output results of this process
     */
    public KnowtatorToi2b2(iGUI father)
    {
        this.father = father;
        list = new ParamList(ParamList.Type.xml);
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Private Methods">
    /**
     * This method will do the conversion from knowtator XML files to i2b2 files.
     * <pre>
     * (xml,txt) -> (rel, con, ast)
     * </pre>
     * @param files - the parameters to perform the extraction with.
     * @param outputPath - the output directory for the rel, con, and ast
     * @see #getKnowtatorParams(Vector, Vector)
     */
    private void knowtatorToi2b2(ArrayList<XMLToI2B2Params> files, String outputPath)
    {

        //Loop through each set of parameters and perform an extraction for each.
        for (int i =0; i< files.size(); i++)
        {
            XMLToI2B2Params params = files.get(i);
            //For each set of parameters.
            File text = params.getTxtFile();
            File xml = params.getXmlFile();

            //Used to do the actual conversion
            KnowtatorToPipeline knowtator = new KnowtatorToPipeline();

            //If we're missing either the text or the xml files return.
            if(text ==null || xml == null)
            {
                father.Output(params.getName() + " missing required files\n");
                father.setProgress((i+1)*100/files.size(), "Converting: " + params.getName());
                continue;
            }

            //Used to hold any errors from the conversion
            ArrayList<String> errors = new ArrayList<String>();

            //Try the conversion
            try
            {
                errors = knowtator.convertXMLtoI2B2(text, xml, new File(outputPath));
                father.Output(params.getName() + " Converted successfully!\n");
                if(errors.size() >0)
                {
                    father.Output("<"+params.getName()+ " ERRORS START"+ ">\n");
                    for(String error: errors)
                            father.Output(error + "\n");
                    father.Output("<"+params.getName()+ " ERRORS END"+ ">\n");
                }
                father.setProgress((i+1)*100/files.size(), "Converting: " + params.getName());
            }
            //catch any exceptions
            catch (Exception e)
            {
                String fileName = params.getName();
                if (fileName != null)
                {
                    father.Output(fileName + " Failed to convert!\n");
                    for(String error: errors)
                        father.Output(error + "\n");
                }
                father.setProgress((i+1)*100/files.size(), "Converting: " + params.getName());
            }

        }
    }
    //</editor-fold>
    
    //<editor-fold defaultstate="collapsed" desc="Methods from iConversion">
    //Implements the abstract class from conversion class.(Refer to conversion class for documentation)
    public void convert(String output)
    {
        //ArrayList<ArrayList<listObject>> files = getKnowtatorParams(super.getConvertFiles(), super.getSourceFiles());
        Vector<iParameterSet> temp = list.getAllEntries();
        ArrayList<XMLToI2B2Params> casted= new ArrayList<XMLToI2B2Params>();
        for(iParameterSet param: temp)
            casted.add((XMLToI2B2Params)param);
        knowtatorToi2b2(casted, output);
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
