package Enumeration;

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
import java.util.Arrays;
import java.util.HashMap;

public class EnmuAllocate {
    public Instance instance;

    public EnmuAllocate(Instance instance) {
        this.instance = instance;
    }

    //成本分配
    public void run() throws ParseException {
        //Start enumerated allocation
        long startTime = System.currentTimeMillis();
        double Cost = instance.COST_enum;

        HashMap<int[], Double> constrEnum = enumConstraints(); // 所有约束
        instance.allocateEnum = solve_Gurobi(constrEnum);

        //Check validity
        double total = 0;
        for (int i = 1; i <= instance.NUM; i++) {
            if (Double.isNaN(instance.allocateEnum[i])) {
                System.out.printf("####### Error in exhausted allocation: %d, %.4f ####### %n",
                        i, instance.allocateEnum[i]);
            }
            total = total + instance.allocateEnum[i];
        }
        if (Math.abs(Cost - total) > 1e-6) {
            System.out.printf("####### The exhausted allocation is NOT balanced: %.4f, %.4f ####### %n",
                    Cost, total);
        }

        //Check stability
        double[] stb = instance.MaxDEV(instance.allocateEnum,constrEnum); //eps, dev, instb
        instance.epsilon_exact = stb[0];
        instance.MAXDEV_exact = stb[1];
        instance.instb_exact = stb[2];

        //The running time of enumerated allocation
        long endTime = System.currentTimeMillis();
        double run_exhau = (endTime - startTime)/1000.0/60;
        instance.TIME_exact = run_exhau + instance.TIME_enum;
        System.out.printf("--------- Exhausted allocation completed: %d routes, %.2f min, %.4f eps ---------%n",
                instance.NUM_enum, instance.TIME_exact, instance.epsilon_exact);
    }

    public HashMap<int[], Double> enumConstraints() throws ParseException {
        int NUM = instance.NUM;
        HashMap<int[], Double> Constraints = new HashMap<>();

        //生成包含2个成员的联盟
        int num2 = instance.TWO_Routes.size();
        for(Route route:instance.TWO_Routes){
            int[] coefficient = new int[NUM + 1];
            coefficient[0] = -1;
            for(int i = 1; i < route.R.size()/2; i++){
                coefficient[route.R.get(i)] = 1;
            }
            Constraints.put(coefficient, route.cost);
        }
        System.out.println("Add all 2-route coalition stability constraints: " + num2);

        //枚举所有的3个的联盟
        if(instance.maxOrders >= 3){
            int num3 = instance.THREE_Routes.size();
            for(Route route:instance.THREE_Routes){
                int[] coefficient = new int[NUM + 1];
                coefficient[0] = -1;
                for(int i = 1; i < route.R.size()/2; i++){
                    coefficient[route.R.get(i)] = 1;
                }
                Constraints.put(coefficient, route.cost);
            }
            System.out.println("Add all 3-route coalition stability constraints: " + num3);
        }

        // 枚举所有4个订单的联盟
        if (instance.maxOrders >= 4) {
            int num4 = instance.FOUR_Routes.size();
            for(Route route:instance.FOUR_Routes){
                int[] coefficient = new int[NUM + 1];
                coefficient[0] = -1;
                for(int i = 1; i < route.R.size()/2; i++){
                    coefficient[route.R.get(i)] = 1;
                }
                Constraints.put(coefficient, route.cost);
            }
            System.out.println("Add all 4-route coalition stability constraints: " + num4);
        }
        return Constraints;
    }


    //Solve the allocation model by Gurobi
    public Double[] solve_Gurobi(HashMap<int[],Double> Constraints) {
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
            pi[0] = model.addVar(0.0, GRB.INFINITY, 1.0, GRB.CONTINUOUS, "pi" + "_" + 0);
            for (int i = 1; i <= NUM; i++) {
                pi[i] = model.addVar(0.0, GRB.INFINITY, 0.0, GRB.CONTINUOUS, "pi" + "_" + i);
            }

            //Objective function: min pi_0
            model.set(GRB.IntAttr.ModelSense, GRB.MINIMIZE);

            //Constraints: budget balance \sum_{i>0}pi_i = cost_N
            int id = 0;
            double[] constr = new double[NUM + 1];
            constr[0] = 0;
            for (int i = 1; i <= NUM; i++) {
                constr[i] = 1;
            }
            GRBLinExpr exprB = new GRBLinExpr();
            exprB.addTerms(constr, pi);
            model.addConstr(exprB, GRB.EQUAL, instance.COST_enum, "cons" + id);

            //Constraints: coalition stability -pi_0 + \sum_{i\in R}pi_i <= cost_R
            for (int[] ind:Constraints.keySet()) {
                id ++;
                double[] coef = Arrays.stream(ind).mapToDouble(Double::valueOf).toArray();
                if (Math.abs(ind[0] - coef[0]) > 1e-5) {
                    System.err.printf("######Stability constraint coeff NOT consistent : %d, %.2f ######%n", ind[0], coef[0]);
                }
                GRBLinExpr exprC = new GRBLinExpr();
                exprC.addTerms(coef, pi);
                model.addConstr(exprC, GRB.LESS_EQUAL, Constraints.get(ind), "cons" + id);
            }

            //Constraints: individual rationality pi_i <= cost_i
            for (int i = 1; i <= NUM; i++) {
                id ++;
                GRBLinExpr exprI = new GRBLinExpr();
                exprI.addTerm(1, pi[i]);
                model.addConstr(exprI, GRB.LESS_EQUAL, instance.SignleCost[i], "cons" + id);
            }

            //Constraints: no free-rider pi_i >= indcosts_i
            double[] fr = new double[NUM+1]; //1 = order shipped by MSTL; 0 = otherwise
            for (Route route:instance.routesEnmu.values()) {
                if (route.R.size() > 4) {
                    for (int i = 1; i < route.R.size()/2; i++) {
                        int c = route.R.get(i);
                        fr[c] = 1;
                    }
                }
            }

            for (int i = 1; i <= NUM; i++) {
                if(fr[i] > 0) {
                    Customer c_pick = instance.AllCustomers.get(i);  //pick点
                    double delay = instance.subtEnum.get(i+NUM);  //启发式求解subt，记录到deli点
                    double ci = instance.stop + c_pick.lateCost * delay * c_pick.demand;

                    GRBLinExpr exprF = new GRBLinExpr();
                    exprF.addTerm(1, pi[i]);
                    model.addConstr(exprF, GRB.GREATER_EQUAL, ci, "cons" + id);
                }
            }

            //Solve the model
            model.set(GRB.IntParam.OutputFlag, 0); //no log output
            model.set("Method", "2"); //1 = dual simplex; 2 = barrier method
            model.set("OptimalityTol", "1e-4"); //model.set("OptimalityTol", "1e-3")
            model.optimize();

            //Check validity of solution
            int status = model.get (GRB.IntAttr.Status);
            if (status == GRB.Status.INF_OR_UNBD || status == GRB.Status.INFEASIBLE){
                System.out.println ("###### The enum allocation problem is NOT feasible: " + status + " ######");
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
            System.out.printf("Least Core solved: status %s, time %.4f sec, epsilon %.4f %n", status, tt, obj);

            //Clear the model
            model.dispose();
            env.dispose();
        } catch (GRBException e) {
            System.err.println("Error code: " + e.getErrorCode() + ". " + e.getMessage());
        }

        return assignment;
    }


    /*
    //调用cplex求解当前问题
    public Double[] solveCore_Cplex(HashMap<int[],Double> Constraints) {
        int NUM = instance.NUM;
        Double[] allocate = new Double[NUM + 1]; //定义决策变量(X[0]为epsilon，X[i]为顾客i的分配成本
        //定义模型
        IloCplex model = null;
        try {
            model = new IloCplex();
            //定义决策变量(X[0]为epsilon，X[i]为顾客i的分配成本
            IloNumVar[] X = model.numVarArray(NUM + 1, 0, Double.MAX_VALUE);

            //添加约束
            //(1) 总分配成本约束
            double[] coeff1 = new double[NUM + 1];
            coeff1[0] = 0;
            for (int i = 1; i <= NUM; i++) {
                coeff1[i] = 1;
            }
            model.addEq(model.scalProd(X, coeff1), Cost);

            //(2) stable 约束
            for (int[] a : Constraints.keySet()) {
                model.addLe(model.scalProd(X, a), Constraints.get(a));
            }

            //(3) 优于单独配送约束
            for (int i = 1; i <= NUM; i++) {
                model.addLe(model.prod(X[i], 1), instance.SignleCost[i]);
            }

            //(4) free-rider(避免搭便车约束）
            double[] mode = new double[NUM+1]; //1 = order shipped by MSTL; 0 = otherwise
            for(Route route: instance.routesEnmu.values()){
                if(route.R.size() > 4){
                    for(int i = 1; i < route.R.size()/2; i++){
                        int c = route.R.get(i);
                        mode[c] = 1;
                    }
                }
            }

            for (int i = 1; i <= NUM; i++) {
                if(mode[i] > 0) {
                    Customer c_pick = instance.AllCustomers.get(i);  // pick点
                    double delay = instance.subtEnum.get(i+NUM);
                    model.addGe(model.prod(X[i], 1), stop + c_pick.lateCost * delay * c_pick.demand);
                }
            }

            //添加目标函数
            model.addMinimize(model.prod(X[0], 1));
            model.setParam(IloCplex.IntParam.RootAlg,IloCplex.Algorithm.Barrier);
            model.setOut(null);

            //记录当前解
            if (model.solve()) {
                double[] val = model.getValues(X);
                for (int j = 0; j < val.length; j++) {
                    allocate[j] = val[j];
                }
            }
            model.end();
        } catch (IloException e) {
            e.printStackTrace();
        }
        return allocate;
    }
     */


}
