/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.cltlaugust2020.bcp;

import static ca.mcmaster.cltlaugust2020.Constants.*;
import static ca.mcmaster.cltlaugust2020.Parameters.*;
import ca.mcmaster.cltlaugust2020.common.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 *
 * @author tamvadss
 */
public class BCP_Runner  {
    
    public BCP_Result performBCP(TreeMap<Integer, List<HyperCube>> infeasibleCubeMap, String triggerVar, int triggerValue) {
        //
        TreeMap<Integer, List<HyperCube>> remainingInfeasibleCubes = 
                new TreeMap<Integer, List<HyperCube>> ();
        initRemainingInfeasibleCubes    (remainingInfeasibleCubes, infeasibleCubeMap)    ;
        
        //we also record some stats on cubes that conflit with the implications 
        //TreeMap <Double, Boolean> eliminated_Nogoods_Map  =  new TreeMap <Double, Boolean> (); 
        TreeMap <Double , Integer> volumeOf_removedCubes_ByPriority =  new     TreeMap <Double , Integer> ();
                
        Map<String, Boolean> varFixings = new HashMap <String, Boolean> ();
        varFixings.put ( triggerVar, ZERO==triggerValue? false: true );  
        
        boolean isInfeasibilityDetected = false;
        
        int numVarFixings = varFixings.size();
         
        do {
            remainingInfeasibleCubes = performBCP(remainingInfeasibleCubes,  varFixings , volumeOf_removedCubes_ByPriority);
            if (  remainingInfeasibleCubes ==null /*isInfeasibilityDetected*/){
                isInfeasibilityDetected= true;
                break;
            }
            if (numVarFixings==varFixings.size()){
                //no new fixings found, we are done
                break;
            }    else {
                numVarFixings=varFixings.size();
            }
              
            if (remainingInfeasibleCubes.size()==ZERO) break;
        } while (true) ;
        
        
        BCP_Result bcpResult= new BCP_Result();
        bcpResult.isInfeasibilityDetected = isInfeasibilityDetected;
        bcpResult.varFixingsFound= varFixings;
        bcpResult.remainingInfeasibleCubes= remainingInfeasibleCubes;
        bcpResult.volumeOf_removedCubes_ByPriority =volumeOf_removedCubes_ByPriority ;
        return bcpResult;
        
    }
  
    //for each remainingInfeasibleCubes do
    //if mismatch then ignore
    //else find number of non-mathcing conditions
    //
    //if all match then infeasible is hit, set remainingInfeasibleCubes to null and exit
    //if more than 2 left unmatched then add to new list of remainingInfeasibleCubes
    //if exactly 1 unmatched then collect and append new var fixing
    private TreeMap<Integer, List<HyperCube>> performBCP(TreeMap<Integer, List<HyperCube>> remainingInfeasibleCubes,  
                                  Map<String, Boolean> varFixings  ,
                                  TreeMap <Double , Integer> volumeOf_removedCubes_ByPriority){
        
        TreeMap<Integer, List<HyperCube>> new_RemainingInfeasibleCubes
                = new TreeMap<Integer, List<HyperCube>> ();
        
        boolean isInfeasibilityHit = false;
        
        for (Map.Entry<Integer, List<HyperCube>> entry :remainingInfeasibleCubes.entrySet()){
            List<HyperCube> new_InfeasibleCubeList = new ArrayList<HyperCube> ();
            
            for (HyperCube cube : entry.getValue()){
                if (! isConflict(cube, varFixings)){
                    TreeMap<String, Boolean> unMatched =  getNumUnmatchedConditions (cube,  varFixings);
                    if (ZERO == unMatched.size()){
                        //infeasiblity hit
                        isInfeasibilityHit = true; 
                        break;
                    }else if (ONE == unMatched.size()) {
                        //we must fix this var, BCP is happening !
                        Map.Entry <String, Boolean> onlyEntry = unMatched.firstEntry() ;
                        varFixings.put ( onlyEntry.getKey(), !  onlyEntry.getValue() );
                        
                        //record stats
                        recordStats_for_ConflictingCube(cube,  volumeOf_removedCubes_ByPriority) ;
                        
                    } else {
                        new_InfeasibleCubeList.add (cube );
                    }
                }else {
                    //record ststs on conflicting cube
                    recordStats_for_ConflictingCube(cube,  volumeOf_removedCubes_ByPriority) ;
                     
                } 
            }
            
            if (isInfeasibilityHit) break;
            
            if (new_InfeasibleCubeList.size()>ZERO) new_RemainingInfeasibleCubes.put ( entry.getKey() , new_InfeasibleCubeList);
        }
        
        if (isInfeasibilityHit)  new_RemainingInfeasibleCubes = null;
        
        return new_RemainingInfeasibleCubes;
        
    
    }
    
    
    private void  initRemainingInfeasibleCubes    (TreeMap<Integer, List<HyperCube>> remainingInfeasibleCubes, 
                                                   TreeMap<Integer, List<HyperCube>> infeasibleCubeMap) {
        
        for (Map.Entry <Integer, List<HyperCube>> entry : infeasibleCubeMap.entrySet()){
            List<HyperCube> list = new ArrayList<HyperCube> ();
            list.addAll( entry.getValue());
            remainingInfeasibleCubes.put ( entry.getKey(), list);
        }
        
    }
    
    

      
    
    
    //    look for hypercube which has exactly one condition missing from already fixed list
    private TreeMap<String, Boolean> getNumUnmatchedConditions (HyperCube hyperCube,   Map<String, Boolean> varFixings){
        
        TreeMap<String, Boolean> unMatched = new TreeMap<String, Boolean> ();
                
        for (String var : hyperCube.getZeroFixedVars()){
            if (unMatched.size()>=TWO) break;
            if (null== varFixings.get( var)) {
                unMatched.put(var, Boolean.FALSE);
                
            }
        }
        
        for (String var : hyperCube.getOneFixedVars()){
            if (unMatched.size()>=TWO) break;
            if (null==varFixings.get( var)) {
                unMatched.put(var, Boolean.TRUE);
            }
        }
        
        return unMatched;
        
    }
    
     
    
    private boolean isConflict (HyperCube hcube, Map<String, Boolean> varFixings) {
        boolean isConflict = false;
        
        for (String var : hcube.getZeroFixedVars()){
            if (isConflict) break;
            if (varFixings.keySet().contains(var) && varFixings.get(var)==true){
                isConflict=true;                 
            }
        }
        for (String var : hcube.getOneFixedVars()){
            if (isConflict) break;
            if (varFixings.keySet().contains(var) && varFixings.get(var)==false){
                isConflict=true;                 
            }
        }
        
        return   isConflict;
    }
    
    
    private void recordStats_for_ConflictingCube (HyperCube cube , TreeMap <Double , Integer> countOf_removedCubes_ByPriority){
        
        //
        double thisPriority = cube.getPriority();
        Integer currentVolume =  countOf_removedCubes_ByPriority.get( thisPriority );
        if (null ==currentVolume) {
            currentVolume = ZERO;
        }
        countOf_removedCubes_ByPriority.put (thisPriority , currentVolume +  cube.getVolume() ) ; // 2^-n
        
    }
    
    
       
}

