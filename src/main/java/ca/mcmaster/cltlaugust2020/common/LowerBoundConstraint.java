/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.cltlaugust2020.common;
 
import static ca.mcmaster.cltlaugust2020.Constants.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 *
 * @author tamvadss
 */
public class LowerBoundConstraint {
    
    private TreeMap <Double, List<String>> varCoeff_Map = new TreeMap <Double, List<String>>  ();
    
    public TreeMap <Double, TreeMap<Integer,List<String>>> sorted_varCoeff_Map = 
            new TreeMap <Double,TreeMap<Integer,List<String>>>  ();
    
    //bound should be private too
    public double lowerBound ;
    
     
   
    public void sortByFrequency (TreeMap <String, Integer> frequencyMap) {
        for (Map.Entry <Double, List<String>> entry : varCoeff_Map.entrySet()){
            double thiskey = entry.getKey();
            if (!sorted_varCoeff_Map.containsKey(thiskey)) {
                sorted_varCoeff_Map.put (thiskey , new TreeMap<Integer,List<String>>());
            }
            TreeMap<Integer,List<String>> innerMap = sorted_varCoeff_Map.get(thiskey);
            List<String>  thisList = entry.getValue();
            for (String str :  thisList){
                int thisFrequency = frequencyMap.get (str);
                if (!innerMap.containsKey( thisFrequency)){
                   innerMap.put( thisFrequency, new ArrayList<String>());
                }
                List<String> existing = innerMap.get (thisFrequency );
                existing.add (str);
                innerMap.put (thisFrequency,  existing);
            }
            sorted_varCoeff_Map.put(thiskey,innerMap );
        }
        
         
        
    }
        
    public void add (String var, Double coeff) {
        List<String> existing = varCoeff_Map.get( coeff);
        if (null==existing) existing = new ArrayList<String>();
        existing.add(var);
        varCoeff_Map.put( coeff, existing);
    }
    
    public boolean isGauranteed_Infeasible (HyperCube hyperCube) {
        return getLargestPossible_LHS(hyperCube) < lowerBound;
    }
    

    
    public boolean isGauranteedFeasible (HyperCube hyperCube) {
        return getSmallestPossible_LHS(hyperCube) >= lowerBound;
    }
    
    public Tuple getLargestReaminingCoeff_WithHighestFreq (HyperCube hyperCube) {
        Tuple result = new Tuple (); 
        boolean isFound = false;
        //for (Map.Entry  <Double, List<String>>  entry :   varCoeff_Map.descendingMap().entrySet()){
        for (Map.Entry  <Double, TreeMap<Integer,List<String>>>  sortedMap_Entry  :   sorted_varCoeff_Map .descendingMap().entrySet()){
            
            for (Map.Entry <Integer,List<String>>  entry: sortedMap_Entry.getValue().descendingMap().entrySet()){
                String freeVar = getFreevar (hyperCube, entry.getValue() ); 
                if ( null!=freeVar){
                    result.ceoff= sortedMap_Entry.getKey();
                    result.varName = freeVar;
                    isFound = true;
                    break;
                }
            }   
            
            if (isFound) break;

        }
        return result;
    } 
    
    private String getFreevar (HyperCube hyperCube,  List<String> vars ){
        String result = null;
        List<String> fixedVars = new ArrayList <String> () ;
        fixedVars.addAll( hyperCube.getOneFixedVars());
        fixedVars.addAll( hyperCube.getZeroFixedVars());
        
        
        for (String var : vars){
            if (!fixedVars.contains(var)){
                result = var;
                break;
            }
        }
        
        
        return result;
    }
       
    //if smallest possible LHS   >= lowerBound , hypercube is gauranteed feasible
    private double getSmallestPossible_LHS (HyperCube hyperCube){
        
        double lhs = DOUBLE_ZERO;
        
        for ( Map.Entry  <Double, List<String>>  entry :varCoeff_Map.entrySet() ){
            final List<String> vars = entry.getValue();
            final double coeff= entry.getKey();
            for (String var: vars){
                if (hyperCube.getOneFixedVars().contains(var )){
                    lhs += coeff;
                }else if (!hyperCube.getZeroFixedVars().contains(var )){
                    //use smallest possible value
                    lhs += (coeff<=ZERO? coeff: ZERO);
                }
            }
            
        }
        
        return lhs;
    }  
    
     
    //if largest possible LHS   < lowerBound , hypercube is gauranteed infeasible
    public double getLargestPossible_LHS (HyperCube hyperCube){
        double lhs = DOUBLE_ZERO;
        
        for ( Map.Entry  <Double, List<String>>  entry :varCoeff_Map.entrySet() ){
            final List<String> vars = entry.getValue();
            final double coeff= entry.getKey();
            for (String var: vars){
                if (hyperCube.getOneFixedVars().contains(var )){
                    lhs += coeff;
                }else if (!hyperCube.getZeroFixedVars().contains(var )){
                    //use largest possible value
                    lhs += (coeff>=ZERO? coeff: ZERO);
                }
            }
            
        }
        
        return lhs;
    }
}
