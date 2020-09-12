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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
public class Testing___Driver extends BaseDriver{
    
    public  static List<LowerBoundConstraint> mipConstraintList ;
    public static  TreeMap<String, IloNumVar> mapOfAllVariablesInTheModel = new TreeMap<String, IloNumVar> ();
    
    public static TreeMap < String, Integer> frequencyOfVariables  = new TreeMap < String, Integer>();
     
    static {
        logger=Logger.getLogger(Testing___Driver.class);
        logger.setLevel(LOGGING_LEVEL);
        PatternLayout layout = new PatternLayout("%5p  %d  %F  %L  %m%n");     
        try {
            RollingFileAppender rfa =new  
                RollingFileAppender(layout,LOG_FOLDER+Testing___Driver.class.getSimpleName()+ LOG_FILE_EXTENSION);
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
            
            Validator validator = new Validator () ;
            for (Map.Entry<Integer, List<HyperCube>> entry :cubeCollector.collectedInfeasibleHypercubes.entrySet()){
                for (HyperCube cube : entry.getValue()){
                    //cube.print();
                    validator.validate(cube);
                }
            }
            
             
            
           Set<String> fractionalVars = new HashSet<String> ();
           //fractionalVars.add ("x1" );
           //fractionalVars.add ("x2" );
           //fractionalVars.add ("x3" );
           fractionalVars.add ("x4" );
           fractionalVars.add ("x5" );
           fractionalVars.add ("x6" );
           fractionalVars.add ("x7" );
           TED ted = new TED (fractionalVars,cubeCollector.collectedInfeasibleHypercubes ) ;
           ted.run(true, new HashSet<String>(), new HashSet<String>(), true);
           
           for (String var : fractionalVars){
                Trigger triggerZero = new Trigger();
                triggerZero.value=ZERO;
                triggerZero.varName=var;
                Trigger triggerOne = new Trigger();
                triggerOne.value= ONE;
                triggerOne.varName = var;
                
                TED anotherTed = null;
                List<Trigger> eqv = null;
                
                anotherTed=new TED (fractionalVars,cubeCollector.collectedInfeasibleHypercubes ) ;
                eqv =anotherTed.getEquivalentTriggers( triggerZero, fractionalVars, cubeCollector.collectedInfeasibleHypercubes);    
                
                if (eqv.size()>ZERO){
                    System.out.println(" 0 Equivalent triggers found for var "+ triggerZero.varName) ;
                }
                
                anotherTed = new TED (fractionalVars,cubeCollector.collectedInfeasibleHypercubes ) ;
                eqv =anotherTed.getEquivalentTriggers( triggerOne, fractionalVars, cubeCollector.collectedInfeasibleHypercubes);    
                     
                if (eqv.size()>ZERO){
                    System.out.println(" 1 Equivalent triggers found for var "+ triggerOne.varName) ;
                }
           }
           

            logger.info("TEST driver Completed successfully !" + mip.getStatus()) ;
                       
        } catch ( Exception ex ) {
            System.err.println(ex) ;
            exit(ONE);
        }
        
        
        
    }
    
    
    
}
