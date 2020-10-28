/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.cltlaugust2020.bcp;

import static ca.mcmaster.cltlaugust2020.Constants.*;
import static ca.mcmaster.cltlaugust2020.Parameters.PERF_VARIABILITY_RANDOM_GENERATOR;
import ca.mcmaster.cltlaugust2020.common.HyperCube; 
import ca.mcmaster.cltlaugust2020.drivers.TED_Driver;
import java.util.ArrayList;
import java.util.HashMap;
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
public class TED {
    
    private TreeMap <String, BCP_Result> zeroFixingResults = new TreeMap<String, BCP_Result>();
    private TreeMap <String, BCP_Result> oneFixingResults = new TreeMap<String, BCP_Result>();
    
    private TreeSet <String> zero_apexTriggersSet = new TreeSet <String>();
    private TreeSet <String> one_apexTriggersSet = new TreeSet <String>();
    
    private TreeMap <Integer, List<HyperCube > > infeasibleHypercubes;
    
    public boolean isInfeasibilityDetected = false;
    
    public TED (Set<String> fractionalVars  , TreeMap <Integer, List<HyperCube > > infeasibleHypercubes) {
        
        //
        for (String var : fractionalVars) {
            zeroFixingResults.put (var, null);
            oneFixingResults.put (var, null);
            
            zero_apexTriggersSet.add (var);
            one_apexTriggersSet.add (var);
            
        }
        this.infeasibleHypercubes = infeasibleHypercubes;
                        
    }
    
    //public String getVariable_with_Largest_MOHP_Metric   (){
         
        //return getVariable_with_MOHP_BestMetric();
    //}
    
    public String getVariable_with_Largest_Volume_Metric(double minPriority, boolean useMaxiMin) {
        
        List<String> suggestions =  new ArrayList<String> ();       
        
        Set<String> candidates =  new HashSet<String> ();        
        candidates .addAll(       this.zero_apexTriggersSet );
        candidates.addAll( this.one_apexTriggersSet);
        
        int bestKnownPrimaryMetric = -BILLION;
        int bestKnownSecondaryMetric = -BILLION;
        
        for (String candidateVar : candidates){
            
            int downVolume = ZERO;
            int upVolume = ZERO;
            for (Map.Entry <Double, Integer> entry : 
                    this.zeroFixingResults.get(candidateVar).volumeOf_removedCubes_ByPriority.descendingMap().entrySet()){
                if (entry.getKey() < minPriority) break;
                downVolume += entry .getValue();
            }
            for (Map.Entry <Double, Integer> entry : 
                    this.oneFixingResults.get(candidateVar).volumeOf_removedCubes_ByPriority.descendingMap().entrySet()){
                if (entry.getKey() < minPriority) break;
                upVolume += entry .getValue();
            }
            
            int primaryMetric = upVolume < downVolume? upVolume : downVolume;
            int secondaryMetric = upVolume > downVolume? upVolume : downVolume;
            
            if (!useMaxiMin){
                int temp = primaryMetric;
                primaryMetric = secondaryMetric;
                secondaryMetric= temp;
            }
            
            if ((primaryMetric> bestKnownPrimaryMetric) || bestKnownPrimaryMetric ==primaryMetric && 
                    bestKnownSecondaryMetric < secondaryMetric){
                
                bestKnownPrimaryMetric = primaryMetric;
                bestKnownSecondaryMetric=secondaryMetric;
                suggestions.clear();
                suggestions.add (candidateVar);
            }else if (bestKnownPrimaryMetric ==primaryMetric && 
                    bestKnownSecondaryMetric == secondaryMetric){
                suggestions.add (candidateVar);
            }
            
        }
        //System.out.println("suggestions "+ suggestions.size() + " metrics "+ bestKnownPrimaryMetric + ", "+bestKnownSecondaryMetric) ;
        int randomPosition = PERF_VARIABILITY_RANDOM_GENERATOR.nextInt(suggestions.size() );
        return suggestions.get( randomPosition);
    }
  
    public List<Trigger> getAllApexTriggers (Set<String >  zeroDominatingTriggers, 
                                             Set<String >  oneDominatingTriggers 
                                               ) {
        List<Trigger> result = new ArrayList<Trigger> () ;
        for ( String str:this.zero_apexTriggersSet){
            Trigger trigger = new Trigger ();
            trigger.varName = str;
            trigger.value=ZERO;
            zeroDominatingTriggers.add (str) ;
            result.add (trigger );                         
        }
        
        for (String str:this.one_apexTriggersSet){
            Trigger trigger = new Trigger ();
            trigger.varName =  str ;
            trigger.value=ONE;
            oneDominatingTriggers.add (str) ;
            result.add (trigger );             
        }
        
        return result;
    }
    
 
    
    public  List<Trigger>  runTheOtherSide  (  List<Trigger> apexTriggers) {
        
        List<Trigger>  complimentaryTriggers = new ArrayList<Trigger>  ();
                
        for (Trigger  apexTrigger: apexTriggers){
            Trigger complimentaryTrigger = new Trigger () ;
            complimentaryTrigger.value = apexTrigger.value==ONE? ZERO: ONE;
            complimentaryTrigger.varName= apexTrigger.varName;
            BCP_Result thisResult  =null;

            //check if bcp result already available, it will be if the var has a dominating trigger on both sides
            if (complimentaryTrigger.value==ZERO ) {                
                thisResult= this.zeroFixingResults.get(complimentaryTrigger.varName );
            } else {
                thisResult= this.oneFixingResults.get(complimentaryTrigger.varName );
            }
            if (null==thisResult) {
                //does not already exist, so run it again
                run (complimentaryTrigger,false, true) ;
            }
            
            complimentaryTriggers.add (complimentaryTrigger);
            
        }
        
        return complimentaryTriggers;
         
    }
    
    public int run ( boolean with_Deletion_Of_DominatedTriggers, Set<String> known_ZeroDominators, 
                      Set<String> known_OneDominators, boolean withInsertionOfResults) {
        
        int apexTriggerCount = ZERO ;
        
        //first run with known dominators
        for (String str: known_ZeroDominators){
            //must be frcational, or notdominated now by some other trigger
            if (  this.zeroFixingResults.containsKey( str) && null == this.zeroFixingResults.get( str)){
                // 
                //edit - better to check in the apex trigger map than in the results map, in case result is 
                //already available. So added check for null result
                //
                Trigger trigger = new Trigger();
                trigger.value= ZERO;
                trigger.varName= str;
                apexTriggerCount = run (trigger,   with_Deletion_Of_DominatedTriggers,   withInsertionOfResults ) ;
            }
        }
        
        for (String str: known_OneDominators){
            if (  this.oneFixingResults.containsKey( str) && null == this.oneFixingResults.get( str)){
                Trigger trigger = new Trigger();
                trigger.value= ONE;
                trigger.varName= str;
                apexTriggerCount = run (trigger,   with_Deletion_Of_DominatedTriggers,   withInsertionOfResults ) ;
            }
        }
        
        //now run BCP with any new fractional vars
        Trigger trigger = getNextTrigger();
        while (null!=trigger){
            apexTriggerCount = run (trigger,   with_Deletion_Of_DominatedTriggers,   withInsertionOfResults ) ;
            trigger = getNextTrigger();
        }
        
        return apexTriggerCount ;
    }
    
   
    
    //return map of trigger sand their bcp results which were deleted
    private   int  run (Trigger trigger, boolean with_Deletion_Of_DominatedTriggers, boolean withInsertionOfResults) {
        
        //int countOfDeletedTriggers = ZERO;
                 
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
                        if (  oneFixingResults.containsKey( entry.getKey()) && null == oneFixingResults.get( entry.getKey())) {
                            oneFixingResults.remove(entry.getKey() );
                            //countOfDeletedTriggers ++;
                        }
                        
                        this.one_apexTriggersSet.remove( entry.getKey()  );
                         
                    }else {
                        //0 fixing
                        if (zeroFixingResults.containsKey( entry.getKey()) && null == zeroFixingResults.get( entry.getKey()))  {
                            zeroFixingResults.remove(entry.getKey() );
                            //countOfDeletedTriggers ++;
                        }
                        
                        this.zero_apexTriggersSet.remove( entry.getKey()  );
                        
                    }
                }

                //re-insert reults for this trigger
                if (withInsertionOfResults){
                    if (trigger.value==ZERO){
                        zeroFixingResults.put (trigger.varName, bcp_Result) ;
                        
                        zero_apexTriggersSet.add(trigger.varName) ;
                                
                        
                    }else {
                        oneFixingResults.put (trigger.varName, bcp_Result) ;
                        
                        this.one_apexTriggersSet.add (trigger.varName);
                    }
                    
                    //countOfDeletedTriggers--;
                    
                }

            }
        }
        
        return this.one_apexTriggersSet.size() + this.zero_apexTriggersSet.size();
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
    
    /*private TreeSet<Double > getPriortiesLevelsRemoved (int sizeLimit) {
        TreeSet<Double > result= new TreeSet<Double> () ;
        
        for ( String var : this.zero_apexTriggersSet){
            BCP_Result thisResult = this.zeroFixingResults.get(var);
            result.addAll(thisResult.volumeOf_removedCubes_ByPriority.keySet() );
        }
        
        for (String var :  this.one_apexTriggersSet){
            BCP_Result thisResult   = this.oneFixingResults.get(var);                    
            result.addAll(thisResult.volumeOf_removedCubes_ByPriority.keySet() );            
        }
        
        //leave the top 10 in the set
        while (result.size() > sizeLimit){
            result.remove( result.first());
        }
        
        return result;
    }
    
    private String getVariable_with_MOHP_BestMetric (){
        TreeSet<Double > allPriorityLevels =  getPriortiesLevelsRemoved (BILLION);
        Set<String> candidates =  new HashSet<String> ();
        
        candidates .addAll(       this.zero_apexTriggersSet );
        candidates.addAll( this.one_apexTriggersSet);
        
        for (Double priority: allPriorityLevels.descendingSet()) {
            if (candidates.size()==ONE) break;
            candidates =getVariables_with_MOHP_BestMetric(priority, candidates, true);
            if (candidates.size()==ONE) break;
            candidates =getVariables_with_MOHP_BestMetric(priority, candidates, false);           
        }
        
        //System.out.println("candidates.get(ZERO) "+ candidates.get(ZERO)) ;
        List<String> candidates_list = new ArrayList<String> ();
        candidates_list.addAll (candidates );
        return candidates_list.get(ZERO);
    }
    
    //which var removes th elargest vol at this priority level?
    private Set<String> getVariables_with_MOHP_BestMetric ( double wantedPriority , Set<String> candidates, boolean useMaxiMin){  
        Set<String> result = new HashSet<String> ();
        
        int best_Known_primaryMetric = -BILLION;
         
        
        //assumes both sides have been populated
        for (String str : candidates){
             
            Integer  downFixings = this.zeroFixingResults.get(str).volumeOf_removedCubes_ByPriority.get( wantedPriority);
            if (null ==downFixings) downFixings = ZERO;
             
            Integer  upFixings = this.oneFixingResults.get(str).volumeOf_removedCubes_ByPriority.get( wantedPriority);
            if (null ==upFixings) upFixings = ZERO;
            
            //System.out.println(wantedPriority + " " + downFixings + " "+ upFixings) ;
             
            //asume useMaxiMin
            int this_primaryMetric = (upFixings < downFixings) ? upFixings : downFixings;
            if (!useMaxiMin){
                this_primaryMetric = (upFixings < downFixings) ? downFixings: upFixings  ;
            }
                          
            if ( this_primaryMetric>  best_Known_primaryMetric  )  {
                 best_Known_primaryMetric =this_primaryMetric;
                 result.clear();
                 result.add( str);
            } else if (best_Known_primaryMetric==this_primaryMetric ){
                 result.add( str);
            }
             
        }
       
        return result;
    }*/
  

  
}
