/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.cltlaugust2020.bcp;

import static ca.mcmaster.cltlaugust2020.Constants.*;
import ca.mcmaster.cltlaugust2020.common.HyperCube;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 *
 * @author tamvadss
 */
public class BCP_Result {
    
    //fixings found, value false indicates 0 fixing
    public Map<String, Boolean> varFixingsFound = new HashMap<String, Boolean> ();    
          
    //does this trigger lead to infeasibility, ie the conditions of a infeasible hypercube are all satisfied
    public boolean isInfeasibilityDetected = false;      
    
    public  TreeMap<Integer, List<HyperCube>> remainingInfeasibleCubes =null;
        
    //volume of nogoods eliminated, key by priority, value not used
    //public  TreeMap <Double, Boolean>  eliminated_Nogoods_Map  =  new  TreeMap <Double, Boolean>(); 
    
    public TreeMap <Double , Integer> volumeOf_removedCubes_ByPriority = new TreeMap <Double , Integer>();
            
}
