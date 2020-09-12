/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.cltlaugust2020.collection;

import static ca.mcmaster.cltlaugust2020.Constants.*;
import static ca.mcmaster.cltlaugust2020.Parameters.PRESOLVED_MIP_FILENAME;
import ca.mcmaster.cltlaugust2020.common.HyperCube;
import static ca.mcmaster.cltlaugust2020.utils.MIP_Reader.getVariables;
import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.Status;
import static java.lang.System.exit;
import java.util.List;

/**
 *
 * @author tamvadss
 * 
 * ensure that cube is instantly infeasible
 * 
 */
public class Validator {
    
    public void validate (HyperCube cube) throws Exception {
        
         IloCplex mip =  new IloCplex();
         mip.importModel(PRESOLVED_MIP_FILENAME);
        
         List<IloNumVar> varList = getVariables(mip) ;
         
         for (IloNumVar var : varList){
             if (cube.zeroFixedVars.contains( var.getName())){
                 var.setUB( ZERO);
             }else if (cube.oneFixedVars.contains( var.getName())) {
                 var.setLB(ONE);
             }
         }
          
         
         mip.solve();
         if (! mip.getStatus().equals( Status.Infeasible)) {
             System.err.println("Cube is feasible !") ;
             exit(ONE);
             
         }
         mip.end();
    }
    
}
