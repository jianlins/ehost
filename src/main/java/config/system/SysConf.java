/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package config.system;

import config.Block;

import java.nio.file.Paths;
import java.util.Vector;

/**
 * This class is used to write parameters for whole eHOST system to disk configure
 * file and load it while system started;
 *
 * @author leng
 */
public class SysConf {

    /**The name of eHOST system configure*/
    public static String SYS_CONFIGURE;

    /**Load systemm setting variables of eHOST from disk to memory.*/

    public static void loadSystemConfigure(){
        if (SYS_CONFIGURE==null)
            loadSystemConfigure(null);
        else
            loadSystemConfigure(SYS_CONFIGURE);
    }
    public static void loadSystemConfigure(String ehostConfigHome)
    {
        // split configure file into blocks
        if (ehostConfigHome!=null)
            SYS_CONFIGURE=Paths.get(ehostConfigHome,"eHOST.sys").toString();
        else
            SYS_CONFIGURE= Paths.get(System.getProperty("user.home"),".ehost","eHOST.sys").toString();

        config.Extracter extracter = new config.Extracter(SYS_CONFIGURE);
        Vector<Block> blocks = extracter.getBlocks();

        // load variable from block into memory
        ParameterGather memloader = new ParameterGather();
        memloader.load(blocks);

    }

    /**save configure to disk*/
    public static void saveSystemConfigure()
    {
        SaveConf saveconf = new SaveConf();
        saveconf.save(SYS_CONFIGURE);
    }
}
