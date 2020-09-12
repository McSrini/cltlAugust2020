/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.cltlaugust2020.cplex;

import ca.mcmaster.cltlaugust2020.bcp.Trigger;
import ca.mcmaster.cltlaugust2020.common.HyperCube;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

/**
 *
 * @author tamvadss
 */
public class NodePayload {
    
    public TreeMap <Integer, List<HyperCube > > infeasibleCubeMap ;
    public Set<String> inherited_ZeroDominators  = new HashSet<String>();
    public Set<String> inherited_OneDominators   = new HashSet<String>();
    
}
