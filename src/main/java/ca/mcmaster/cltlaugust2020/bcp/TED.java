/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.cltlaugust2020.bcp;

import static ca.mcmaster.cltlaugust2020.Constants.*;
import ca.mcmaster.cltlaugust2020.common.HyperCube; 
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 *
 * @author tamvadss
 */
public class TED {
    
    private TreeMap <String, BCP_Result> zeroFixingResults = new TreeMap<String, BCP_Result>();
    private TreeMap <String, BCP_Result> oneFixingResults = new TreeMap<String, BCP_Result>();
    
    private TreeMap <Integer, List<HyperCube > > infeasibleHypercubes;
    
    public boolean isInfeasibilityDetected = false;
    
    public TED (Set<String> fractionalVars  , TreeMap <Integer, List<HyperCube > > infeasibleHypercubes) {
        
        //
        for (String var : fractionalVars) {
            zeroFixingResults.put (var, null);
            oneFixingResults.put (var, null);
        }
        this.infeasibleHypercubes = infeasibleHypercubes;
                        
    }
    
    public List<Trigger> getEquivalentTriggers (Trigger trigger, Set<String> fractionalVars  , 
                                                TreeMap <Integer, List<HyperCube > > infeasibleHypercubes){
        List<Trigger> result = new ArrayList<Trigger> ();
        
        TED ted = new TED (  fractionalVars  ,   infeasibleHypercubes) ;
        BCP_Result bcpResult = ted.run(trigger, false, false);
        
        for (Map.Entry <String, Boolean> entry : bcpResult.varFixingsFound.entrySet()){
            
            //recall that fixings include self, which should be skipped here
            if (entry.getKey().equals(trigger.varName )) {
                continue;
            }
            
            Trigger dominatedTrigger = new Trigger () ;
            dominatedTrigger.varName = entry.getKey();
            dominatedTrigger.value = entry.getValue()? ONE :ZERO;
            
            TED newTed =  new TED (  fractionalVars  ,   infeasibleHypercubes) ;
            BCP_Result newResult = newTed.run( dominatedTrigger, false, false);
            
            //if new result contains original fixing, then equivalent, else not
            boolean isEquivalent = false;
            for (Map.Entry <String, Boolean> newEntry: newResult.varFixingsFound.entrySet()){
                if (newEntry.getKey().equals( trigger.varName)){
                    int newValue = newEntry.getValue()? ONE :ZERO;
                    if (newValue==trigger.value){
                        isEquivalent= true;
                        break;
                    }
                }
            }
            
            if (isEquivalent) result.add (dominatedTrigger) ;
            
        }
        
        return result;
    }
    
    //get triggers with smallest metric
    public List<Trigger> getTriggersWithSmallestMetric_on_EitherSide ( boolean mustBeOnBothSides ){   
        double smallestKnown = BILLION;
        List<Trigger> result = new ArrayList<Trigger>();
        
        //System.out.println("getVariableWithSmallestMetric") ;
        
        for (Map.Entry <String, BCP_Result> entry : zeroFixingResults.entrySet()){
            BCP_Result thisResult = entry.getValue();
            double thismetric = DOUBLE_ZERO;
            
            if (mustBeOnBothSides && ! oneFixingResults.containsKey( entry.getKey())){
                continue ;
            }
            
            for (Map.Entry<Integer, List<HyperCube>> remainingCubesEntry : thisResult.remainingInfeasibleCubes.entrySet()){
                double divisor = getTwoPower(remainingCubesEntry.getKey()) ;
                thismetric +=  ( DOUBLE_ZERO + remainingCubesEntry.getValue().size() )/ divisor;
            }
                     
            //System.out.println(thismetric) ;
            
            if (thismetric< smallestKnown) {
                smallestKnown = thismetric;
                result.clear();
                Trigger trig =  new Trigger ();
                trig.value = ZERO;
                trig.varName=  entry.getKey();
                result.add(trig);
            }else if (thismetric == smallestKnown) {
                Trigger trig =  new Trigger ();
                trig.value = ZERO;
                trig.varName=  entry.getKey();
                result.add(trig);
            }
        }
        
        for (Map.Entry <String, BCP_Result> entry : oneFixingResults.entrySet()){
            
            BCP_Result thisResult = entry.getValue();
            
            double thismetric = DOUBLE_ZERO;
            
            if (mustBeOnBothSides && ! zeroFixingResults.containsKey( entry.getKey())){
                continue ;
            }
            
            for (Map.Entry<Integer, List<HyperCube>> remainingCubesEntry : thisResult.remainingInfeasibleCubes.entrySet()){
                double divisor = getTwoPower(remainingCubesEntry.getKey()) ;
                thismetric +=  ( DOUBLE_ZERO + remainingCubesEntry.getValue().size() )/ divisor;
            }
                     
             //System.out.println(thismetric) ;
            
            if (thismetric< smallestKnown) {
                smallestKnown = thismetric;
                result.clear();
                Trigger trig =  new Trigger ();
                trig.value = ONE;
                trig.varName=  entry.getKey();
                result.add(trig);
            }else if (thismetric == smallestKnown) {
                Trigger trig =  new Trigger ();
                trig.value = ONE;
                trig.varName=  entry.getKey();
                result.add(trig);
            }
        }
        
        return result;
        
    }
    
    public int getNumDominatingTriggers () {
        return zeroFixingResults.size() + oneFixingResults.size();
    }
    
    public Set<String> getZeroDominatingTriggers (){
        return zeroFixingResults.keySet();
    }
    
    public Set<String> getOneDominatingTriggers (){
        return this.oneFixingResults.keySet();
    }
    
    //run ted with complimentary triggers
    //return var with smallest metric
    public String runTheOtherSide_NoDeletion ( List<Trigger> triggersWithSmallestMetric) {
        
        String result = null;
        double smallestKnownMetric = BILLION;
        
        for (Trigger trigger: triggersWithSmallestMetric){
            Trigger complimentaryTrigger = new Trigger () ;
            complimentaryTrigger.value = trigger.value==ONE? ZERO: ONE;
            complimentaryTrigger.varName= trigger.varName;
            BCP_Result thisResult  =null;
                    
            //check if bcp result already available, it will be if the var has a dominating trigger on both sides
            if (complimentaryTrigger.value==ZERO ) {                
                thisResult= this.zeroFixingResults.get(complimentaryTrigger.varName );
            } else {
                thisResult= this.oneFixingResults.get(complimentaryTrigger.varName );
            }
            if (null==thisResult) {
                //does not already exist, so run it again
                thisResult=run (complimentaryTrigger,false, false) ;
            }
            
                    
            
            double thismetric=DOUBLE_ZERO;
            
            for (Map.Entry<Integer, List<HyperCube>> remainingCubesEntry : thisResult.remainingInfeasibleCubes.entrySet()){
                double divisor = getTwoPower(remainingCubesEntry.getKey()) ;
                thismetric +=  ( DOUBLE_ZERO + remainingCubesEntry.getValue().size() )/ divisor;
            }
            
            if (thismetric < smallestKnownMetric){
                smallestKnownMetric = thismetric;
                result = complimentaryTrigger.varName;
            }
           
        }
         
        return result;
    }
 
    public void run ( boolean with_Deletion_Of_DominatedTriggers, Set<String> known_ZeroDominators, 
                      Set<String> known_OneDominators, boolean withInsertionOfResults) {
        
        //first run with known dominators
        for (String str: known_ZeroDominators){
            //must be frcational, or notdominated now by some other trigger
            if (  this.zeroFixingResults.containsKey( str)){
                
                Trigger trigger = new Trigger();
                trigger.value= ZERO;
                trigger.varName= str;
                run (trigger,   with_Deletion_Of_DominatedTriggers,   withInsertionOfResults ) ;
            }
        }
        
        for (String str: known_OneDominators){
            if (  this.oneFixingResults.containsKey( str)){
                Trigger trigger = new Trigger();
                trigger.value= ONE;
                trigger.varName= str;
                run (trigger,   with_Deletion_Of_DominatedTriggers,   withInsertionOfResults ) ;
            }
        }
        
        //now run BCP with any new fractional vars
        Trigger trigger = getNextTrigger();
        while (null!=trigger){
            run (trigger,   with_Deletion_Of_DominatedTriggers,   withInsertionOfResults ) ;
            trigger = getNextTrigger();
        }
    }
    
    private BCP_Result run (Trigger trigger, boolean with_Deletion_Of_DominatedTriggers, boolean withInsertionOfResults) {
        
        BCP_Result bcp_Result =    (new BCP_Runner()) .performBCP(infeasibleHypercubes,  trigger.varName, trigger.value) ;

        if (bcp_Result.isInfeasibilityDetected){
            zeroFixingResults.clear();
            oneFixingResults.clear();  
            isInfeasibilityDetected =true;
        } 

        if (withInsertionOfResults){
            if (trigger.value==ZERO){
                zeroFixingResults.put (trigger.varName, bcp_Result) ;
            }else {
                oneFixingResults.put (trigger.varName, bcp_Result) ;
            }
        }


        if (! bcp_Result.isInfeasibilityDetected) {
            if (with_Deletion_Of_DominatedTriggers){

                //delete map entries for dominated triggers    
                //note    varFixingsFound includes this trigger too     
                for ( Map.Entry<String, Boolean>  entry : bcp_Result.varFixingsFound.entrySet()){
                    if (entry.getValue()){
                        //1 fixing
                        oneFixingResults.remove(entry.getKey() );
                    }else {
                        //0 fixing
                        zeroFixingResults.remove(entry.getKey() );
                    }
                }

                //re-insert reults for this trigger
                if (withInsertionOfResults){
                    if (trigger.value==ZERO){
                        zeroFixingResults.put (trigger.varName, bcp_Result) ;
                    }else {
                        oneFixingResults.put (trigger.varName, bcp_Result) ;
                    }
                }

            }
        }
        
        return bcp_Result;
    }
    
    private Trigger getNextTrigger () {
        Trigger result = new Trigger ();
        result.varName=getNextZeroTrigger();
        result .value= ZERO;
        if (result.varName==null) {
            result.varName = getNextOneTrigger();
            result.value=ONE;
        }
        return result.varName==null? null : result;
    }
        
    private String getNextZeroTrigger () {
        String result = null;
        for (Map.Entry <String, BCP_Result> entry : zeroFixingResults.entrySet()){
            if (entry.getValue()==null){
                result = entry.getKey();
                break;
            }
        }
        return result;
    }
        
    private String getNextOneTrigger () {
        String result = null;
        for (Map.Entry <String, BCP_Result> entry : oneFixingResults.entrySet()){
            if (entry.getValue()==null){
                result = entry.getKey();
                break;
            }
        }
        return result;
    }
    
    double getTwoPower (int power) {
        double result = DOUBLE_ZERO + ONE;
        
        for (int index = ZERO; index < power; index ++){
            result *= TWO;
        }
        
        return result;
    }
    
}
