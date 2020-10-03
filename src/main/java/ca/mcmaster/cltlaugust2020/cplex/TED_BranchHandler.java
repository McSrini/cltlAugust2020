/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.cltlaugust2020.cplex;

import static ca.mcmaster.cltlaugust2020.Constants.*;
import ca.mcmaster.cltlaugust2020.Parameters;
import ca.mcmaster.cltlaugust2020.bcp.BCP_Result;
import ca.mcmaster.cltlaugust2020.drivers.TED_Driver;
import ca.mcmaster.cltlaugust2020.bcp.TED;
import ca.mcmaster.cltlaugust2020.bcp.Trigger; 
import ca.mcmaster.cltlaugust2020.common.HyperCube;
import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import static java.lang.System.exit;
import java.util.ArrayList;
import java.util.HashMap;
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
 */
public class TED_BranchHandler extends Base_BracnchHandler{
    
    private static Logger logger=Logger.getLogger(TED_BranchHandler.class);
    static {
        logger.setLevel(LOGGING_LEVEL);
        PatternLayout layout = new PatternLayout("%5p  %d  %F  %L  %m%n");     
        try {
            RollingFileAppender appender = new  RollingFileAppender(layout,
                    LOG_FOLDER+TED_BranchHandler.class.getSimpleName()+ LOG_FILE_EXTENSION);
            appender.setMaxBackupIndex(SIXTY);
            logger.addAppender(appender);
            logger.setAdditivity(false);
        } catch (Exception ex) {
            ///
            System.err.println("Exit: unable to initialize logging"+ex);       
            exit(ONE);
        }
    } 
    
    private  TreeMap <Integer, List<HyperCube > > collectedInfeasibleHypercubes;
    
    public TED_BranchHandler (TreeMap <Integer, List<HyperCube > > collectedInfeasibleHypercubes) {
        this.collectedInfeasibleHypercubes=collectedInfeasibleHypercubes;
    }
 
        
    protected void main() throws IloException {
        if ( getNbranches()> ZERO ){  
             
            boolean isMipRoot = ( getNodeId().toString()).equals( MIP_ROOT_ID);
                       
            //get the node attachment for this node, any child nodes will accumulate the branching conditions
            
            if (isMipRoot){
                //root of mip
                
                NodePayload data = new NodePayload (  );
                data.infeasibleCubeMap=this.collectedInfeasibleHypercubes;
                setNodeData(data);                
            } 
            
            NodePayload nodeData = (NodePayload) getNodeData();
            
            if (nodeData!=null && nodeData.infeasibleCubeMap !=null && nodeData.infeasibleCubeMap.size()>ZERO) {
                IntegerPair result = overruleCplexBranching( nodeData );
                if (isMipRoot) logger.info (""+result.bcp_Variable_Count + ","+ result.apex_trigger_count);
            }else {
                //take default cplex branching
                logger.warn("taking default cplex branching at node for lack of node data"+ getNodeId()) ;
                
            }
            
            
        } //nbranches >0   
    }
    
    //cubes with conflict get dropped
    //cubes with no matches pass thru
    //cubes with a few matches get replacced by a new ,smaller cube
    private TreeMap <Integer, List<HyperCube > >   getFilteredCubes 
        (TreeMap <Integer, List<HyperCube > > infeasibleCubeMap, Map <String, Integer> fixedVars) {
            
            TreeMap <Integer, List<HyperCube > > result = new TreeMap <Integer, List<HyperCube > > ();
            
            for (Map.Entry <Integer, List<HyperCube > > entry :infeasibleCubeMap.entrySet()){                
                                
                for (HyperCube aCube : entry.getValue()){
                    HyperCube newCube =  getFilteredCube      (   aCube,     fixedVars);
                    if (null!= newCube && newCube.getSize()>=TWO) {
                        //only interested in cubes of size 2 or more
                        List<HyperCube >  current  = result.get (newCube.getSize());
                        if (null==current)  current = new ArrayList<HyperCube > ();
                        current.add (newCube);
                        result.put (newCube.getSize(), current) ;
                    }
                }
                 
            }
                        
            return result;    
    }
        
 
    
        
  
    
    private IntegerPair overruleCplexBranching (NodePayload nodeData) throws IloException {
        
        String branchingVarName = null;
        
        IntegerPair result = new IntegerPair ();
        
        //first get the variables whose values have been fixed
        Map <String, Integer> fixedVars = new HashMap <String, Integer> () ;
        //Map <String, Integer> integralVars = new HashMap <String, Integer> () ;
        Set<String> fractionalVars = getFractionalAndFixedVars ( fixedVars) ;
        
                
        //filter cubes , starting from size 2 cubes
        TreeMap <Integer, List<HyperCube > >  filteredCubes =getFilteredCubes (nodeData.infeasibleCubeMap, fixedVars) ;
        
        Set<String > zeroDominatingTriggers =new HashSet<String> ();
        Set<String> oneDominatingTriggers =new HashSet<String> ();
         
        TreeMap<Double, Set<String >> fractionalVars_InSize2Cubes  =
                get_FractionalVars_InSize2Cubes (filteredCubes.get(TWO),fractionalVars ) ;
        
        boolean only_One_priorty_Level_exists = fractionalVars_InSize2Cubes.size()==ONE && 
                                                -BILLION == fractionalVars_InSize2Cubes.firstKey();
        
        Set<String> fractionalVars_forBCP =  new HashSet<String>();
        double bandFloor =-BILLION;
        if (fractionalVars_InSize2Cubes.size()>ZERO) {
            bandFloor=getFractionalVarsInHighestBand ( fractionalVars_InSize2Cubes , fractionalVars_forBCP);
        }
        result.bcp_Variable_Count=fractionalVars_forBCP.size();
        
        if  (fractionalVars_forBCP.size()==ONE){
            
            List<String> fractionalVars_forBCP_list = new ArrayList<String> ( );
            fractionalVars_forBCP_list.addAll(fractionalVars_forBCP );
            branchingVarName = fractionalVars_forBCP_list.get(ZERO);
            
        }   else if  (fractionalVars_forBCP.size()==ZERO){
         
            branchingVarName=  getBranchingVariableSuggestion_MOMS (  filteredCubes, fractionalVars);
        }else {
            TED ted = new TED (fractionalVars_forBCP, filteredCubes  ) ;
             
            result.apex_trigger_count =  ted.run(true, nodeData.inherited_ZeroDominators, nodeData.inherited_OneDominators, true);
             
            
            List<Trigger> apexTriggers = ted.getAllApexTriggers (zeroDominatingTriggers,
                                                                oneDominatingTriggers 
                                                                ) ;
             
            if (ted.isInfeasibilityDetected){
                //
                //get the only var in the TED maps
                for (String  str : zeroDominatingTriggers){
                    branchingVarName=str;
                }
                for (String  str : oneDominatingTriggers){
                    branchingVarName =str ;
                }
            }else {
                //
                
                ted.runTheOtherSide(apexTriggers);
                branchingVarName = ted.getVariable_with_Largest_Volume_Metric(   bandFloor, ! only_One_priorty_Level_exists) ;
                      
                                              
            }
                    
            
        }
        
        //branch on this var and pass on filteredCubes  to the kids
        if (branchingVarName!=null){
            // vars needed for child node creation 
            IloNumVar[][] vars = new IloNumVar[TWO][] ;
            double[ ][] bounds = new double[TWO ][];
            IloCplex.BranchDirection[ ][]  dirs = new  IloCplex.BranchDirection[ TWO][];
            getArraysNeededForCplexBranching(branchingVarName, vars , bounds , dirs);

            //create both kids, pass on infeasible hypercubes from parent      

            double lpEstimate = getObjValue();

            NodePayload zeroChildData = new NodePayload (  );
            zeroChildData.infeasibleCubeMap=  filteredCubes;
            
            //append dominated triggers to node data, minus branching var
            appendTriggers (zeroChildData ,zeroDominatingTriggers, oneDominatingTriggers , 
                            branchingVarName, fractionalVars_forBCP, nodeData);
            
            IloCplex.NodeId zeroChildID =  makeBranch( vars[ZERO][ZERO],  bounds[ZERO][ZERO],
                                              dirs[ZERO][ZERO],  lpEstimate  , zeroChildData );

            NodePayload    oneChildData = zeroChildData; //same cubes go to both kids
            oneChildData.infeasibleCubeMap=  filteredCubes;
            IloCplex.NodeId oneChildID = makeBranch( vars[ONE][ZERO],  bounds[ONE][ZERO],
                                                 dirs[ONE][ZERO],   lpEstimate, oneChildData );
            

        }else {
            logger.warn("Took CPLEX default branch at node "+ getNodeId()+ " for lack of candidates");
        }
        
        return result;
        
    }
    
    /**
     * 
     * @param nodeData
     * @param zeroDominatingTriggers
     * @param oneDominatingTriggers
     * @param branchingVar 
     * 
     * 
     * 
     * 
     */
    private void appendTriggers (NodePayload childNodeData, Set<String > zeroDominatingTriggers ,
                                 Set<String > oneDominatingTriggers , String branchingVar,
                                 Set<String> fractionalVarsUsedForBCP, NodePayload parentNodeData){
        
         
        //any exixiting dominators that are not in the fractional vars used for BCP must be added
        //plus
        //all the dominators found at this node must be added
        //minus
        //branchingvar must be removed
        
        childNodeData.inherited_ZeroDominators.addAll(parentNodeData.inherited_ZeroDominators );
        if (null!=fractionalVarsUsedForBCP) childNodeData.inherited_ZeroDominators.removeAll( fractionalVarsUsedForBCP);
        childNodeData.inherited_OneDominators.addAll(parentNodeData.inherited_OneDominators );
        if (null!=fractionalVarsUsedForBCP) childNodeData.inherited_OneDominators.removeAll (fractionalVarsUsedForBCP) ;
                
        if (null!=zeroDominatingTriggers){
            for (String var: zeroDominatingTriggers){
                childNodeData.inherited_ZeroDominators.add (var );

            }
        }
        if (null!=oneDominatingTriggers){
            for (String var : oneDominatingTriggers){
                childNodeData.inherited_OneDominators.add (var);
            }
        }       

        childNodeData.inherited_ZeroDominators.remove( branchingVar);
        childNodeData.inherited_OneDominators.remove( branchingVar);
    }
    
    
    private String getBranchingVariableSuggestion_MOMS (TreeMap <Integer, List<HyperCube > > collectedInfeasibleHypercubes,
            Set<String> fractionalVars) {
        
        List<String> candidateVars = new ArrayList <String> ();
        candidateVars.addAll( fractionalVars);
        
        for ( Map.Entry <Integer, List<HyperCube > > entry: collectedInfeasibleHypercubes .entrySet()){
            if (candidateVars.size()==ONE ) break;
            
            TreeMap < String, Integer > frequencyMap = getVariableFrequency ( candidateVars,   entry.getValue() );
            if (frequencyMap.size()>ZERO) candidateVars = getNewCandidates(frequencyMap);
        }
        
        return candidateVars.get(ZERO) ;
    }
     
    private TreeMap<Double, Set<String >> get_FractionalVars_InSize2Cubes (List<HyperCube > sizeTwoCubes ,
            Set<String> fractionalVars) {
        
        TreeMap<Double, Set<String >> result = new TreeMap<Double, Set<String >> ();
        
        if (null!= sizeTwoCubes) {
            for (HyperCube twoCube: sizeTwoCubes){
                
                //
                Set<String > setOfVarsInthisCube = new HashSet <String > ();
                setOfVarsInthisCube.addAll (twoCube.getZeroFixedVars()) ;
                setOfVarsInthisCube.addAll (twoCube.getOneFixedVars()) ;

                //allVarsInSizeTWoCubes.addAll( setOfVarsInthisCube);

                setOfVarsInthisCube.retainAll( fractionalVars);



                if (setOfVarsInthisCube.size()>ZERO){

                    double thisCubesPriority = twoCube.getPriority();
                    Set<String > current = result.get (thisCubesPriority ); 
                    if (null == current) current = new HashSet<String> ( );
                    current.addAll (setOfVarsInthisCube) ;
                    result.put ( thisCubesPriority, current);
                }


            }
        }
        
        
        return result;
        
    }
    
    //private
    public double   getFractionalVarsInHighestBand ( TreeMap<Double, Set<String >> fractionalVars_InSize2Cubes ,
            Set<String> result){
         
        double retval=-BILLION;
        
        if (fractionalVars_InSize2Cubes.size()> TWO + TWO*Parameters.MAX_INFEASIBLE_HYPERCUBE_SIZE){
            
            Set<String > minusInfinityPrioityVars = fractionalVars_InSize2Cubes.remove(DOUBLE_ZERO-BILLION);
            
            final double MIN =  fractionalVars_InSize2Cubes.firstKey();
            final double WIDTH =( fractionalVars_InSize2Cubes.lastKey() - MIN )/(TWO + TWO*Parameters.MAX_INFEASIBLE_HYPERCUBE_SIZE);
            final double MAX = fractionalVars_InSize2Cubes.lastKey();
            
            for (Map.Entry<Double, Set<String >> entry :fractionalVars_InSize2Cubes.descendingMap().entrySet()){
                if (entry.getKey() >=MAX- WIDTH ){
                    result.addAll(entry.getValue() );
                }else {
                    retval = MAX- WIDTH;
                    break;
                }
            }
            
            if (minusInfinityPrioityVars!=null){
                //put it back
                fractionalVars_InSize2Cubes.put (DOUBLE_ZERO-BILLION, minusInfinityPrioityVars) ;
                 
            }
            
        }else {
            Map.Entry<Double, Set<String >> entry = fractionalVars_InSize2Cubes.lastEntry();
            retval =   entry.getKey();
            result.addAll( entry.getValue());
        }
        
        return retval ;
    }
    
    /*private   void   getFractionalVars_IncludedIn_SizeTwoCubes (
            List<HyperCube > sizeTwoCubes ,
            Set<String> fractionalVars, Set<String> highPriortyCandidates
            ) {
        
        
        
        double highestKnownPriority  = -BILLION;
        
        
        //Set<String> allVarsInSizeTWoCubes = new HashSet<String>();
        
        
        if (sizeTwoCubes!=null){
            for (HyperCube twoCube: sizeTwoCubes){
                
                //
                Set<String > setOfVarsInthisCube = new HashSet <String > ();
                setOfVarsInthisCube.addAll (twoCube.getZeroFixedVars()) ;
                setOfVarsInthisCube.addAll (twoCube.getOneFixedVars()) ;
                
                //allVarsInSizeTWoCubes.addAll( setOfVarsInthisCube);
                
                setOfVarsInthisCube.retainAll( fractionalVars);
                
                
                
                if (setOfVarsInthisCube.size()>ZERO){
                    
                    double thisCubesPriority = twoCube.getPriority();
                    
                    if (highestKnownPriority < thisCubesPriority){
                         highPriortyCandidates.clear();
                         highPriortyCandidates.addAll (setOfVarsInthisCube) ;
                         highestKnownPriority = thisCubesPriority;
                    } else if (highestKnownPriority ==  thisCubesPriority){
                         highPriortyCandidates.addAll (setOfVarsInthisCube) ;
                    }
                }

                                
            }
        }
        
         
    }*/
    
    
    
    /*private   Set<String>       get_Thousand_HighPriority_Vars( TreeMap<String, Double> prioritiesMap){
        Set<String> results = new HashSet<String> ();
         
        
        TreeMap <Double, List<String>> varsBypriority = new  TreeMap <Double, List<String>> ();
        for (Map.Entry<String, Double> entry :prioritiesMap.entrySet()){
            String thisVar = entry.getKey();
            Double thisPri = entry.getValue();
            List<String> varsAtThisPri = varsBypriority.get( thisPri);
            if (null==varsAtThisPri) varsAtThisPri = new ArrayList<String>();
            varsAtThisPri.add (thisVar );
            varsBypriority.put (thisPri, varsAtThisPri);
        }
        
        for  ( Map.Entry <Double, List<String>> entry : varsBypriority.descendingMap().entrySet()){
             
            if (results.size()>= HUNDRED) break;
                   
            
            results.addAll (entry.getValue()) ;
        }
        
        return results;
    }*/
    
    /*private TreeMap<String, Double> get_HighestPriority_of_FractionalVar (List<HyperCube > sizeTwoCubes,
            Set<String> fractionalVars){
        
        TreeMap<String, Double> prioritiesMap = new  TreeMap<String, Double> ();
                
        //List<HyperCube > sizeTwoCubes = filteredCubes.get(TWO);
        if (sizeTwoCubes!=null){
            for (HyperCube twoCube: sizeTwoCubes){
                
                double thisCubePriority = twoCube.getPriority();
                
                //
                Set<String > setOfVarsInthisCube = new HashSet <String > ();
                setOfVarsInthisCube.addAll (twoCube.getZeroFixedVars()) ;
                setOfVarsInthisCube.addAll (twoCube.getOneFixedVars()) ;
                
                //allVarsInSizeTWoCubes.addAll( setOfVarsInthisCube);
                
                setOfVarsInthisCube.retainAll( fractionalVars);
                
                //update freq for these vars
                for (String str : setOfVarsInthisCube){
                    Double currentPri= prioritiesMap.get( str);                    
                    if (null==currentPri || currentPri <thisCubePriority ) 
                    prioritiesMap.put( str, thisCubePriority);
                }
                                
            }
        }
        
         
        
        return prioritiesMap;
    }*/
    
}
