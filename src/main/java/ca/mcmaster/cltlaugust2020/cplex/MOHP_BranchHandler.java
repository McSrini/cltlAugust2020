/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.cltlaugust2020.cplex;

import static ca.mcmaster.cltlaugust2020.Constants.*; 
import ca.mcmaster.cltlaugust2020.Parameters;
import static ca.mcmaster.cltlaugust2020.Parameters.PERF_VARIABILITY_RANDOM_GENERATOR;
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
public class MOHP_BranchHandler extends Base_BracnchHandler{
    
    private static Logger logger=Logger.getLogger(MOHP_BranchHandler.class);
    static {
        logger.setLevel(LOGGING_LEVEL);
        PatternLayout layout = new PatternLayout("%5p  %d  %F  %L  %m%n");     
        try {
            RollingFileAppender appender = new  RollingFileAppender(layout,
                    LOG_FOLDER+MOHP_BranchHandler.class.getSimpleName()+ LOG_FILE_EXTENSION);
            appender.setMaxBackupIndex(SIXTY);
            logger.addAppender(appender);
            logger.setAdditivity(false);
        } catch (Exception ex) {
            ///
            System.err.println("Exit: unable to initialize logging"+ex);       
            exit(ONE);
        }
    } 
    
    private  TreeMap <Double, List<HyperCube > > infeasibleHypercubes_byPriority;
    
    public MOHP_BranchHandler (TreeMap <Double, List<HyperCube > > infeasibleHypercubes_byPriority) {
        this.infeasibleHypercubes_byPriority=infeasibleHypercubes_byPriority;
    }

    //@Override
    protected void main() throws IloException {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        //
        
        if ( getNbranches()> ZERO ){  
           
                       
            boolean isMipRoot = ( getNodeId().toString()).equals( MIP_ROOT_ID);
                       
            //get the node attachment for this node, any child nodes will accumulate the branching conditions
            
            if (isMipRoot){
                //root of mip
                
                MOHP_Payload data = new MOHP_Payload (  );
                data.infeasibleHypercubes_byPriority=this.infeasibleHypercubes_byPriority;
                setNodeData(data);                
            } 
            
            MOHP_Payload nodeData = (MOHP_Payload) getNodeData();
            
            if (nodeData!=null && nodeData.infeasibleHypercubes_byPriority !=null && nodeData.infeasibleHypercubes_byPriority.size()>ZERO) {
                overruleCplexBranching( nodeData );
                 
            }else {
                //take default cplex branching
                logger.warn("taking default cplex branching at node for lack of node data"+ getNodeId()) ;
                
            }
            
        }
        
    }//end main
    
    private void overruleCplexBranching (MOHP_Payload nodeData) throws IloException {
         
        String branchingVarName = null;
        
         
        //first get the variables whose values have been fixed
        Map <String, Integer> fixedVars = new HashMap <String, Integer> () ;
        //Map <String, Integer> integralVars = new HashMap <String, Integer> () ;
        Set<String> fractionalVars = getFractionalAndFixedVars ( fixedVars) ;
        
                
        //filter cubes , starting from size 2 cubes
        TreeMap <Double, List<HyperCube > >  filteredCubes =getFilteredCubes (nodeData.infeasibleHypercubes_byPriority, fixedVars) ;
        
        TreeMap <Double, List<HyperCube > >  filteredCubes_topTen = convertToBands (filteredCubes) ;
        
        branchingVarName = getBranchingDecision (fractionalVars,  filteredCubes_topTen) ;
        
        
        //branch on this var and pass on filteredCubes  to the kids
        if (branchingVarName!=null){
            // vars needed for child node creation 
            IloNumVar[][] vars = new IloNumVar[TWO][] ;
            double[ ][] bounds = new double[TWO ][];
            IloCplex.BranchDirection[ ][]  dirs = new  IloCplex.BranchDirection[ TWO][];
            getArraysNeededForCplexBranching(branchingVarName, vars , bounds , dirs);

            //create both kids, pass on infeasible hypercubes from parent      

            double lpEstimate = getObjValue();

            MOHP_Payload zeroChildData = new MOHP_Payload (  );
            zeroChildData.infeasibleHypercubes_byPriority=  filteredCubes;
            
            
            
            IloCplex.NodeId zeroChildID =  makeBranch( vars[ZERO][ZERO],  bounds[ZERO][ZERO],
                                              dirs[ZERO][ZERO],  lpEstimate  , zeroChildData );

            MOHP_Payload    oneChildData = zeroChildData; //same cubes go to both kids
            oneChildData.infeasibleHypercubes_byPriority=  filteredCubes;
            IloCplex.NodeId oneChildID = makeBranch( vars[ONE][ZERO],  bounds[ONE][ZERO],
                                                 dirs[ONE][ZERO],   lpEstimate, oneChildData );
            

        }else {
            logger.warn("Took CPLEX default branch at node "+ getNodeId()+ " for lack of candidates");
        }
        
    }
    
    private     String getBranchingDecision (Set<String> fractionalVars, TreeMap <Double, List<HyperCube > >  cubesToConsider) {
        
        List<String> candidates = new ArrayList<String> ();
        candidates.addAll(        fractionalVars);
        
        for ( Map.Entry <Double, List<HyperCube > > entry: cubesToConsider.descendingMap().entrySet()){
            if (candidates.size()==ONE ) break;
            
            TreeMap < String, Integer > frequencyMap = getVariableFrequency ( candidates,   entry.getValue() );
            if (frequencyMap.size()>ZERO) candidates = getNewCandidates(frequencyMap);
        }
        
        int randomPosition = PERF_VARIABILITY_RANDOM_GENERATOR.nextInt(candidates.size() );
        return candidates.get(randomPosition);
    }
    
 
    
    
    private TreeMap <Double, List<HyperCube > > getFilteredCubes (
        TreeMap <Double, List<HyperCube > > infeasibleHypercubes_byPriority,
        Map <String, Integer> fixedVars    ) {
        
        TreeMap <Double, List<HyperCube > > result = new TreeMap <Double, List<HyperCube > > ();
        
        for (Map.Entry <Double, List<HyperCube > > entry :infeasibleHypercubes_byPriority.entrySet()){                
                                
            for (HyperCube aCube : entry.getValue()){
                HyperCube newCube =  getFilteredCube      (   aCube,     fixedVars);
                if (null!= newCube && newCube.getSize()>=TWO) {
                    //only interested in cubes of size 2 or more
                    List<HyperCube >  current  = result.get (newCube.getPriority());
                    if (null==current)  current = new ArrayList<HyperCube > ();
                    current.add (newCube);
                    result.put (newCube.getPriority(), current) ;
                }
            }

        }
        
        return result;
    }
        
    //private 
    public TreeMap <Double, List<HyperCube > >   convertToBands (TreeMap <Double, List<HyperCube > > filteredCubes) {
        TreeMap <Double, List<HyperCube > >  result = new TreeMap <Double, List<HyperCube > >  ();
        
        if (filteredCubes.size()> TWO + TWO*Parameters.MAX_INFEASIBLE_HYPERCUBE_SIZE){
            
            List<HyperCube > minusInfinityPrioityCubes = filteredCubes.remove(DOUBLE_ZERO-BILLION);
            
            final double MIN =  filteredCubes.firstKey();
            final double WIDTH =( filteredCubes.lastKey() - MIN )/(TWO + TWO*Parameters.MAX_INFEASIBLE_HYPERCUBE_SIZE);
            
            int index = ONE;
            List<HyperCube > accumulated = new ArrayList<HyperCube> ();
            for (Map.Entry <Double, List<HyperCube > > entry: filteredCubes.entrySet()){
                double thisPri = entry.getKey();
                if (thisPri <= MIN + index*WIDTH){
                    accumulated.addAll(entry.getValue() );
                }else {                    
                    result.put (DOUBLE_ZERO+index , accumulated);
                    index ++;
                    accumulated = new ArrayList<HyperCube> ();
                    accumulated.addAll(entry.getValue() );
                }
            }
            
            if (!accumulated.isEmpty()){
                //will never be empty
                result.put (DOUBLE_ZERO+index , accumulated);
            }
            
            if (minusInfinityPrioityCubes!=null){
                //put it back
                filteredCubes.put (DOUBLE_ZERO-BILLION, minusInfinityPrioityCubes) ;
                //also add to result
                result.put (DOUBLE_ZERO-BILLION, minusInfinityPrioityCubes) ;
            }
            
        }else {
            result = filteredCubes;
        }
        
       
        return result;
    }
    
}//end class
