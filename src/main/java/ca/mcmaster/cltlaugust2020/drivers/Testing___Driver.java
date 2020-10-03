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
import java.util.TreeSet;
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
        
        /*objectiveFunctionMap=new TreeMap<String, Double> ();
        objectiveFunctionMap.put( "x1",  19.0);
        objectiveFunctionMap.put( "x2", - 8.2);
        objectiveFunctionMap.put( "x3",  -1.7);
        MOHP_BranchHandler mohp_handler = new MOHP_BranchHandler (new TreeMap <Double, List<HyperCube > >  ());
        TreeMap <Double, List<HyperCube > >  filteredCubes = new TreeMap <Double, List<HyperCube > >  ();
        Set<String> fractional_Vars = new HashSet<> () ;
        fractional_Vars.add ("x1");
        fractional_Vars.add ("x2");
        //fractional_Vars.add ("x3");
        HyperCube h1 = new HyperCube (new HashSet <String> (), new HashSet <String> ());
        h1.addOneFixing("x1");
         h1.addOneFixing("x3");
        HyperCube h2 = new HyperCube (new HashSet <String> (), new HashSet <String> ());
        h2.addOneFixing("x3");
         h2.addOneFixing("x2");
        HyperCube h3 = new HyperCube (new HashSet <String> (), new HashSet <String> ());
        h3.addOneFixing("x1");
         h3.addZeroFixing("x2");
        List<HyperCube > list1 = new ArrayList<HyperCube> ( );
        List<HyperCube > list2 = new ArrayList<HyperCube> ( );
        List<HyperCube > list3 = new ArrayList<HyperCube> ( );
        list1.add (h1  );
        list2.add (h2 );
        list3.add (h3 );
        filteredCubes.put( h2.getPriority(), list2);
        filteredCubes.put( h1.getPriority(), list1);
        filteredCubes.put( -1.5,new ArrayList<HyperCube> ( )  );
        List<HyperCube > list4 =  filteredCubes.get (h3.getPriority()) ;
        list3.addAll(list1);
        filteredCubes.put( h3.getPriority(),list3 );
        TED_BranchHandler tbh = new TED_BranchHandler(new TreeMap <Integer, List<HyperCube > > ());
        Set<String> highPriortyCandidates = new HashSet <String> () ;
        TreeMap <Integer, List<HyperCube > >  fc = new TreeMap <Integer, List<HyperCube > >  ();
        fc.put(2, list2);
        fc.put(2, list3);
        tbh.getFractionalVars_IncludedIn_SizeTwoCubes (
               fc,fractional_Vars,  highPriortyCandidates); */
        //String str = mohp_handler. getBranchingDecision (fractional_Vars,filteredCubes );
        
           
        logger.info("Start !") ;
        HypercubeCollector cubeCollector =null;
        
        runTest_ConvertToBands ( );
        
        
        try {
            
            IloCplex mip =  new IloCplex();
            mip.importModel(PRESOLVED_MIP_FILENAME);
            
            logger.info ("preparing allVariablesInModel ... ");
            for (IloNumVar var :MIP_Reader.getVariables(mip)){
                mapOfAllVariablesInTheModel.put (var.getName(), var );
            }
            System.out.println ("DONE preparing vars. Total is  "+ mapOfAllVariablesInTheModel.size());  
            
            objectiveFunctionMap  =  MIP_Reader.getObjective(mip);
             
            if ( BaseDriver .isEvryVariableInTheObjective()){
                System.out.println("All vars in model") ;
            }else {
                System.err.println("Some vars NOT in model") ;
            }
            
            logger.info ("preparing constraints ... ");
            mipConstraintList= MIP_Reader.getConstraints(mip, frequencyOfVariables);
            
             //arrange vars in constraints by frequency
            for (LowerBoundConstraint lbc : mipConstraintList ){
                lbc.sortByFrequency( frequencyOfVariables);
            }
            
            
            HyperCube testcube = new HyperCube( new HashSet<String> (),  new HashSet<String> ()); 
            testcube.addOneFixing("x1");
            testcube.addZeroFixing("x2");
            testcube.addZeroFixing("x6");
            testcube.addZeroFixing("x7");
            System.out.println("test cube priority is " + testcube.getPriority()) ;
            testcube.removeOneFixedVar("x1");
            testcube.removeZeroFixedVar("x2");
            System.out.println("test cube priority is " + testcube.getPriority()) ;
            
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
                //eqv =anotherTed.getEquivalentTriggers( triggerZero, fractionalVars, cubeCollector.collectedInfeasibleHypercubes);    
                
                if (eqv.size()>ZERO){
                    System.out.println(" 0 Equivalent triggers found for var "+ triggerZero.varName) ;
                }
                
                anotherTed = new TED (fractionalVars,cubeCollector.collectedInfeasibleHypercubes ) ;
                //eqv =anotherTed.getEquivalentTriggers( triggerOne, fractionalVars, cubeCollector.collectedInfeasibleHypercubes);    
                     
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
    
    private static void runTest_ConvertToBands (){
        MOHP_BranchHandler mohpBH = new MOHP_BranchHandler (null);
        TreeMap <Double, List<HyperCube > > filteredCubes = new TreeMap <Double, List<HyperCube > > () ;
        
        List<HyperCube > list1 = new ArrayList <HyperCube> (  );
        filteredCubes.put (1.1, list1) ;
        List<HyperCube > list2 = new ArrayList <HyperCube> (  );
         filteredCubes.put (2.2, list1) ;
        List<HyperCube > list3 = new ArrayList <HyperCube> (  );
         filteredCubes.put (3.3, list1) ;
        List<HyperCube > list4 = new ArrayList <HyperCube> (  );
         filteredCubes.put (4.4, list1) ;
        List<HyperCube > list5 = new ArrayList <HyperCube> (  );
         filteredCubes.put (5.5, list1) ;
        List<HyperCube > list6 = new ArrayList <HyperCube> (  );
         filteredCubes.put (6.6, list1) ;
        List<HyperCube > list7 = new ArrayList <HyperCube> (  );
         filteredCubes.put (7.7, list1) ;
        List<HyperCube > list8 = new ArrayList <HyperCube> (  );
         filteredCubes.put (8.8, list1) ;
        List<HyperCube > list9 = new ArrayList <HyperCube> (  );
         filteredCubes.put (9.9, list1) ;
        List<HyperCube > list10 = new ArrayList <HyperCube> (  );
         filteredCubes.put (10.0, list1) ;
        List<HyperCube > list11 = new ArrayList <HyperCube> (  );
         filteredCubes.put (11.1, list1) ;
         List<HyperCube > listBil = new ArrayList <HyperCube> (  );
         filteredCubes.put (DOUBLE_ZERO - BILLION, listBil) ;
        
        TreeMap <Double, List<HyperCube > > reult = mohpBH.  convertToBands (  filteredCubes);
        
    }
    
}
