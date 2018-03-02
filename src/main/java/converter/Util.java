/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package converter;

import preAnnotate.outDated.listObject;
import java.io.File;
import java.util.ArrayList;
import javax.swing.JList;

/**
 *
 * @author Kyle
 */
public class Util
{
    //<editor-fold defaultstate="collapsed" desc="Constructor">
    /**
     * Constructor
     */
    public Util()
    {
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Public Methods">
    /**
     * *MULTI-USE* This function will arrange a list of files according to the passed
     * in extensions.  Any files that do not match will be thrown out, and any extensions
     * with no match will be filled in with a null.
     *
     * <pre>
     * ex.
     * Files{file.txt, file.rel, file.con, file.read}
     * extensions{txt, con, read, write}
     * return {file.txt, file.con, file.read, null}
     * </pre>
     *
     * @param params - the files to rearrange
     * @param extensions - the extensions to rearrange the files to match
     * @return - rearranged files
     */
    public ArrayList<listObject> arrangeByExtension(ArrayList<listObject> params, ArrayList<String> extensions)
    {
        //temp arraylist to hold the file parameters
        ArrayList<listObject> temp = new ArrayList<listObject>();

        //the arraylist that will be returned
        ArrayList<listObject> toReturn = new ArrayList<listObject>();

        //add the Files to the temp list
        temp.addAll(params);

        //Loop through each extension to try and find a match
        for (String extension : extensions)
        {
            //Originally set the file to null
            listObject toAdd = null;

            //Try to find a matching file
            for (listObject file : params)
            {
                //If the extension matches then this is the file we're looking for
                if (getExtension(file.getFile()).trim().toLowerCase().equals(extension))
                {
                    toAdd = file;
                }
            }
            //Add the matching file(will be null if nothing matches).
            toReturn.add(toAdd);
        }
        //Return the arranged files
        return toReturn;
    }

    /**
     * Get the extension for a file.
     * @param file - the file to get tne extension from
     * @return - return the extension
     */
    public String getExtension(File file)
    {
        String name = file.getName();
        if (!name.contains("."))
        {
            return name;
        }
        return name.substring(name.lastIndexOf(".") + 1);
    }

    /**
     * *MULTI-USE* This function will return match files with the same name.  Files are considered matches
     * if the name matches when the path and extension are stripped off.
     *
     * <pre>
     * ex.
     * C:/desktop/tests/123456.txt
     * matches
     * C:/dekstop/test2/123456.rel
     * </pre>
     *
     * @param allFiles - files to match
     * @return An ArrayList containg ArrayLists of matching files
     */
    public ArrayList<ArrayList<listObject>> getMatchingFiles(ArrayList<listObject> allFiles)
    {
        //Return list
        ArrayList<ArrayList<listObject>> toReturn = new ArrayList<ArrayList<listObject>>();

        //This list will match the allFiles list. trimmedFileNames.get(i) will
        //match allFiles.get(i).
        ArrayList<String> trimmedFileNames = getNames(allFiles);

        //Keep track of strings we have already matched.
        ArrayList<String> alreadyMatched = new ArrayList<String>();

        //Loop through all files to find a match
        for (int i = 0; i < trimmedFileNames.size(); i++)
        {
            //Create a new list of files to find any match.
            ArrayList<listObject> thisMatch = new ArrayList<listObject>();
            thisMatch.add(allFiles.get(i));

            //get the current name.
            String name = trimmedFileNames.get(i);

            //Make sure we haven't already used this file in a match
            if (alreadyMatched.contains(name))
            {
                continue;
            }

            //Add this name to the list of already matched filenames
            alreadyMatched.add(name);

            //Loop through all file names to find matches
            //If there is a match then add it to thisMatch list
            for (int j = i + 1; j < trimmedFileNames.size(); j++)
            {
                if (trimmedFileNames.get(j).equals(name))
                {
                    thisMatch.add(allFiles.get(j));
                }
            }

            //Add the match if the length is greater than zero.
            if (thisMatch.size() > 0)
            {
                toReturn.add(thisMatch);
            }
        }
        return toReturn;
    }
    /**
     * Get the names from an ArrayList of files.
     * <pre>
     * Example:
     *  Passed In: {(new File(C\\users\\desktop\\file.txt), (new File(C:\\users\\desktop\\anotherFile.txt)}
     *  Return: {file, anotherFile}
     * </pre>
     * @param allFiles - The files to find the names of.
     * @return - the names of all of the files.
     */
    public ArrayList<String> getNames(ArrayList<listObject> allFiles)
    {
        ArrayList<String> toReturn = new ArrayList<String>();
        for (listObject file : allFiles)
        {
            String nameWithoutExtension = getNameWithoutExtension(file.getFile().getName());
            toReturn.add(nameWithoutExtension);
        }
        return toReturn;

    }
    /**
     * This method will strip off the extension from a file object.
     * <pre>
     * Example:
     * Passed in file.txt -> return file
     * </pre>
     * @param name - the name of a file
     * @return the file with the extension stripped off.
     */
    public String getNameWithoutExtension(String name)
    {
        if (!name.contains("."))
        {
            return name;
        }
        return name.substring(0, name.indexOf("."));
    }
    /**
     * Extract the items from the list.
     * @param aList - A list containing listObjects
     * @return - An ArrayList of list objects.
     */
    public ArrayList<listObject> getList(JList aList)
    {
        ArrayList<listObject> sourceFiles = new ArrayList<listObject>();
        // Get number of items in the list
        int size = aList.getModel().getSize();
        if (size == 0)
        {
            return new ArrayList<listObject>();
        }
        // Get all item objects
        for (int i = 0; i < size; i++)
        {
            listObject temp = (listObject) aList.getModel().getElementAt(i);
            sourceFiles.add(temp);
        }
        return sourceFiles;
    }
    /**
     * This method will check to find files that are missing.  Files from the fileList
     * will be checked against the required extensions to see if there are any missing
     * entries.
     * <pre>
     * Example:
     *      ListObject{file.txt, file.con, file.rel, null} , Extensions{txt, con, rel, ast}
     *          -> ast will be added as a missing file for file.txt, file.con, and file.rel
     *      {file.txt, null, null, file.ast} , Extensions{txt, con, rel, ast}
     *          ->file.txt and file.ast will have con and rel as missing extensions.
     * </pre>
     * @param fileList - the parameters
     * @param requiredExtensions - the required extensions
     */
    public void checkNotMatching(ArrayList<ArrayList<listObject>> fileList, ArrayList<String> requiredExtensions)
    {

        for (ArrayList<listObject> potentialParameters : fileList)
        {
            ArrayList<String> tempExtensions = new ArrayList<String>();
            tempExtensions.addAll(requiredExtensions);
            for (listObject param : potentialParameters)
            {

                if (param == null)
                {
                    continue;
                }
                String name = param.getFile().getName();
                String extension = name.substring(name.lastIndexOf(".") + 1);
                tempExtensions.remove(extension.toLowerCase());
            }
            for (listObject param : potentialParameters)
            {
                if(param == null)
                    continue;
                param.clearMissingFiles();
                for (String extension : tempExtensions)
                {
                    param.addMissingFiles("Missing " + extension + " File");
                }
            }
        }
        //return problems;
    }
    //</editor-fold>
}
