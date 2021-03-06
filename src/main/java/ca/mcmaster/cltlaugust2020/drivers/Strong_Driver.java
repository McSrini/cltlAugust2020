/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.cltlaugust2020.drivers;

import static ca.mcmaster.cltlaugust2020.Constants.LOGGING_LEVEL;
import static ca.mcmaster.cltlaugust2020.Constants.LOG_FILE_EXTENSION;
import static ca.mcmaster.cltlaugust2020.Constants.LOG_FOLDER;
import static ca.mcmaster.cltlaugust2020.Constants.*;
import static ca.mcmaster.cltlaugust2020.Constants.SIXTY;
import ca.mcmaster.cltlaugust2020.Parameters;
import static ca.mcmaster.cltlaugust2020.Parameters.PRESOLVED_MIP_FILENAME;
import static ca.mcmaster.cltlaugust2020.Parameters.*;
import ca.mcmaster.cltlaugust2020.cplex.*; 
import ilog.concert.IloException;
import ilog.cplex.IloCplex;
import static java.lang.System.exit;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

/**
 *
 * @author tamvadss
 */
public class Strong_Driver extends BaseDriver {
    
     
    static {
        logger=Logger.getLogger(Strong_Driver.class);
        logger.setLevel(LOGGING_LEVEL);
        PatternLayout layout = new PatternLayout("%5p  %d  %F  %L  %m%n");     
        try {
            RollingFileAppender rfa =new  
                RollingFileAppender(layout,LOG_FOLDER+Strong_Driver.class.getSimpleName()+ LOG_FILE_EXTENSION);
            rfa.setMaxBackupIndex(SIXTY);
            logger.addAppender(rfa);
            logger.setAdditivity(false);            
             
        } catch (Exception ex) {
            ///
            System.err.println("Exit: unable to initialize logging"+ex);       
            exit(ONE);
        }
    }
    
    public static void main(String[] args) throws Exception {
        
        logger.info("Start ! " + Parameters.USE_FULL_STRONG) ;
          
        IloCplex mip =  new IloCplex();
            
        mip.importModel(PRESOLVED_MIP_FILENAME);
        
        
        //mip.setParam(IloCplex.Param.RandomSeed,  13);
        
        
        
           
        solve (mip, new EmptyBranchHandler(), null) ; 
        
       
        
        logger.info("Strong driver Completed successfully !" + mip.getStatus()) ;
        
    }
    
  
    
}
