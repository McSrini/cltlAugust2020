/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.cltlaugust2020.common;

import static ca.mcmaster.cltlaugust2020.Constants.*;
import static ca.mcmaster.cltlaugust2020.Parameters.MAX_INFEASIBLE_HYPERCUBE_SIZE;
import ca.mcmaster.cltlaugust2020.drivers.BaseDriver;
import static ca.mcmaster.cltlaugust2020.drivers.BaseDriver.objectiveFunctionMap;
import ca.mcmaster.cltlaugust2020.drivers.TED_Driver;
import static java.util.Collections.unmodifiableSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 *
 * @author tamvadss
 */
public class HyperCube {
    
    private Set<String> zeroFixedVars = new HashSet<String> () ;
    private Set<String> oneFixedVars = new HashSet<String> () ;
    
    private TreeSet<String> varsInThisCube_that_Are_included_in_objective = new TreeSet<String> () ;
    
    private double priority = DOUBLE_ZERO;
    
    
    /*public HyperCube ( ) {
        
    }*/
    
    public HyperCube (Set<String> zeroFixedVars,  Set<String> oneFixedVars ) {
        
        this.addZeroFixings( zeroFixedVars);
        this.addOneFixings(oneFixedVars );
        
         
    }
    
    public int getVolume () {
        int numVars =  this.zeroFixedVars.size() + this.oneFixedVars.size();
        int result = ONE;
        while (numVars < MAX_INFEASIBLE_HYPERCUBE_SIZE) {
            result*= TWO;
            numVars++;
        }
            
        return result;
    }
    
    public Set<String> getZeroFixedVars (){
        //not a good idea to return a private collection
        return   unmodifiableSet (this.zeroFixedVars);
    }
    public Set<String> getOneFixedVars (){
        return unmodifiableSet (this.oneFixedVars);
    }
    
    public void addZeroFixings (Set<String> varList) {
        for (String str : varList){
            addZeroFixing (str );
        }
    }
    
    public void addOneFixings (Set<String> varList) {
        for (String str: varList){
            addOneFixing (str);
        }
    }
    
    public void addZeroFixing (String var) {
        this.zeroFixedVars.add(var);
        double objectiveCoeff = objectiveFunctionMap.get(var );
        //if (objectiveCoeff < ZERO){
            priority += objectiveCoeff;
        //}
        if (DOUBLE_ZERO != objectiveCoeff){
            this.varsInThisCube_that_Are_included_in_objective.add (var);
        }
    }
    
    public void addOneFixing (String var) {
        this.oneFixedVars.add(var);
        double objectiveCoeff = objectiveFunctionMap.get(var );
        //if (objectiveCoeff > ZERO){
            priority -= objectiveCoeff;
        //}
        if (DOUBLE_ZERO != objectiveCoeff){
            this.varsInThisCube_that_Are_included_in_objective.add (var);
        }
    }
    
    public void removeZeroFixedVar (String var) {
        if (zeroFixedVars.remove( var)) {
            //adjust priority
            
            double objectiveCoeff = objectiveFunctionMap.get(var );
            //if (objectiveCoeff < ZERO){
                priority -= objectiveCoeff;
            //}

            this.varsInThisCube_that_Are_included_in_objective.remove(var);
            
        }        
    }
    
    public void removeOneFixedVar (String var) {
        if (this.oneFixedVars.remove( var)) {
            //adjust priority
            
            double objectiveCoeff = objectiveFunctionMap.get(var );
            //if (objectiveCoeff >ZERO){
                priority += objectiveCoeff;
            //}
            this.varsInThisCube_that_Are_included_in_objective.remove(var);
        }        
    }
    
    public double getPriority () {
        
        return varsInThisCube_that_Are_included_in_objective.size() == ZERO ? -BILLION: this.priority;
    }
    
    public int getSize () {
        return oneFixedVars.size()+ zeroFixedVars.size();
    } 
    
    public void print () {
        System.out.println("Zero fixed vars") ;
        for (String str : zeroFixedVars){
            System.out.println(str) ;
        }
        System.out.println("One fixed vars") ;
        for (String str : oneFixedVars){
            System.out.println(str) ;
        }
        System.out.println();
    } 
}
