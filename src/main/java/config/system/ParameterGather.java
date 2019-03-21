/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package config.system;

import config.Block;

import java.io.File;
import java.math.BigInteger;
import java.util.Vector;
import java.util.logging.Level;

import env.Parameters;
import webservices.PBECoder;

/**
 * @author leng
 */
public class ParameterGather {


    /**
     * Load parameters into memory: In each of these splitted blocks,
     * parametername indicates the name of
     * a parameters, and all strings stored in the vector named "values" are
     * its values defined in repetitive formats.
     */
    public void load(Vector<Block> _blocks) {

        if ((_blocks == null) || (_blocks.size() < 1))
            return;

        try {
            for (Block block : _blocks) {

                if (block == null)
                    continue;

                switch (block.parameterName) {
                    case "WORKSPACE_PATH":
                        getLatestUsedWorkspace(block);
                        break;
                    case "ANNOTATOR":
                        getLatestUsedAnnotator(block);
                        break;
                    case "ANNOTATOR_ID":
                        getLatestUsedAnnotatorID(block);
                        break;
                    case "MASK":
                        getMask(block);
                        break;
                    case "ENABLE_DIFF_BUTTON":
                        getDIffButtonStatus(block);
                        break;
                    case "OracleFunction":
                        getOracleVisibleStatus(block);
                        break;
                    case "UMLS_USERNAME":
                        getUMLSUsername(block);
                        break;
                    case "UMLS_PASSWORD":
                        getUMLSPassword(block);
                        break;
                    case "RESTFUL_SERVER":
                        getRESTFULServerConfig(block);
                        break;
                }

            }
        } catch (Exception ex) {

        }
    }


    private static void getMask(Block _block) {

        try {
            if (_block == null)
                return;

            if ((_block.values == null) || (_block.values.size() != 1))
                return;

            env.Parameters.Sysini.functions = new char[6];
            for (int i = 0; i < 6; i++) {
                env.Parameters.Sysini.functions[i] = 0;
            }
            env.Parameters.Sysini.functions[1] = 1;
            env.Parameters.Sysini.functions[5] = 1;


            // check validity
            String strPath = _block.values.get(0);
            if ((strPath != null) && (strPath.trim().length() == 6)) {
                for (int i = 0; i < 6; i++) {
                    env.Parameters.Sysini.functions[i] = strPath.charAt(i);
                }
            }


        } catch (Exception ex) {
            log.LoggingToFile.log(Level.SEVERE, "error 1101141245:: fail to get mark to enable or disable functions" +
                    " of workspace!"
                    + ex.getMessage());
        }
    }

    private static void getDIffButtonStatus(Block _block) {

        try {
            env.Parameters.EnableDiffButton = false;

            if (_block == null)
                return;

            if ((_block.values == null) || (_block.values.size() != 1))
                return;

            // check validity
            String enabled = _block.values.get(0);
            ;
            if (enabled == null)
                env.Parameters.EnableDiffButton = false;
            else {
                if (enabled.trim().toLowerCase().compareTo("true") == 0)
                    env.Parameters.EnableDiffButton = true;
                else
                    env.Parameters.EnableDiffButton = false;
            }


        } catch (Exception ex) {
            log.LoggingToFile.log(Level.SEVERE, "error 1101141245:: fail to get mark to enable or disable functions" +
                    " of workspace!"
                    + ex.getMessage());
        }
    }


    private static void getOracleVisibleStatus(Block _block) {

        try {
            env.Parameters.OracleStatus.sysvisible = true;

            if (_block == null)
                return;

            if ((_block.values == null) || (_block.values.size() != 1))
                return;

            // check validity
            String enabled = _block.values.get(0);
            ;
            if (enabled == null)
                env.Parameters.EnableDiffButton = false;
            else {
                if (enabled.trim().toLowerCase().compareTo("true") == 0)
                    env.Parameters.OracleStatus.sysvisible = true;
                else
                    env.Parameters.OracleStatus.sysvisible = false;
            }


        } catch (Exception ex) {
            log.LoggingToFile.log(Level.SEVERE, "error 1101141245:: fail to get mark to enable or disable functions" +
                    " of workspace!"
                    + ex.getMessage());
        }
    }


    private static void getLatestUsedAnnotatorID(Block _block) {
        try {
            if (_block == null)
                return;

            if ((_block.values == null) || (_block.values.size() != 1))
                return;

            // check validity
            String id = _block.values.get(0);

            if ((id == null) || (id.trim().length() < 1)) {
                resultEditor.annotator.Manager.setCurrentAnnotatorID(null);
            } else {
                resultEditor.annotator.Manager.setCurrentAnnotatorID(id.trim());
            }

        } catch (Exception ex) {
            log.LoggingToFile.log(Level.SEVERE, "error 1101141245:: fail to get latest used path" +
                    " of workspace!"
                    + ex.getMessage());
        }
    }

    private static void getLatestUsedAnnotator(Block _block) {

        try {
            if (_block == null)
                return;

            if ((_block.values == null) || (_block.values.size() != 1))
                return;

            // check validity
            String strPath = _block.values.get(0);

            if ((strPath == null) || (strPath.trim().length() < 1)) {
                resultEditor.annotator.Manager.setCurrentAnnotator(null);
            } else {
                resultEditor.annotator.Manager.setCurrentAnnotator(strPath.trim());
            }


        } catch (Exception ex) {
            log.LoggingToFile.log(Level.SEVERE, "error 1101141245:: fail to get latest used path" +
                    " of workspace!"
                    + ex.getMessage());
        }
    }

    private static void getRESTFULServerConfig(Block _block) {
        try {
            if (_block == null)
                return;

            if ((_block.values == null) || (_block.values.size() != 1))
                return;

            String strPath = _block.values.get(0);
            if (strPath == null || strPath.toLowerCase().charAt(0) == 'f')
                Parameters.RESTFulServer = false;
            else
                Parameters.RESTFulServer = true;
        } catch (Exception ex) {
            log.LoggingToFile.log(Level.WARNING, "error 1101141245:: fail to get latest RESTful server setting."
                    + ex.getMessage());
        }
    }

    /**
     * extra path of latest used workspace by saved string.
     * get absolute path of latest used workspace,
     * [1]set parameter=null, if no any saved path;
     * [2]if latest used workspace found, replace all '=' back to space;
     * [3]set parameter=null if any errors occurred.
     */
    private static void getLatestUsedWorkspace(Block _block) {

        try {
            if (_block == null)
                return;

            if ((_block.values == null) || (_block.values.size() != 1))
                return;


            // check validity
            String strPath = _block.values.get(0);
            ;
            if (strPath == null)
                env.Parameters.WorkSpace.WorkSpace_AbsolutelyPath = null;


            strPath = strPath.trim();

            // check validity again
            if (strPath.trim().length() < 1)
                env.Parameters.WorkSpace.WorkSpace_AbsolutelyPath = null;

            // check the path is existing and is a folder;
            if (!verifyWorkspacePath(strPath)) {
                env.Parameters.WorkSpace.WorkSpace_AbsolutelyPath = null;
            }

            // evententlly, set the paramter
            env.Parameters.WorkSpace.WorkSpace_AbsolutelyPath = strPath;

        } catch (Exception ex) {
            log.LoggingToFile.log(Level.SEVERE, "error 1101141245:: fail to get latest used path" +
                    " of workspace!"
                    + ex.getMessage());
        }
    }

    /**
     * This method is used to verify the workspace.
     */
    private static boolean verifyWorkspacePath(String path) {
        try {

            File f = new File(path);
            if (f == null)
                return false;
            if (!f.exists())
                return false;
            if (f.isFile())
                return false;

        } catch (Exception ex) {
            log.LoggingToFile.log(Level.SEVERE, "error 1101141247:: fail to verify the latest " +
                    "used path of workspace:: "
                    + ex.getMessage());
            return false;
        }
        return true;
    }

    private void getUMLSUsername(Block block) {
        try {
            if (block == null)
                return;

            if ((block.values == null) || (block.values.size() != 1))
                return;

            // check validity
            String username = block.values.get(0);

            if ((username == null) || (username.trim().length() < 1)) {
                env.Parameters.umls_username = null;
            } else {
                env.Parameters.umls_username = username.trim();
            }


        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void getUMLSPassword(Block block) {
        try {
            if (block == null)
                return;

            if ((block.values == null) || (block.values.size() != 1))
                return;

            // check validity
            String encryptedpassword = block.values.get(0);

            if ((encryptedpassword == null) || (encryptedpassword.trim().length() < 1)
                    || (encryptedpassword.trim().compareTo("NOPASSWORD") == 0)) {
                env.Parameters.umls_decryptedPassword = null;
            } else {
                PBECoder decryptor = new PBECoder();
                decryptor.prepareForCONFIG();

                // string to bigint
                //BigInteger bigInt = new BigInteger(encryptedpassword, 16);                                                                        
                // bigint to byte
                // bigInt.toByteArray()

                // Arrays.toString(bigInt.toByteArray())                                                                                     

                //PBECoder.prepareForCONFIG();
                //byte[] decryptData = PBECoder.decrypt(
                //        bigInt.toByteArray(), 
                //        env.Parameters.umls_encryptorPassword, 
                //        env.Parameters.umls_encryptorSalt);
                env.Parameters.umls_decryptedPassword = safe.AESCoder.decrypt(encryptedpassword);//new String(decryptData);


                // System.out.println( env.Parameters.umls_decryptedPassword );
            }

        } catch (Exception ex) {
            System.out.println("error: fail to decrypt the umls password!!");
            //ex.printStackTrace();
        }
    }

}
