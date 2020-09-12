/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.cltlaugust2020.drivers;

import static ca.mcmaster.cltlaugust2020.Constants.*;
import static ca.mcmaster.cltlaugust2020.Parameters.*;
import ca.mcmaster.cltlaugust2020.bcp.*;
import ca.mcmaster.cltlaugust2020.collection.*;
import ca.mcmaster.cltlaugust2020.common.*;
import ca.mcmaster.cltlaugust2020.cplex.*; 
import ca.mcmaster.cltlaugust2020.heuristics.MOMS;
import ca.mcmaster.cltlaugust2020.utils.*;
import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.Status;
import static java.lang.System.exit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

/**
 *
 * @author tamvadss
 * 
 * Strong branching for   integer programming solvers using Boolean Constraint propagation
 * 
 */
public class TED_Driver extends BaseDriver{
    
    public  static List<LowerBoundConstraint> mipConstraintList ;
    public static  TreeMap<String, IloNumVar> mapOfAllVariablesInTheModel = new TreeMap<String, IloNumVar> ();
    
    public static TreeMap < String, Integer> frequencyOfVariables  = new TreeMap < String, Integer>();
    
    //freq credits map , 10 =1, 9 =2, 8= 4, 7 = 8, 6= 16, 5= 32, 4= 64, 3= 128, 2 = 256
    public static TreeMap < Integer, Integer> frequencyCredits  = new TreeMap < Integer, Integer>();
     
    static {
        logger=Logger.getLogger(TED_Driver.class);
        logger.setLevel(LOGGING_LEVEL);
        PatternLayout layout = new PatternLayout("%5p  %d  %F  %L  %m%n");     
        try {
            RollingFileAppender rfa =new  
                RollingFileAppender(layout,LOG_FOLDER+TED_Driver.class.getSimpleName()+ LOG_FILE_EXTENSION);
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
           
        logger.info("Start !") ;
        HypercubeCollector cubeCollector =null;
        
        init_frequencyCredits () ;
        
        try {
            
            IloCplex mip =  new IloCplex();
            mip.importModel(PRESOLVED_MIP_FILENAME);
            
            logger.info ("preparing allVariablesInModel ... ");
            for (IloNumVar var :MIP_Reader.getVariables(mip)){
                mapOfAllVariablesInTheModel.put (var.getName(), var );
            }
            System.out.println ("DONE preparing vars. Total is  "+ mapOfAllVariablesInTheModel.size());  
            
            logger.info ("preparing constraints ... ");
            mipConstraintList= MIP_Reader.getConstraints(mip, frequencyOfVariables);
            
            //arrange vars in constraints by frequency
            for (LowerBoundConstraint lbc : mipConstraintList ){
                lbc.sortByFrequency( frequencyOfVariables);
            }
            
            
            cubeCollector=new HypercubeCollector ( ) ;
            cubeCollector.collect(mipConstraintList );
            cubeCollector.printStatistics();
            
            /*Validator validator = new Validator () ;
            for (Map.Entry<Integer, List<HyperCube>> entry :cubeCollector.collectedInfeasibleHypercubes.entrySet()){
                for (HyperCube cube : entry.getValue()){
                    cube.print();
                    //validator.validate(cube);
                }
            }*/
             
            
            
            
            TED_BranchHandler tedHandler = new TED_BranchHandler (cubeCollector.collectedInfeasibleHypercubes);
           
           
            solve (mip, tedHandler, false) ;  

            
            logger.info("TED driver Completed successfully !" + mip.getStatus()) ;
                       
        } catch ( Exception ex ) {
            System.err.println(ex) ;
            exit(ONE);
        }
        
        
        
    }
    
    private static void init_frequencyCredits () {
        frequencyCredits.put (10, 1) ;
        frequencyCredits.put (9, 2) ;
        frequencyCredits.put (8, 4) ;
        frequencyCredits.put (7, 8) ;
        frequencyCredits.put (6, 16) ;
        frequencyCredits.put (5, 32) ;
        frequencyCredits.put (4, 64) ;
        frequencyCredits.put (3, 128) ;
        frequencyCredits.put (2, 256) ;
        
        //ignore larger constraints
    }
    
}
