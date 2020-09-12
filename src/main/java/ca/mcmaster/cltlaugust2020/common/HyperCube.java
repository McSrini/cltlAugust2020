/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.cltlaugust2020.common;

import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author tamvadss
 */
public class HyperCube {
    
    public Set<String> zeroFixedVars = new HashSet<String> () ;
    public Set<String> oneFixedVars = new HashSet<String> () ;
    
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
