/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.cltlaugust2020.cplex;

import static ca.mcmaster.cltlaugust2020.Constants.*;  
import static ca.mcmaster.cltlaugust2020.Parameters.*;
import ca.mcmaster.cltlaugust2020.common.HyperCube;
import static ca.mcmaster.cltlaugust2020.drivers.BaseDriver.mapOfAllVariablesInTheModel;
import ca.mcmaster.cltlaugust2020.drivers.TED_Driver;
import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 *
 * @author tamvadss
 */
public abstract class Base_BracnchHandler  extends IloCplex.BranchCallback{
    
    
    protected  Set<String> getFractionalAndFixedVars (  Map <String, Integer> fixedVars) throws IloException {
        
        Set<String>  fractionalVars = new HashSet<String> ();
        
        IloNumVar[] allVariables = new  IloNumVar[mapOfAllVariablesInTheModel.size()] ;
        int index =ZERO;
        for  (Map.Entry <String, IloNumVar> entry : mapOfAllVariablesInTheModel.entrySet()) {
            //
            allVariables[index++] = entry.getValue();
        }
        IloCplex.IntegerFeasibilityStatus [] status =   getFeasibilities(allVariables);
        
        //double[] varValues = getValues (allVariables) ;
        
        index =-ONE;
        for (IloNumVar var: allVariables){
            index ++;
            //check if candidate is integer infeasible in the LP relax
            if (status[index].equals( IloCplex.IntegerFeasibilityStatus.Infeasible)) {
                fractionalVars.add(var.getName()  );
            }else {
                //var is fixed if its upper and lower bounds are the same
                Double ub = getUB(var) ;
                Double lb = getLB(var) ;
                if (ZERO==Double.compare( ub,  lb)){
                    fixedVars.put (var.getName(), (int) Math.round(lb)) ;
                }else {
                    //not fixed but integer
                    //integralVars.put (var.getName(), (int) Math.round(varValues[index])  ) ;
                }
            }
        }
        
        return fractionalVars;
        
    }
    
    protected List<String> getNewCandidates(TreeMap < String, Integer > frequencyMap ){
        List<String> newCandidates = new ArrayList<String> ();
        
        //all vars with highest freq in the map
        int highestKnownFreq = - ONE;
        for (Map.Entry < String, Integer > entry: frequencyMap.entrySet()){
            if (entry.getValue() > highestKnownFreq){
                highestKnownFreq = entry.getValue();
                newCandidates.clear();
                newCandidates.add (entry.getKey());
            }else if (entry.getValue() == highestKnownFreq) {
                newCandidates.add (entry.getKey());
            }
        }
         
        return newCandidates;
    }
    
    
    protected TreeMap < String, Integer > getVariableFrequency (List<String> candidates,List<HyperCube >  cubes ){
        TreeMap < String, Integer > result = new TreeMap < String, Integer > ();
        
        for (HyperCube cube :  cubes){
            Set< String> allvars = new HashSet <String> ();
            allvars.addAll(cube.getZeroFixedVars());
            allvars.addAll(cube.getOneFixedVars());
            //allvars.retainAll( candidates);
            for (String var: candidates){
                if (! allvars.contains(var)) continue;
                int count = ZERO ;
                if (result.containsKey(var)){
                    count = result.get(var);
                }
                result.put (var, ONE+count) ;
            }
        }
        
        return result;
    } 
    
    protected void getArraysNeededForCplexBranching (String branchingVar,IloNumVar[][] vars ,
                                                   double[ ][] bounds ,IloCplex.BranchDirection[ ][]  dirs ){
        
        IloNumVar branchingCplexVar = mapOfAllVariablesInTheModel.get(branchingVar );
        
         
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
    
    
    protected boolean isConflict (HyperCube hcube, Map<String, Integer> varFixings) {
        boolean isConflict = false;
        
        for (String var : hcube.getZeroFixedVars()){
            if (isConflict) break;
            if (varFixings.keySet().contains(var) && varFixings.get(var)==ONE){
                isConflict=true;                 
            }
        }
        for (String var : hcube.getOneFixedVars()){
            if (isConflict) break;
            if (varFixings.keySet().contains(var) && varFixings.get(var)==ZERO){
                isConflict=true;                 
            }
        }
        
        return   isConflict;
    }
    
    protected HyperCube    getFilteredCube      (HyperCube cube,  Map <String, Integer>  fixedVars) {
        HyperCube result = cube;
        
        if (isConflict (cube,  fixedVars)){
            result = null;
        } else {
            HyperCube newcube = new HyperCube(cube.getZeroFixedVars(), cube.getOneFixedVars());
            
            for ( Map.Entry <String, Integer> entry : fixedVars.entrySet()){
                if (entry.getValue()==ZERO){
                    newcube.removeZeroFixedVar ( entry.getKey());
                }else {
                    newcube.removeOneFixedVar ( entry.getKey());
                }
            }
            
            if (newcube.getSize()!=cube.getSize()){
                result= newcube;
            }
            
        }
        
        return result;    
    }
    
    
        
    
}
