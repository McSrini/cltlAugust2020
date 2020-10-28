/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.cltlaugust2020.drivers;

import static ca.mcmaster.cltlaugust2020.Constants.*; 
import static ca.mcmaster.cltlaugust2020.Parameters.*; 
import ca.mcmaster.cltlaugust2020.cplex.EmptyBranchHandler;
import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.BranchCallback;
import ilog.cplex.IloCplex.NodeCallback;
import java.io.File;
import java.net.InetAddress;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.apache.log4j.Logger;

/**
 *
 * @author tamvadss
 */
public abstract class BaseDriver {
    
    protected static Logger logger;
    
    public static TreeMap<String, Double> objectiveFunctionMap =null;
    public static  TreeMap<String, IloNumVar> mapOfAllVariablesInTheModel = new TreeMap<String, IloNumVar> ();
    
    
   
      //freq credits map , 10 =1, 9 =2, 8= 4, 7 = 8, 6= 16, 5= 32, 4= 64, 3= 128, 2 = 256
    public static TreeMap < Integer, Integer> frequencyCredits  = new TreeMap < Integer, Integer>();
    
    
    protected static boolean isHaltFilePresent (){
        File file = new File("haltfile.txt");         
        return file.exists();
    }

    protected static void print_statistics (IloCplex cplex, int hour) throws IloException {
        double bestSoln = BILLION;
        double relativeMipGap = BILLION;
        IloCplex.Status cplexStatus  = cplex.getStatus();
        if (cplexStatus.equals( IloCplex.Status.Feasible)  ||cplexStatus.equals( IloCplex.Status.Optimal) ) {
            bestSoln=cplex.getObjValue();
            relativeMipGap=  cplex.getMIPRelativeGap();
        };
        logger.info ("" + hour + ","+  bestSoln + ","+  
                cplex.getBestObjValue() + "," + cplex.getNnodesLeft64() +
                "," + cplex.getNnodes64() + "," + relativeMipGap ) ;
    }
    
    protected static void solve (IloCplex mip, 
            BranchCallback branch_callback /*, boolean useStrongForFirstHour */ , NodeCallback nodehandler) throws Exception {
        
        mip.setParam( IloCplex.Param.TimeLimit, SIXTY*  SIXTY);
        //switch to the correct number of threads
        mip.setParam( IloCplex.Param.Threads, MAX_THREADS);

        mip.setParam( IloCplex.Param.MIP.Strategy.File, FILE_STRATEGY);
        
        mip.setParam( IloCplex.Param.Emphasis.MIP, MIP_EMPHASIS_OPTIMALITY);
        
        final String dir =System.getProperty("user.dir");
        final String hostname  = InetAddress.getLocalHost().getHostName();
        logger.info ("Solve started "+  hostname + " " + dir + "\n MAX_TEST_DURATION_HOURS "+ MAX_TEST_DURATION_HOURS +
                 " , MAX_INFEASIBLE_HYPERCUBE_SIZE =  "+ MAX_INFEASIBLE_HYPERCUBE_SIZE +
                " , RAMP_UP_DURATION_HOURS = "+ RAMP_UP_DURATION_HOURS +
                ", PERF_VARIABILITY_RANDOM_SEED = "+ PERF_VARIABILITY_RANDOM_SEED);

        if (USE_BARRIER_FOR_SOLVING_LP) {
            mip.setParam( IloCplex.Param.NodeAlgorithm  ,  IloCplex.Algorithm.Barrier);
            mip.setParam( IloCplex.Param.RootAlgorithm  ,  IloCplex.Algorithm.Barrier);
        }

        mip.setParam( IloCplex.Param.MIP.Strategy.HeuristicFreq , -ONE);
        
        if (true /*useStrongForFirstHour*/) mip.setParam(IloCplex.Param.MIP.Strategy.VariableSelect  , THREE );
        
        if (USE_FULL_STRONG) mip.setParam(IloCplex.Param.MIP.Limits.StrongCand  ,BILLION);
        //if (USE_FULL_STRONG) mip.setParam(IloCplex.Param.MIP.Limits.StrongIt, BILLION );

        mip.use (branch_callback) ;
        if (null!=nodehandler) {
            mip.use(nodehandler);
            mip.setParam( IloCplex.Param.Threads, ONE);
        }
             
       
        for (int hours = ONE; hours <= MAX_TEST_DURATION_HOURS ; hours ++){                
            mip.solve();
            print_statistics (mip, hours) ;
            
            
            if (hours == RAMP_UP_DURATION_HOURS){
                //restore default branching
                //mip.setParam(IloCplex.Param.MIP.Strategy.VariableSelect  , ZERO );

                //remove TED callback if any
                mip.clearCallbacks();
                mip.use (new EmptyBranchHandler() );
                mip.setParam( IloCplex.Param.Threads, MAX_THREADS);
                logger.info ("Special callback removed" );
            }
            
            if (isHaltFilePresent()) break;

            if (mip.getStatus().equals( IloCplex.Status.Infeasible)) break;
            if (mip.getStatus().equals( IloCplex.Status.Optimal)) break;

        }
    }
    
    protected static boolean isEvryVariableInTheObjective (){
        Set<String> allvars = mapOfAllVariablesInTheModel.keySet();
        Set<String> objVars  = new HashSet<String> ();
        for (Map.Entry<String, Double> entry :objectiveFunctionMap.entrySet()){
            if (DOUBLE_ZERO!=entry.getValue()){
                objVars.add(entry.getKey()) ;
            }
        }
        
        return allvars.size()==objVars.size();
    }
}
