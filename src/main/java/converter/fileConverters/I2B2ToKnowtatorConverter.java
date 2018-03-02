/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package converter.fileConverters;

import converter.params.iParameterSet;
import converter.params.I2B2ToXMLParams;
import com.sun.org.apache.xml.internal.serialize.OutputFormat;
import com.sun.org.apache.xml.internal.serialize.XMLSerializer;
import converter.params.ParamList;
import converter.iGUI;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Vector;

/**
 * This class extends the conversion class and will perform a conversion between
 * the i2b2 file format to knowtator xml.
 * <pre>
 * required i2b2 files are:
 *  .rel
 *  .ast
 *  .con
 *  .txt
 * Output is:
 *  .knowtator.xml(one for each set of required i2b2 files)
 * </pre>
 * @author Kyle
 */
public class I2B2ToKnowtatorConverter implements iConversion
{
    //<editor-fold defaultstate="collapsed" desc="Private Strings for Allowable Extensions">
    //Allowable extensions
    private static String[] allowableSourceExts = new String[]
    {
        "txt"
    };
    private static String[] allowableConvertExts = new String[]
    {
        "rel", "con", "ast"
    };
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Member Variables">
    //Used for Outputting results and updating progress
    private iGUI father;
    //List of parameters to extract
    private ParamList list;
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Constructor">
    /**
     * Constructor
     * @param father - mostly used to output some text results of this process.
     */
    public I2B2ToKnowtatorConverter(iGUI father)
    {
        this.father = father;
        list = new ParamList(ParamList.Type.i2b2);
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Methods from iConversion">
    public void convert(String output)
    {
        //ArrayList<ArrayList<listObject>> files = getI2B2Params(super.getConvertFiles(), super.getSourceFiles());
        Vector<iParameterSet> temp = list.getAllEntries();
        ArrayList<I2B2ToXMLParams> toSend = new ArrayList<I2B2ToXMLParams>();
        for (iParameterSet param : temp)
        {
            toSend.add((I2B2ToXMLParams) param);
        }
        i2b2ToKnowtator(toSend, output);

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
        for (iParameterSet object : toRemove)
        {
            list.removeAParamSet(object);
        }
        return list.getAllEntries();
    }

    public Vector<String> getExtensions()
    {
        Vector<String> lists = new Vector<String>();
        for (String extension : allowableConvertExts)
        {
            lists.add(extension);
        }
        for (String extension : allowableSourceExts)
        {
            lists.add(extension);
        }
        return lists;
    }

    public String getFileDescription()
    {
        String lists = "";
        for (String extension : allowableConvertExts)
        {
            lists += extension + ", ";
        }
        for (String extension : allowableSourceExts)
        {
            lists += extension + ", ";
        }
        lists = lists.substring(0, lists.length() - 2);
        return lists;
    }

    public Vector<iParameterSet> getAllEntries()
    {
        return list.getAllEntries();
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Private Methods">
    /**
     * This method will do the conversion from i2b2 format to knowtator xml format.
     * @param files - the parameters to do the extraction.  Each Entry in the top
     * level ArrayList contains the parameters for a single run.
     * @see #getI2B2Params(Vector, Vector)
     */
    private void i2b2ToKnowtator(ArrayList<I2B2ToXMLParams> files, String output)
    {
        int countGood = 0;
        int countBad = 0;
        //Loop through all parameters and perform a conversion for every set of parameters.
        for (int i = 0; i < files.size(); i++)
        {
            I2B2ToXMLParams params = files.get(i);
            //To store parameters
            File text = params.getTxtFile();
            File concept = params.getConFile();
            File relation = params.getRelFile();
            File assertion = params.getAstFile();

            //Create a new object to do the extraction
            PipelineToKnowtator knowtator = new PipelineToKnowtator();

            //If it is missing the text or concept file... then don't bother running a conversion
            if (text == null || concept == null)
            {
                father.Output(params.getName() + " missing required file(s)\n");
                father.setProgress((i + 1) * 100 / files.size(), "Converting: " + params.getName());
                countBad++;
                continue;
            }

            //Try to perform the extraction
            try
            {
                org.w3c.dom.Document xmlDoc = knowtator.convertI2B2toXML(text, concept, relation, assertion);

                //Save XML file
                FileOutputStream fos = new FileOutputStream(output + "/" + params.getName() + ".txt.knowtator.xml");
                OutputFormat of = new OutputFormat("XML", "ISO-8859-1", true);
                of.setIndenting(true);
                XMLSerializer ser = new XMLSerializer(fos, of);
                ser.serialize(xmlDoc.getDocumentElement());
                fos.close();

                //Output the text
                father.Output(params.getName() + " converted successfully!\n");
                father.setProgress((i + 1) * 100 / files.size(), "Converting: " + params.getName());
                countGood ++;
            }
            //Catch any exceptions from the conversion
            catch (Exception e)
            {
                //If there are some parameters then try to get a name out of it.
                String name = params.getName();
                father.Output(name + " failed to convert!\n");
                father.Output("<" + params.getName() + " ERRORS START" + ">\n");
                father.Output(e.getMessage() + "\n");
                father.Output("<" + params.getName() + " ERRORS END" + ">\n");
                father.setProgress((i + 1) * 100 / files.size(), "Converting: " + params.getName());
                countBad++;

            }
        }
        //print out some final information
        father.Output(countGood+"/"+files.size()+ " converted successfully.\n");
        if(countBad > 0)
            father.Output(countBad + " files FAILED to convert\n");
    }
    //</editor-fold>
}
