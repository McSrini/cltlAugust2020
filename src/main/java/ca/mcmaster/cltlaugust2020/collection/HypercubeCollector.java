/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.cltlaugust2020.collection;

import static ca.mcmaster.cltlaugust2020.Constants.*;
import static ca.mcmaster.cltlaugust2020.Parameters.*;
import ca.mcmaster.cltlaugust2020.common.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 *
 * @author tamvadss
 */
public class HypercubeCollector {
    
    private TreeMap < Integer, List<HyperCube> > jobMap = new TreeMap < Integer, List<HyperCube> > ();
    
    public  TreeMap <Integer, List<HyperCube > > collectedInfeasibleHypercubes = new TreeMap <Integer, List<HyperCube > > ();
    
    public void collect (List<LowerBoundConstraint> lbcList){
        
        for (LowerBoundConstraint lbc : lbcList){
            jobMap.clear();
            List<HyperCube> list = new ArrayList<HyperCube> ();
            list.add ( new HyperCube()) ;
            jobMap.put (ZERO,list) ;
            collect(lbc)            ;
        }       
    }
    
    public void printStatistics () {
        for (Map.Entry <Integer, List<HyperCube > > entry : collectedInfeasibleHypercubes.entrySet()){
            System.out.println("Collected size " + entry.getKey()+" cubes "+ entry.getValue().size()) ;
        }        
    }
    
    private void collect (LowerBoundConstraint lbc) {
        
       
        while (jobMap.size()>ZERO && MAX_INFEASIBLE_HYPERCUBE_SIZE > jobMap.firstKey()) {
            //pick a job with the smallest number of fixings already
            int smallestKey= jobMap.firstKey() ;
            List<HyperCube> jobs = jobMap.remove(  jobMap.firstKey());
            HyperCube job = jobs.remove(ZERO);
            if (!jobs.isEmpty()) jobMap.put (smallestKey, jobs );
           
            //branch on the variable in the lbc that has the largest coeff
            Tuple tuple =  lbc.getLargestReaminingCoeff_WithHighestFreq(job);
            HyperCube newJobUp =new HyperCube();
            HyperCube newJobDown=new HyperCube();
            newJobUp.oneFixedVars.add(tuple.varName);
            newJobUp.oneFixedVars.addAll (job.oneFixedVars) ;
            newJobUp.zeroFixedVars.addAll(job.zeroFixedVars );
            
            newJobDown.zeroFixedVars.add (tuple.varName );
            newJobDown.oneFixedVars.addAll (job.oneFixedVars) ;
            newJobDown.zeroFixedVars.addAll(job.zeroFixedVars );
            
            //if either of the new cubes is infeasible , collect it
            //else insert it back into job map
            insertJob (newJobDown, lbc) ;
            insertJob (newJobUp, lbc);
        }
              
    }
    
    private void insertJob (HyperCube cube, LowerBoundConstraint lbc) {
        
        int size = cube.getSize();
        
        if (lbc.isGauranteedFeasible(cube)){
            //discard
        } else if (lbc.isGauranteed_Infeasible(cube)){
            //collect it
            
            List<HyperCube > existing =         collectedInfeasibleHypercubes.get(size);
            if (null==existing )  existing = new ArrayList<HyperCube > ();
            existing.add (cube ); 
            collectedInfeasibleHypercubes.put (size, existing);
        }else {
            //insert it back into job list
            List<HyperCube > existing =     this.jobMap.get( size);
            if (null==existing )  existing = new ArrayList<HyperCube > ();
            existing.add (cube ); 
            jobMap  .put (size, existing);
        }
    }
}
