package RouteGenerate;

import Common.*;

/*
import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import static ilog.concert.IloNumVarType.Bool;
*/

import gurobi.*;
import static gurobi.GRB.IntParam.OutputFlag;

import java.text.ParseException;
import java.util.*;

public class BasicAllocate{
    public Instance instance;  // 当前对应实例
    public int NUM;
    public double COST;
    public HashMap<Integer,Route> resultRoutes ;
    public HashSet<Route> ThetaRoutes;
    public HashMap<int[],Double> ConstrStb;

    public BasicAllocate(Instance instance) {
        this.instance = instance;
        this.NUM = instance.NUM;
        this.COST = instance.COST_LCS;
        this.resultRoutes = instance.routesLCS;
        this.ThetaRoutes = instance.LCS_Routes;
        this.ConstrStb = instance.constrLCS;
    }

    public void Proportional_rule() throws ParseException {
        int NUM = instance.NUM;
        long startTime = System.currentTimeMillis();

        //Computing PR rule
        Double[] assignment = new Double[NUM+1];
        for(int i = 0; i<=NUM; i++) {
            assignment[i] = 0.0;
        }
        for(Route route:resultRoutes.values()){
            //System.out.printf("Route id %d: size %d, time %.2f, load %.4f, cost %.4f. %n",
            //        r.id, r.R.size(), r.time, r.load, r.cost);
            if (route.load > 0){
                double sc = 0;
                for (int i = 1; i < route.R.size() / 2; i++) {
                    int c = route.R.get(i);
                    double w = instance.AllCustomers.get(c).demand;
                    assignment[c] = assignment[c] + route.cost * (w / route.load);
                    sc = sc + assignment[c];
                    if (Double.isNaN(assignment[c]) || assignment[c] == 0) {
                        System.out.printf("####### Error in PR allocation: %d, %.4f, %.4f, %.4f ####### %n",
                                c, assignment[c], route.cost, w);
                    }
                }
            } else {
                System.out.printf("####### Error in route: %d, %.4f, %.4f ####### %n",
                        route.R.size(), route.cost, route.load);
            }
        }

        Double[] x = Arrays.copyOfRange(assignment, 1, instance.NUM);
        Arrays.sort(x);
        double minx = x[0];
        double maxx = x[x.length-1];
        System.out.printf("PR allocation: minPI %.4f, maxPI %.4f %n", minx, maxx);

        //Check budget balance
        double total = 0;
        for (int i = 1; i<=NUM; i++) {
            total = total + assignment[i];
        }
        if (Math.abs(COST - total) > 1e-6){
            System.out.printf("####### PR allocation is NOT balanced: %.4f, %.4f #######%n", COST, total);
        }

        //Check stability
        double[] stb = instance.MaxDEV(assignment,ConstrStb); //eps, dev, instb
        assignment[0] = stb[0];

        //The proportional rule
        instance.allocatePR = assignment;
        instance.epsilon_PR = stb[0];
        instance.MAXDEV_PR = stb[1];
        instance.instb_PR = stb[2];

        long endTime = System.currentTimeMillis();
        double tt = (endTime - startTime)/1000.0;
        System.out.printf("---------- PR allocation is completed: %d routes, %.2f s ----------%n",
                ConstrStb.size(), tt);
    }


    public void Dual_rule() throws ParseException {
        int NUM = instance.NUM;
        //HashSet<Route> ThetaRoutes = instance.PDRP_localRoutes;
        long startTime = System.currentTimeMillis();

        //Computing DR rule
        Double[] assignment = new Double[NUM+1];
        HashMap<int[],Double> Constr = new HashMap<>();
        for(Route r:ThetaRoutes) {
            int[] coefficient = new int[NUM+1];
            coefficient[0] = 0;
            for (int i = 1; i < r.R.size() / 2; i++) {
                int cus = r.R.get(i);
                coefficient[cus] = 1;
            }
            Constr.put(coefficient, r.cost);
        }

        Double[] pi = solveDual_Gurobi(Constr);
        for(int i = 1; i <= NUM; i++){
            if (Double.isNaN(pi[i])) {
                System.out.printf("####### Error in DR solution: %d, %.4f ####### %n", i, pi[i]);
            }
        }

        //Scaled up
        for(int i = 0; i <= NUM; i++) {
            assignment[i] = 0.0;
        }
        for(Route r: resultRoutes.values()){
            if(r.cost > 1e-6) {
                //total allocation
                double sum = 0;
                for (int i = 1; i < r.R.size() / 2; i++) {
                    int c = r.R.get(i);
                    sum = sum + pi[c];
                }
                //cover deficit
                for (int i = 1; i < r.R.size() / 2; i++) {
                    int c = r.R.get(i);
                    if (r.R.size() == 4) { //case: LTL
                        assignment[c] = r.cost;
                    } else { //case: MSTL
                        assignment[c] = assignment[c] + (r.cost / sum) * pi[c];
                    }

                    if (Double.isNaN(assignment[c])) {
                        System.out.printf("####### Error in DR allocation: %d, %.4f, %.4f, %.4f, %.4f, %d ####### %n",
                                c, assignment[c], pi[c], sum, r.cost, r.R.size());
                    }
                }
            }
        }

        Double[] x = Arrays.copyOfRange(assignment, 1, instance.NUM);
        Arrays.sort(x);
        double minx = x[0];
        double maxx = x[x.length-1];
        System.out.printf("DR allocation: minPI %.4f, maxPI %.4f %n", minx, maxx);

        //Check budget balance
        double total = 0;
        for (int i = 1; i <= NUM; i++) {
            total = total + assignment[i];
        }
        if (Math.abs(COST - total) > 1e-6){
            System.out.printf("####### DR allocation is NOT balanced: %.4f, %.4f #######%n",
                    COST, total);
        }

        //Check stability
        double[] stb = instance.MaxDEV(assignment,ConstrStb); //eps, dev, instb
        assignment[0] = stb[0];

        //The dual rule
        instance.allocateDR = assignment;
        instance.epsilon_DR = stb[0];
        instance.MAXDEV_DR = stb[1];
        instance.instb_DR = stb[2];

        long endTime = System.currentTimeMillis();
        double tt = (endTime - startTime)/1000.0;
        System.out.printf("---------- DR allocation is completed: %d routes, %.2f s ----------%n",
                Constr.size(), tt);
    }


    public Double[] solveDual_Gurobi(HashMap<int[],Double> Constrs) {
        long tt0 = System.currentTimeMillis();
        int NUM = instance.NUM;
        Double[] assignment = new Double[NUM + 1];

        try {
            //Setup model
            GRBEnv env = new GRBEnv();
            GRBModel model = new GRBModel(env);
            model.set(GRB.StringAttr.ModelName, "CORE");

            //Decision variables: pi_{i}
            GRBVar[] pi = new GRBVar[NUM + 1];
            pi[0] = model.addVar(0.0, 0, 0.0, GRB.CONTINUOUS, "pi" + "_" + 0); //dummy
            for (int i = 1; i <= NUM; i++) {
                pi[i] = model.addVar(0.0, GRB.INFINITY, 1.0, GRB.CONTINUOUS, "pi" + "_" + i);
            }

            //Objective function: max sum_{i>0} pi_i
            model.set(GRB.IntAttr.ModelSense, GRB.MAXIMIZE);
            /* GRBLinExpr expr = new GRBLinExpr();
            expr.addTerm(0, pi[0]);
            for (int i = 1; i <= NUM; i++) {
                expr.addTerm(1, pi[i]);
            }
            model.setObjective(expr, GRB.MAXIMIZE); */

            //Constraints: coalition stability \sum_{i\in R}pi_i <= cost_R
            int id = 0;
            for (int[] ind:Constrs.keySet()) {
                double[] coef = Arrays.stream(ind).mapToDouble(Double::valueOf).toArray();
                GRBLinExpr exprC = new GRBLinExpr();
                exprC.addTerms(coef, pi);
                model.addConstr(exprC, GRB.LESS_EQUAL, Constrs.get(ind), "cons" + id);
                id ++;
            }

            //Solve the model
            model.set(GRB.IntParam.OutputFlag, 0); //no log output
            model.set("Method", "2"); //1 = dual simplex; 2 = barrier method
            model.set("OptimalityTol", "1e-4"); //model.set("OptimalityTol", "1e-3")
            model.optimize();

            //Check validity of solution
            int status = model.get (GRB.IntAttr.Status);
            if (status == GRB.Status.INF_OR_UNBD || status == GRB.Status.INFEASIBLE){
                System.out.println ("###### The restricted primal problem is NOT feasible: " + status + " ######");
                System.exit(0);
            }

            //The solution
            for (int j = 0; j <= NUM; j++) {
                assignment[j] = pi[j].get(GRB.DoubleAttr.X);
            }

            //Optimum
            double obj = model.get(GRB.DoubleAttr.ObjVal);

            //Result
            long tt1 = System.currentTimeMillis();
            double tt = (tt1 - tt0)/1000.0;
            System.out.printf("Allocation model solved: status %s, time %.4f sec, epsilon %.4f %n", status, tt, obj);

            //Clear the model
            model.dispose();
            env.dispose();
        } catch (GRBException e) {
            System.err.println("Error code: " + e.getErrorCode() + ". " + e.getMessage());
        }

        return assignment;
    }


    /*
    public Double[] solveDual_Cplex (HashMap<int[], Double> Constrs) {
        int NUM = instance.NUM;
        Double[] pi = new Double[NUM+1];

        //定义模型
        IloCplex model = null;
        try {
            model = new IloCplex();
            //定义决策变量
            IloNumVar[] X = model.numVarArray(NUM+1, 0, Double.MAX_VALUE); //X[0] = ???

            //添加约束
            for (int[] ck : Constrs.keySet()) {
                model.addLe(model.scalProd(X, ck), Constrs.get(ck));
            }

            //添加目标函数
            int[] coff = new int[NUM+1];
            for(int i = 1; i <= NUM; i++)
                coff[i] = 1;
            model.addMaximize(model.scalProd(X, coff));
            model.setOut(null);

            //记录当前解
            if (model.solve()) {
                double[] val = model.getValues(X);
                for (int j = 0; j < val.length; j++) {
                    pi[j] = val[j];
                }
            }
            model.end();

        } catch (IloException e) {
            e.printStackTrace();
        }
        return pi;
    }
     */


}
