/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.cltlaugust2020.drivers;

import static ca.mcmaster.cltlaugust2020.Constants.BILLION;
import static ca.mcmaster.cltlaugust2020.Constants.ONE;
import static ca.mcmaster.cltlaugust2020.Constants.SIXTY;
import static ca.mcmaster.cltlaugust2020.Constants.THREE;
import static ca.mcmaster.cltlaugust2020.Constants.TWO;
import static ca.mcmaster.cltlaugust2020.Constants.ZERO;
import static ca.mcmaster.cltlaugust2020.Parameters.FILE_STRATEGY;
import static ca.mcmaster.cltlaugust2020.Parameters.MAX_TEST_DURATION_HOURS;
import static ca.mcmaster.cltlaugust2020.Parameters.MAX_THREADS;
import static ca.mcmaster.cltlaugust2020.Parameters.MIP_EMPHASIS_OPTIMALITY;
import static ca.mcmaster.cltlaugust2020.Parameters.USE_BARRIER_FOR_SOLVING_LP;
import static ca.mcmaster.cltlaugust2020.Parameters.*;
import ca.mcmaster.cltlaugust2020.cplex.EmptyBranchHandler;
import ilog.concert.IloException;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.BranchCallback;
import java.io.File;
import org.apache.log4j.Logger;

/**
 *
 * @author tamvadss
 */
public abstract class BaseDriver {
    
    protected static Logger logger;
    
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
            BranchCallback branch_callback, boolean useStrongForFirstHour ) throws IloException {
        
        mip.setParam( IloCplex.Param.TimeLimit, SIXTY*  SIXTY);
        //switch to the correct number of threads
        mip.setParam( IloCplex.Param.Threads, MAX_THREADS);

        mip.setParam( IloCplex.Param.MIP.Strategy.File, FILE_STRATEGY);
        
        mip.setParam( IloCplex.Param.Emphasis.MIP, MIP_EMPHASIS_OPTIMALITY);
        
        

        if (USE_BARRIER_FOR_SOLVING_LP) {
            mip.setParam( IloCplex.Param.NodeAlgorithm  ,  IloCplex.Algorithm.Barrier);
            mip.setParam( IloCplex.Param.RootAlgorithm  ,  IloCplex.Algorithm.Barrier);
        }

        mip.setParam( IloCplex.Param.MIP.Strategy.HeuristicFreq , -ONE);
        
        if (useStrongForFirstHour) mip.setParam(IloCplex.Param.MIP.Strategy.VariableSelect  , THREE );
        if (USE_FULL_STRONG) mip.setParam(IloCplex.Param.MIP.Limits.StrongCand  ,BILLION);
        //if (USE_FULL_STRONG) mip.setParam(IloCplex.Param.MIP.Limits.StrongIt, BILLION );

        mip.use (branch_callback) ;
             
       
        for (int hours = ONE; hours <= MAX_TEST_DURATION_HOURS ; hours ++){                
            mip.solve();
            print_statistics (mip, hours) ;
            
            
            if (hours == RAMP_UP_DURATION_HOURS){
                //restore default branching
                mip.setParam(IloCplex.Param.MIP.Strategy.VariableSelect  , ZERO );

                //remove TED callback if any
                mip.use (new EmptyBranchHandler() );
                //logger.info ("Special callback removed" );
            }
            
            if (isHaltFilePresent()) break;

            if (mip.getStatus().equals( IloCplex.Status.Infeasible)) break;
            if (mip.getStatus().equals( IloCplex.Status.Optimal)) break;

        }
    }
    
    
}
