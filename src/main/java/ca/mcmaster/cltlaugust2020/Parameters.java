/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.cltlaugust2020;

import static ca.mcmaster.cltlaugust2020.Constants.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author tamvadss
 */
public class Parameters {
    
    //public static final String PRESOLVED_MIP_FILENAME =
    //        "F:\\Srini Data\\papers\\my projects\\sparcplex\\Collecting rectangles paper submission\\August 2020 Optimization engg review 1\\ltl Mips\\ltl_March_17.pre" ;
    //public static final String PRESOLVED_MIP_FILENAME = "F:\\temporary files here recovered\\ltl8.pre.sav";
    //public static final String PRESOLVED_MIP_FILENAME = "F:\\temporary files here recovered\\knapsacksmall.lp";
    //public static final String PRESOLVED_MIP_FILENAME = "F:\\temporary files here recovered\\knapsackthreetest.lp";
    //public static final String PRESOLVED_MIP_FILENAME = "F:\\temporary files here\\knapsack_single.lp";
    //public static final String PRESOLVED_MIP_FILENAME = "F:\\temporary files here recovered\\seymour-disj-10.pre";
    //public static final String PRESOLVED_MIP_FILENAME = "F:\\temporary files here recovered\\rail03read .pre";
    //public static final String PRESOLVED_MIP_FILENAME = "F:\\temporary files here\\knapsackonetest.lp";
    //public static final String PRESOLVED_MIP_FILENAME = "F:\\temporary files here recovered\\opm2-z12-s14.pre";
    //public static final String PRESOLVED_MIP_FILENAME = "F:\\temporary files here recovered\\opm2-z10-s4.pre";
    //public static final String PRESOLVED_MIP_FILENAME = "F:\\temporary files here recovered\\opm2-z12-s7.pre";
    //public static final String PRESOLVED_MIP_FILENAME = "F:\\temporary files here recovered\\neos-beardy.pre";
    //public static final String PRESOLVED_MIP_FILENAME = "neos-807456.pre";
    public static final String PRESOLVED_MIP_FILENAME = "ltl_March_11.pre";
    //public static final String PRESOLVED_MIP_FILENAME = "bnatt500.pre";
    //public static final String PRESOLVED_MIP_FILENAME = "neosbeardy.pre";
    //public static final String PRESOLVED_MIP_FILENAME = "seymour-disj-10.pre";
    //public static final String PRESOLVED_MIP_FILENAME = "seymour-disj-10.pre";
    //public static final String PRESOLVED_MIP_FILENAME = "opm2-z12-s14.pre";
    //public static final String PRESOLVED_MIP_FILENAME = "opm2-z12-s8.pre";
    //public static final String PRESOLVED_MIP_FILENAME ="opm2-z12-s14.pre";
    //public static final String PRESOLVED_MIP_FILENAME ="opm2-z12-s7.pre";
    //public static final String PRESOLVED_MIP_FILENAME = "hanoi5.pre";
    //public static final String PRESOLVED_MIP_FILENAME ="opm2-z10-s4.pre";
    //public static final String PRESOLVED_MIP_FILENAME ="2club200v.pre";
    //public static final String PRESOLVED_MIP_FILENAME ="neos-807456.pre";
    
    public static List<String> mipsWithBarrier = new ArrayList<String> (
        Arrays.asList( "seymour-disj-10.pre", "supportcase3.pre"));
    public static boolean USE_BARRIER_FOR_SOLVING_LP = mipsWithBarrier.contains(PRESOLVED_MIP_FILENAME);
    
    
    public static final int RAMP_UP_DURATION_HOURS= 3; 
       
    
    public static final int MAX_THREADS = 32;
    public static final int MAX_INFEASIBLE_HYPERCUBE_SIZE = 4; 
    public static final int MAX_TEST_DURATION_HOURS = 1000;
    public static final int  FILE_STRATEGY= 3;
    public static boolean USE_FULL_STRONG = false;
    public static final int  MIP_EMPHASIS_OPTIMALITY= 2;
    
    
    //public static final long PERF_VARIABILITY_RANDOM_SEED = 0;
    //public static final java.util.Random  PERF_VARIABILITY_RANDOM_GENERATOR =             new  java.util.Random  (PERF_VARIABILITY_RANDOM_SEED);
   
     
}
