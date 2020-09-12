/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.cltlaugust2020.utils;

import static ca.mcmaster.cltlaugust2020.Constants.*;
import ca.mcmaster.cltlaugust2020.common.*;
import ca.mcmaster.cltlaugust2020.drivers.TED_Driver;
import ilog.concert.IloException;
import ilog.concert.IloLPMatrix;
import ilog.concert.IloNumVar;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

/**
 *
 * @author tamvadss
 */
public class MIP_Reader {
    
    public static List<LowerBoundConstraint> getConstraints (IloCplex cplex,  TreeMap < String, Integer> frequencyOfVariables) throws IloException{
           
        IloLPMatrix lpMatrix = (IloLPMatrix)cplex.LPMatrixIterator().next();
        
        final int numConstraints = lpMatrix.getNrows();
         
        List<LowerBoundConstraint> result = new ArrayList<LowerBoundConstraint>( );
        
        
        int[][] ind = new int[ numConstraints][];
        double[][] val = new double[ numConstraints][];
        
        double[] lb = new double[numConstraints] ;
        double[] ub = new double[numConstraints] ;
        
        lpMatrix.getRows(ZERO,   numConstraints, lb, ub, ind, val);
        
        //IloRange[] ranges = lpMatrix.getRanges() ;
        
        
        int numTwoSizeConstraints =ZERO ;
        int numThreeSizeConstraints =ZERO ;
        int numFourSizeConstraints =ZERO ;
        
        //build up each constraint 
        for (int index=ZERO; index < numConstraints ; index ++ ){
            
            //String thisConstraintname = ranges[index].getName();
            //System.out.println("Constarint is : " + thisConstraintname + " lenght is " +ind[index].length);//k
            final int numVarsInConstraint =  ind[index].length;
            if ( TWO== numVarsInConstraint){
                numTwoSizeConstraints++;
            }else if (THREE== numVarsInConstraint) {
                numThreeSizeConstraints ++ ;
            }else if (FOUR== numVarsInConstraint){
                numFourSizeConstraints++;
            }
            //if (ind[index].length > MAX_VARIABLES_PER_CONSTRAINT) continue;
                       
            boolean isUpperBound = Math.abs(ub[index])< BILLION ;
            boolean isLowerBound = Math.abs(lb[index])<BILLION ;
            boolean isEquality = ub[index]==lb[index];
            
            if  (isEquality)  {
                LowerBoundConstraint lbcUP =new LowerBoundConstraint();
                LowerBoundConstraint lbcDOWN =new LowerBoundConstraint( );
                 
                lbcUP .lowerBound= lb[index];
                lbcDOWN.lowerBound=-ub[index]; //ub portion
                
                for (  int varIndex = ZERO;varIndex< ind[index].length;   varIndex ++ ){
                    String var = lpMatrix.getNumVar(ind[index][varIndex]).getName() ;
                    Double coeff = val[index][varIndex];
                    lbcUP.add(var,  coeff) ;
                    lbcDOWN.add(var, -coeff);
                    
                    //this vars freq up by 2
                    int thisVarsFreq = frequencyOfVariables.containsKey (var) ? frequencyOfVariables.get (var):ZERO;
                    int credits = ZERO;
                    if (TED_Driver.frequencyCredits.containsKey(numVarsInConstraint )){
                        credits= TWO*TED_Driver.frequencyCredits.get(numVarsInConstraint );
                    }
                    frequencyOfVariables.put (var, thisVarsFreq+credits) ;
                    
                }
                
                result.add(lbcUP);
                result.add(lbcDOWN);
                
            }else {
                
                //not an equailty constraint
                LowerBoundConstraint lbc =new LowerBoundConstraint();
                lbc.lowerBound=  (isUpperBound && ! isLowerBound )? -ub[index] : lb[index];
                for (  int varIndex = ZERO;varIndex< ind[index].length;   varIndex ++ ){
                    String var = lpMatrix.getNumVar(ind[index][varIndex]).getName() ;
                    Double coeff = val[index][varIndex];
                    lbc.add(var, (isUpperBound && ! isLowerBound )? -coeff: coeff) ;
                    
                    int thisVarsFreq = frequencyOfVariables.containsKey (var) ? frequencyOfVariables.get (var):ZERO;
                    int credits = ZERO;
                    if (TED_Driver.frequencyCredits.containsKey(numVarsInConstraint )){
                        credits= TED_Driver.frequencyCredits.get(numVarsInConstraint );
                    }
                    frequencyOfVariables.put (var, thisVarsFreq+  credits) ;
                    
                }
                result.add(lbc) ;
            }
            
        }
 
        System.out.println("constraint size statistics "+ numTwoSizeConstraints + "," +
                            numThreeSizeConstraints + "," + numFourSizeConstraints);
                 
        return result;
     
          
    }
    
        
    public static List<IloNumVar> getVariables (IloCplex cplex) throws IloException{
        List<IloNumVar> result = new ArrayList<IloNumVar>();
        IloLPMatrix lpMatrix = (IloLPMatrix)cplex.LPMatrixIterator().next();
        IloNumVar[] variables  =lpMatrix.getNumVars();
        for (IloNumVar var :variables){
            result.add(var ) ;
        }
        return result;
    }
    
}
