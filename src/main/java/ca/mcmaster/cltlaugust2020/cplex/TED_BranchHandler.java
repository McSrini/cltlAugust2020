/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.cltlaugust2020.cplex;

import static ca.mcmaster.cltlaugust2020.Constants.*;
import ca.mcmaster.cltlaugust2020.bcp.BCP_Result;
import ca.mcmaster.cltlaugust2020.drivers.TED_Driver;
import ca.mcmaster.cltlaugust2020.bcp.TED;
import ca.mcmaster.cltlaugust2020.bcp.Trigger;
import ca.mcmaster.cltlaugust2020.common.HyperCube;
import ca.mcmaster.cltlaugust2020.heuristics.MOMS;
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
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

/**
 *
 * @author tamvadss
 */
public class TED_BranchHandler extends IloCplex.BranchCallback{
    
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
                if (isMipRoot) logger.info (""+result.dominatingCount + ","+ result.fractionalCOunt);
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
        
    private HyperCube    getFilteredCube      (HyperCube cube,  Map <String, Integer>  fixedVars) {
        HyperCube result = cube;
        
        if (isConflict (cube,  fixedVars)){
            result = null;
        } else {
            HyperCube newcube = new HyperCube();
            newcube.zeroFixedVars.addAll( cube.zeroFixedVars);
            newcube.oneFixedVars.addAll (cube.oneFixedVars);
            
            for ( Map.Entry <String, Integer> entry : fixedVars.entrySet()){
                if (entry.getValue()==ZERO){
                    newcube.zeroFixedVars.remove( entry.getKey());
                }else {
                    newcube.oneFixedVars.remove( entry.getKey());
                }
            }
            
            if (newcube.getSize()!=cube.getSize()){
                result= newcube;
            }
            
        }
        
        return result;    
    }
    
        
    private boolean isConflict (HyperCube hcube, Map<String, Integer> varFixings) {
        boolean isConflict = false;
        
        for (String var : hcube.zeroFixedVars){
            if (isConflict) break;
            if (varFixings.keySet().contains(var) && varFixings.get(var)==ONE){
                isConflict=true;                 
            }
        }
        for (String var : hcube.oneFixedVars){
            if (isConflict) break;
            if (varFixings.keySet().contains(var) && varFixings.get(var)==ZERO){
                isConflict=true;                 
            }
        }
        
        return   isConflict;
    }
    
    private IntegerPair overruleCplexBranching (NodePayload nodeData) throws IloException {
        
        String branchingVarName = null;
        
        IntegerPair result = new IntegerPair ();
        
        //first get the variables whose values have been fixed
        Map <String, Integer> fixedVars = new HashMap <String, Integer> () ;
        List<String> fractionalVars = getFractionalAndFixedVars ( fixedVars) ;
        
                
        //filter cubes , starting from size 2 cubes
        TreeMap <Integer, List<HyperCube > >  filteredCubes =getFilteredCubes (nodeData.infeasibleCubeMap, fixedVars) ;
        
        Set<String > zeroDominatingTriggers =null;
        Set<String> oneDominatingTriggers =null;
               
        Set<String> fractionalVars_forBCP = getFractionalVars_IncludedIn_SizeTwoCubes (filteredCubes, fractionalVars);
        result.fractionalCOunt=fractionalVars_forBCP.size();
        
        if  (fractionalVars_forBCP.size()==ZERO){
         
            branchingVarName= (new MOMS()) .getBranchingVariableSuggestion (  filteredCubes, fractionalVars);
        }else {
            TED ted = new TED (fractionalVars_forBCP, filteredCubes  ) ;
             
            ted.run(true, nodeData.inherited_ZeroDominators, nodeData.inherited_OneDominators, true);
            
            zeroDominatingTriggers =ted.getZeroDominatingTriggers();
            oneDominatingTriggers = ted.getOneDominatingTriggers();
            
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
                //check for apex trigger present on both sides
                //
                List<Trigger> triggersWithSmallestMetric =  ted.getTriggersWithSmallestMetric_on_EitherSide( false);
                 
                                
                
                if (triggersWithSmallestMetric.size()==ONE){
                    branchingVarName = triggersWithSmallestMetric.get(ZERO).varName;
                } else {
                    //tie break;   
                    //run ted again, on the other side for these variables
                    branchingVarName =ted.runTheOtherSide_NoDeletion (triggersWithSmallestMetric) ;                       
                }
                   
            }
                    
            result.dominatingCount = ted.getNumDominatingTriggers();
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
    
    private Set<String> getFractionalVars_IncludedIn_SizeTwoCubes (
            TreeMap <Integer, List<HyperCube > >  filteredCubes,
            List<String> fractionalVars
            ) {
        
        Set<String> result = new HashSet<String>();
        
        List<HyperCube > sizeTwoCubes = filteredCubes.get(TWO);
        if (sizeTwoCubes!=null){
            for (HyperCube twoCube: sizeTwoCubes){
                //
                Map<String, Boolean> varsInThisCube = new HashMap <String ,Boolean> ();
                for (String  var : twoCube.zeroFixedVars){
                    varsInThisCube.put (var, false) ;
                }
                for (String  var : twoCube.oneFixedVars){
                    varsInThisCube.put (var, true) ;
                }
                Set<String> setOfVarsInthisCube = varsInThisCube.keySet();
                setOfVarsInthisCube.retainAll( fractionalVars);
                
                result.addAll( setOfVarsInthisCube);
                
            }
        }
        
        return result;
    }
    
    private void getArraysNeededForCplexBranching (String branchingVar,IloNumVar[][] vars ,
                                                   double[ ][] bounds ,IloCplex.BranchDirection[ ][]  dirs ){
        
        IloNumVar branchingCplexVar = TED_Driver.mapOfAllVariablesInTheModel.get(branchingVar );
        
         
        //    System.out.println("branchingCplexVar is "+ branchingCplexVar);
         
        
        //get var with given name, and create up and down branch conditions
        vars[ZERO] = new IloNumVar[ONE];
        vars[ZERO][ZERO]= branchingCplexVar;
        bounds[ZERO]=new double[ONE ];
        bounds[ZERO][ZERO]=ZERO;
        dirs[ZERO]= new IloCplex.BranchDirection[ONE];
        dirs[ZERO][ZERO]=IloCplex.BranchDirection.Down;

        vars[ONE] = new IloNumVar[ONE];
        vars[ONE][ZERO]=branchingCplexVar;
        bounds[ONE]=new double[ONE ];
        bounds[ONE][ZERO]=ONE;
        dirs[ONE]= new IloCplex.BranchDirection[ONE];
        dirs[ONE][ZERO]=IloCplex.BranchDirection.Up;
    }
    
    
    private  List<String> getFractionalAndFixedVars (  Map <String, Integer> fixedVars) throws IloException {
        
        List<String>  fractionalVars = new ArrayList<String> ();
        
        IloNumVar[] allVariables = new  IloNumVar[TED_Driver.mapOfAllVariablesInTheModel.size()] ;
        int index =ZERO;
        for  (Map.Entry <String, IloNumVar> entry : TED_Driver.mapOfAllVariablesInTheModel.entrySet()) {
            //
            allVariables[index++] = entry.getValue();
        }
        IloCplex.IntegerFeasibilityStatus [] status =   getFeasibilities(allVariables);
        
        index =-ONE;
        for (IloNumVar var: allVariables){
            index ++;
            //check if candidate is integer infeasible in the LP relax
            if (status[index].equals( IloCplex.IntegerFeasibilityStatus.Infeasible)) {
                fractionalVars.add(var.getName() );
            }else {
                //var is fixed if its upper and lower bounds are the same
                Double ub = getUB(var) ;
                Double lb = getLB(var) ;
                if (ZERO==Double.compare( ub,  lb)){
                    fixedVars.put (var.getName(), (int) Math.round(lb)) ;
                }
            }
        }
        
        return fractionalVars;
        
    }
    
    
    
    
}
