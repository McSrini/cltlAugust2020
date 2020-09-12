/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.cltlaugust2020.heuristics;

import static ca.mcmaster.cltlaugust2020.Constants.*;
import ca.mcmaster.cltlaugust2020.common.HyperCube;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 *
 * @author tamvadss
 */
public class MOMS {
    
    private Map <String, Integer> frequencyMap = new HashMap <String, Integer>();
    
    public String getBranchingVariableSuggestion (TreeMap <Integer, List<HyperCube > > collectedInfeasibleHypercubes,
            List<String> fractionalVars) {
        
        List<String> candidateVars = new ArrayList <String> ();
        candidateVars.addAll( fractionalVars);
        
        List<String> winnersOfThisRound =null;
        
        for (Map.Entry <Integer, List<HyperCube > > entry :collectedInfeasibleHypercubes.entrySet()){
            for (HyperCube cube : entry.getValue()) {
                for (String var : cube.zeroFixedVars){
                    if (!candidateVars.contains(var)) continue;
                    int currentFreq = frequencyMap.containsKey(var) ? frequencyMap.get(var): ZERO;
                    frequencyMap.put (var, ONE+currentFreq ) ;
                }
                for (String var : cube.oneFixedVars){
                    if (!candidateVars.contains(var)) continue;
                    int currentFreq = frequencyMap.containsKey(var) ? frequencyMap.get(var): ZERO;
                    frequencyMap.put (var, ONE+currentFreq ) ;
                }
            }
            
            //if there is a clear winner, we can break, else go to next level with only these winners.
            winnersOfThisRound = getWinnersOfThisRound();
            if (winnersOfThisRound.size()>ZERO) {
                candidateVars.clear();
                candidateVars.addAll( winnersOfThisRound);
            }
            if (candidateVars.size()==ONE){
                break;
            } 
        }
        
        return candidateVars.get(ZERO) ;
    }
    
    private List<String> getWinnersOfThisRound (){
        int highFreq = -ONE;
        List<String> result = new ArrayList<String>();
        for (Map.Entry <String, Integer>  entry : frequencyMap.entrySet()){
            if (entry.getValue() > highFreq){
                highFreq= entry.getValue();
                result.clear();
                result .add ( entry.getKey());
            }else if (entry.getValue() == highFreq) {
                result .add ( entry.getKey());
            }
        }
        return result;
    }
    
}
