/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package converter;

import converter.fileConverters.I2B2ToKnowtatorConverter;
import converter.fileConverters.KnowtatorToi2b2;
import converter.fileConverters.PinsToI2B2Converter;
import converter.fileConverters.PinsToXMLConverter;
import converter.fileConverters.iConversion;

/**
 *
 * @author Kyle
 */
public class TypeMatcher
{
    //<editor-fold defaultstate="collapsed" desc="Private String Arrays for Determining Allowed Conversions">
    //Formats that the FileConverterGUI currently handles
    private static String[] formats = new String[]
    {
        "I2B2", "KNOWTATOR XML", "PINS"
    };
    //formats that pins can be written from
    private static String[] pinsAllowed = new String[]
    {
        "", formats[0], formats[1]
    };
    //formats that knowtator xml can be written from
    private static String[] knowtatorXMLAllowed = new String[]
    {
        "", formats[0]
    };
    //Formats that i2b2 can be written from
    private static String[] i2b2Allowed = new String[]
    {
        "", formats[1]
    };

    //TODO add more string[] here
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Public functions">
    /**
     * Return the initial list of Type choices.  This list includes all file types
     * that are involved in any conversion.
     * @return
     */
    public static String[] getInitialChoices()
    {
        return formats;
    }
    
    /**
     * Get the list of allowed 'to' types based on a given 'from' type.
     * <pre>
     *      ex. if the following conversions exist:
     *          xml -> i2b2
     *          i2b2 -> xml
     *          i2b2 -> pins
     *      if xml is passed in {i2b2} will be returned
     *      if i2b2 is passed in {xml, pins} will be returned
     * </pre>
     * @param from - The String for the 'from' type.  Files of this type will be converted.
     * @return - the values that the given 'from' type can be converted to.
     */
    public static String[] getToByFrom(String from)
    {
        if(from.equals(formats[0]))
        {
            return i2b2Allowed;
        }
        else if(from.equals(formats[1]))
        {
            return knowtatorXMLAllowed;
        }
        else if(from.equals(formats[2]))
        {
            return pinsAllowed;
        }
        else
        {
            return null;
        }
        //TODO: Add more file types here
    }
    /**
     * Return an iConversion object that can be used to converted from the given 'from'
     * file type to the 'to' file type.
     * @param from - the type that the file came from
     * @param to - the type the file will be converted to
     * @param gui - the GUI for output
     * @return - the iConversion object for converting between the filetypes.
     */
    public static iConversion getWorkerByToAndFrom(String from, String to, iGUI gui)
    {
        iConversion work = null;
        //User selected i2b2 -> knowtator XML
        if(to.equals(formats[1]) && from.equals(formats[0]))
        {
            work = new I2B2ToKnowtatorConverter(gui);
        }
        //User selected Knowtator XML -> i2b2
        else if(from.equals(formats[1]) && to.equals(formats[0]))
        {
            work = new KnowtatorToi2b2(gui);
        }
        //User selected pins -> Knowtator XML
        else if(from.equals(formats[2]) && to.equals(formats[1]))
        {
            work = new PinsToXMLConverter(gui);
        }
        //User selected pins -> i2b2
        else if(from.equals(formats[2]) && to.equals(formats[0]))
        {
            work = new PinsToI2B2Converter(gui);
        }
        //TODO: Add more return return types here
        return work;
    }
    //</editor-fold>
}
