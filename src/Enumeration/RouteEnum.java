package Enumeration;

import Common.*;
import static AlgoRun.AlgoParam.capacity;

/*
import ilog.concert.*;
import ilog.cplex.IloCplex;
import static ilog.concert.IloNumVarType.Bool;
*/

import gurobi.*;
import static gurobi.GRB.IntParam.OutputFlag;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class RouteEnum{
    public Instance instance;
    int NUM;

    public RouteEnum(Instance inst) {
        this.instance = inst;
        this.NUM = inst.NUM;
    }

    public void run() throws ParseException {
        long startTime = System.currentTimeMillis();

        //Enumerated solution
        ArrayList<Route> allRoutes = enmuProcess();
        instance.routesEnmu = solve_Gurobi(allRoutes);
        instance.routesEnmu.entrySet().removeIf(route -> route.getValue().R.size() == 2);

        //Double check the solution
        instance.SolutionModify(instance.routesEnmu, "enumeration");
        instance.CheckRoutes(instance.routesEnmu, "enumeration");

        instance.subtEnum = instance.generateSubt(instance.routesEnmu);
        instance.COST_enum = instance.Calculation(instance.routesEnmu);

        //Algorithm stopped
        long endTime = System.currentTimeMillis();
        instance.TIME_enum = (endTime-startTime)/1000.0/60.0;
        System.out.printf("------- Exact algorithm completed: %.4f min, %d 2-routes, %d 3-routes, %d 4-routes -------%n",
                instance.TIME_enum, instance.Two_enum, instance.Three_enum, instance.Four_enum);
    }

    //Enumerate all feasible routes
    public ArrayList<Route> enmuProcess() throws ParseException {
        ArrayList<Route> Routes = new ArrayList<>();
        int num_LTL = NUM;

        //枚举所有可行的单独路径
        ENUM_LTLRoutes(0);
        Routes.addAll(instance.LTL_Routes);

        // 所有可行的包含两个订单的路径
        if(instance.LIFO){
            ENUM_TwoRoutes_LIFO(num_LTL);
        } else {
            ENUM_TwoRoutes(num_LTL);
        }
        instance.Two_enum = instance.TWO_Routes.size();
        Routes.addAll(instance.TWO_Routes);

        // 枚举所有可行包含3个订单的路径(最优路径）
        if(instance.maxOrders >= 3){
            if (instance.LIFO) {
                ENUM_ThreeRoutes_LIFO(num_LTL + instance.Two_enum);
            } else {
                ENUM_ThreeRoutes(num_LTL + instance.Two_enum);
            }
            instance.Three_enum = instance.THREE_Routes.size();
            Routes.addAll(instance.THREE_Routes);
        }

        // 枚举所有可行包含4个订单的路径(最优路径）
        if (instance.maxOrders >= 4) {
            if (instance.LIFO) {
                ENUM_FourRoutes_LIFO(num_LTL + instance.Two_enum + instance.Three_enum);
            } else {
                ENUM_FourRoutes(num_LTL + instance.Two_enum + instance.Three_enum);
            }
            instance.Four_enum = instance.FOUR_Routes.size();
            Routes.addAll(instance.FOUR_Routes);
        }

        //Also add LCS routes if possible
        int id = Routes.size(), num = 0;
        for (Route route : instance.LCS_Routes) {
            if (isNewRoute(Routes,route)) {
                id ++;
                route.id = id;
                Routes.add(route);
                num ++;

                int n = route.R.size();
                if (n <= 4) {
                    continue;
                } else if (n <= 6) {
                    instance.TWO_Routes.add(route);
                } else if (n <= 8) {
                    instance.THREE_Routes.add(route);
                } else {
                    instance.FOUR_Routes.add(route);
                }
            }
        }
        System.out.printf("Add %d LCS routes into the enumerated route sets %n", num);

        //All routes
        instance.NUM_enum = Routes.size();
        instance.Two_enum = instance.TWO_Routes.size();
        instance.Three_enum = instance.THREE_Routes.size();
        instance.Four_enum = instance.FOUR_Routes.size();
        return Routes;
    }

    public void ENUM_LTLRoutes(int startId){
        instance.LTL_Routes = new ArrayList<>();
        //枚举所有可行的单独路径
        int num = startId;
        for (int i = 1; i <= NUM; i++) {
            Route route = new Route();
            route.id = num;
            route.R.add(0);
            route.R.add(i);
            route.R.add(i + NUM);
            route.R.add(2 * NUM + 1);
            route.cost = instance.SignleCost[i];
            if (instance.feasible(route,false)) {
                instance.LTL_Routes.add(route);
                num ++;
            }
        }
        System.out.printf("Enumerating LTL routes stopped: iter %d, routes %d %n",
                num, instance.LTL_Routes.size());
    }

    public void ENUM_TwoRoutes(int startId){
        int id = startId, num = 0;
        long startTime = System.currentTimeMillis();
        instance.TWO_Routes = new ArrayList<>();
        HashSet<HashSet<Integer>> two_colations = new HashSet<>();
        for (int i = 1; i <= NUM; i++) {
            for (int j = 1; j <= NUM; j++) {
                if(i == j){
                    continue;
                }
                //容量约束
                if (instance.AllCustomers.get(i).demand+instance.AllCustomers.get(j).demand > capacity) {
                    continue;
                }
                //联盟不重复
                HashSet<Integer> two_colation = new HashSet<>();
                two_colation.add(i);
                two_colation.add(j);
                if(two_colations.contains(two_colation)){
                    continue;
                }

                two_colations.add(two_colation);

                double best = Double.MAX_VALUE;
                Route best_route = new Route();
                int[] coalition = new int[]{i, j};
                for (int pick1 : coalition) {
                    for (int pick2 : coalition) {
                        if (pick1 == pick2 || instance.Graph[pick1][pick2].distance > instance.radius) { // 两个pick点之间满足半径约束
                            continue;
                        }
                        for (int deli1 : coalition) {
                            for (int deli2 : coalition) {
                                if ( deli1 == deli2 || instance.Graph[deli1+NUM][deli2+NUM].distance > instance.radius ) {
                                    continue;
                                }
                                Route route = new Route();
                                route.R.add(0);
                                route.R.add(pick1);
                                route.R.add(pick2);
                                route.R.add(deli1 + NUM);
                                route.R.add(deli2 + NUM);
                                route.R.add(2 * NUM + 1);
                                if (instance.feasible(route,false) && route.cost < best) {
                                    best = route.cost;
                                    best_route = route.cloneRoute();
                                }
                            }
                        }
                    }
                }
                if (best != Double.MAX_VALUE) {
                    best_route.id = id;
                    instance.TWO_Routes.add(best_route);
                    id++;
                }
                num ++;
            }
        }

        double tt = (System.currentTimeMillis() - startTime)/1000.0/60;
        System.out.printf("Enumerating 2-routes stopped: iter %d, time %.2f min, routes %d %n",
                num, tt, instance.TWO_Routes.size());
    }

    public void ENUM_TwoRoutes_LIFO(int startId) {
        int id = startId, num = 0;
        long startTime = System.currentTimeMillis();
        instance.TWO_Routes = new ArrayList<>();
        HashSet<HashSet<Integer>> two_colations = new HashSet<>();
        for (int i = 1; i <= NUM; i++) {
            for (int j = 1; j <= NUM; j++) {
                if (i == j) {
                    continue;
                }
                //容量约束
                if (instance.AllCustomers.get(i).demand + instance.AllCustomers.get(j).demand > capacity) {
                    continue;
                }
                //联盟不重复
                HashSet<Integer> two_colation = new HashSet<>();
                two_colation.add(i);
                two_colation.add(j);
                if (two_colations.contains(two_colation)) {
                    continue;
                }

                two_colations.add(two_colation);

                double best = Double.MAX_VALUE;
                Route best_route = new Route();
                int[] coalition = new int[]{i, j};
                for (int pick1 : coalition) {
                    for (int pick2 : coalition) {
                        if (pick1 == pick2 || instance.Graph[pick1][pick2].distance > instance.radius) { // 两个pick点之间满足半径约束
                            continue;
                        }

                        //LIFO
                        if (instance.Graph[pick2+NUM][pick1+NUM].distance > instance.radius) {
                            continue;
                        }

                        Route route = new Route();
                        route.R.add(0);
                        route.R.add(pick1);
                        route.R.add(pick2);
                        route.R.add(pick2 + NUM);
                        route.R.add(pick1 + NUM);
                        route.R.add(2 * NUM + 1);
                        if (instance.feasible(route,false) && route.cost < best) {
                            best = route.cost;
                            best_route = route.cloneRoute();
                        }

                    }
                }
                if (best != Double.MAX_VALUE) {
                    best_route.id = id;
                    instance.TWO_Routes.add(best_route);
                    id++;
                }
                num ++;
            }
        }

        double tt = (System.currentTimeMillis() - startTime)/1000.0/60;
        System.out.printf("Enumerating LIFO 2-routes stopped: iter %d, time %.2f min, routes %d %n",
                num, tt, instance.TWO_Routes.size());
    }

    public void ENUM_ThreeRoutes(int StartId) {
        int id = StartId, num = 0;
        long startTime = System.currentTimeMillis();
        instance.THREE_Routes = new ArrayList<>();
        HashSet<HashSet<Integer>> three_colations = new HashSet<>();

        for (int i = 1; i <= NUM; i++) {
            for (int j = 1; j <= NUM; j++) {
                if (i == j) {
                    continue;
                }
                for (int k = 1; k <= NUM; k++) {
                    if (k == j || k == i) {
                        continue;
                    }
                    // 装载约束
                    if (instance.AllCustomers.get(i).demand + instance.AllCustomers.get(j).demand
                            + instance.AllCustomers.get(k).demand > capacity) {
                        continue;
                    }
                    //联盟不重复
                    HashSet<Integer> three_colation = new HashSet<>();
                    three_colation.add(i);
                    three_colation.add(j);
                    three_colation.add(k);
                    if (three_colations.contains(three_colation)) {
                        continue;
                    }
                    three_colations.add(three_colation);

                    int[] colation = new int[]{i, j, k};
                    double best = Double.MAX_VALUE;
                    Route best_route = new Route();
                    for (int pick1 : colation) {
                        for (int pick2 : colation) {
                            if (pick1 == pick2 || instance.Graph[pick1][pick2].distance > instance.radius) {
                                continue;
                            }
                            for (int pick3 : colation) {
                                if (pick1 == pick3 || pick2 == pick3 || instance.Graph[pick2][pick3].distance > instance.radius) {
                                    continue;
                                }
                                for (int deli1 : colation) {
                                    for (int deli2 : colation) {
                                        if (deli1 == deli2 || instance.Graph[deli1 + NUM][deli2 + NUM].distance > instance.radius) {
                                            continue;
                                        }
                                        for (int deli3 : colation) {
                                            if (deli3 == deli1 || deli3 == deli2 || instance.Graph[deli2 + NUM][deli3 + NUM].distance
                                                    > instance.radius) {
                                                continue;
                                            }
                                            Route route = new Route();
                                            route.R.add(0);
                                            route.R.add(pick1);
                                            route.R.add(pick2);
                                            route.R.add(pick3);
                                            route.R.add(deli1 + NUM);
                                            route.R.add(deli2 + NUM);
                                            route.R.add(deli3 + NUM);
                                            route.R.add(2 * NUM + 1);
                                            if (instance.feasible(route,false) && route.cost < best) {
                                                best = route.cost;
                                                best_route = route.cloneRoute();
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (best != Double.MAX_VALUE) {
                        best_route.id = id;
                        instance.THREE_Routes.add(best_route);
                        id++;
                    }
                    num ++;
                }
            }
        }

        double tt = (System.currentTimeMillis() - startTime)/1000.0/60;
        System.out.printf("Enumerating 3-routes stopped: iter %d, time %.2f min, routes %d %n",
                num, tt, instance.THREE_Routes.size());
    }

    public void ENUM_ThreeRoutes_LIFO(int StartId) {
        int id = StartId, num = 0;
        long startTime = System.currentTimeMillis();
        instance.THREE_Routes = new ArrayList<>();
        HashSet<HashSet<Integer>> three_colations = new HashSet<>();

        for (int i = 1; i <= NUM; i++) {
            for (int j = 1; j <= NUM; j++) {
                if (i == j) {
                    continue;
                }
                for (int k = 1; k <= NUM; k++) {
                    if (k == j || k == i) {
                        continue;
                    }
                    // 装载约束
                    if (instance.AllCustomers.get(i).demand + instance.AllCustomers.get(j).demand
                            + instance.AllCustomers.get(k).demand > capacity) {
                        continue;
                    }
                    //联盟不重复
                    HashSet<Integer> three_colation = new HashSet<>();
                    three_colation.add(i);
                    three_colation.add(j);
                    three_colation.add(k);
                    if (three_colations.contains(three_colation)) {
                        continue;
                    }
                    three_colations.add(three_colation);

                    int[] colation = new int[]{i, j, k};
                    double best = Double.MAX_VALUE;
                    Route best_route = new Route();
                    for (int pick1 : colation) {
                        for (int pick2 : colation) {
                            if (pick1 == pick2 || instance.Graph[pick1][pick2].distance > instance.radius) {
                                continue;
                            }
                            for (int pick3 : colation) {
                                if (pick1 == pick3 || pick2 == pick3 || instance.Graph[pick2][pick3].distance > instance.radius) {
                                    continue;
                                }

                                //LIFO
                                if(instance.Graph[pick3+NUM][pick2+NUM].distance > instance.radius || instance.Graph[pick2+NUM][pick1+NUM].distance
                                        > instance.radius) {
                                    continue;
                                }

                                Route route = new Route();
                                route.R.add(0);
                                route.R.add(pick1);
                                route.R.add(pick2);
                                route.R.add(pick3);
                                route.R.add(pick3 + NUM);
                                route.R.add(pick2 + NUM);
                                route.R.add(pick1 + NUM);
                                route.R.add(2 * NUM + 1);
                                if (instance.feasible(route, false) && route.cost < best) {
                                    best = route.cost;
                                    best_route = route.cloneRoute();
                                }
                            }
                        }
                    }
                    if (best != Double.MAX_VALUE) {
                        best_route.id = id;
                        instance.THREE_Routes.add(best_route);
                        id++;
                    }
                    num ++;
                }
            }
        }

        double tt = (System.currentTimeMillis() - startTime)/1000.0/60;
        System.out.printf("Enumerating LIFO 3-routes stopped: iter %d, time %.2f min, routes %d %n",
                num, tt, instance.THREE_Routes.size());
    }

    public void ENUM_FourRoutes(int StartId) {
        int id = StartId, num = 0;
        double maxT; // maximum search time limit: hour
        if (NUM <= 400) {
            maxT = 3.0;
        } else if (NUM <= 500) {
            maxT = 4.0;
        } else {
            maxT = 6.0;
        }

        instance.FOUR_Routes = new ArrayList<>();
        HashSet<HashSet<Integer>> four_colations = new HashSet<>();
        long startTime = System.currentTimeMillis();

        out:for (int i = 1; i <= NUM; i++) {
            for (int j = 1; j <= NUM; j++) {
                if (i == j) {
                    continue;
                }
                for (int k = 1; k <= NUM; k++) {
                    if (k == j || k == i) {
                        continue;
                    }
                    for (int kk = 1; kk <= NUM; kk++) {
                        if (kk == k || kk == i || kk == j) {
                            continue;
                        }

                        // 装载约束
                        if (instance.AllCustomers.get(i).demand + instance.AllCustomers.get(j).demand
                                + instance.AllCustomers.get(k).demand + instance.AllCustomers.get(kk).demand > capacity) {
                            continue;
                        }
                        //联盟不重复
                        HashSet<Integer> four_colation = new HashSet<>();
                        four_colation.add(i);
                        four_colation.add(j);
                        four_colation.add(k);
                        four_colation.add(kk);
                        if (four_colations.contains(four_colation)) {
                            continue;
                        }

                        four_colations.add(four_colation);

                        int[] colation = new int[]{i, j, k, kk};
                        double best = Double.MAX_VALUE;
                        Route best_route = new Route();
                        for (int pick1 : colation) {
                            for (int pick2 : colation) {
                                if (pick1 == pick2 || instance.Graph[pick1][pick2].distance > instance.radius) {
                                    continue;
                                }
                                for (int pick3 : colation) {
                                    if (pick1 == pick3 || pick2 == pick3 || instance.Graph[pick2][pick3].distance > instance.radius) {
                                        continue;
                                    }
                                    for (int pick4 : colation) {
                                        if (pick4 == pick3 || pick4 == pick2 || pick4 == pick1 || instance.Graph[pick3][pick4].distance
                                                > instance.radius) {
                                            continue;
                                        }
                                        for (int deli1 : colation) {
                                            for (int deli2 : colation) {
                                                if (deli1 == deli2 || instance.Graph[deli1 + NUM][deli2 + NUM].distance > instance.radius) {
                                                    continue;
                                                }
                                                for (int deli3 : colation) {
                                                    if (deli3 == deli1 || deli3 == deli2 || instance.Graph[deli2 + NUM][deli3 + NUM].distance
                                                            > instance.radius) {
                                                        continue;
                                                    }
                                                    for (int deli4 : colation) {
                                                        if (deli4 == deli1 || deli4 == deli2 || deli4 == deli3
                                                                || instance.Graph[deli3 + NUM][deli4 + NUM].distance > instance.radius) {
                                                            continue;
                                                        }
                                                        Route route = new Route();
                                                        route.R.add(0);
                                                        route.R.add(pick1);
                                                        route.R.add(pick2);
                                                        route.R.add(pick3);
                                                        route.R.add(pick4);
                                                        route.R.add(deli1 + NUM);
                                                        route.R.add(deli2 + NUM);
                                                        route.R.add(deli3 + NUM);
                                                        route.R.add(deli4 + NUM);
                                                        route.R.add(2 * NUM + 1);
                                                        if (instance.feasible(route,false) && route.cost < best) {
                                                            best = route.cost;
                                                            best_route = route.cloneRoute();
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if (best != Double.MAX_VALUE) {
                            best_route.id = id;
                            instance.FOUR_Routes.add(best_route);
                            id ++;
                        }
                        num ++;

                        double tt = (System.currentTimeMillis() - startTime)/1000.0/3600;
                        if (tt > maxT) {
                            break out; //exceeding the time limit
                        }
                    }
                }
            }
        }

        double tt = (System.currentTimeMillis() - startTime)/1000.0/60;
        System.out.printf("Enumerating 4-routes stopped: iter %d, time %.2f min, routes %d %n",
                num, tt, instance.FOUR_Routes.size());
    }

    public void ENUM_FourRoutes_LIFO(int StartId ) {
        int id = StartId, num = 0;
        double maxT; // maximum search time limit: hour
        if (NUM <= 400) {
            maxT = 3.0;
        } else if (NUM <= 500) {
            maxT = 4.0;
        } else {
            maxT = 6.0;
        }

        instance.FOUR_Routes = new ArrayList<>();
        HashSet<HashSet<Integer>> four_colations = new HashSet<>();
        long startTime = System.currentTimeMillis();
        out:for (int i = 1; i <= NUM; i++) {
            for (int j = 1; j <= NUM; j++) {
                if (i == j) {
                    continue;
                }
                for (int k = 1; k <= NUM; k++) {
                    if (k == j || k == i) {
                        continue;
                    }
                    for (int kk = 1; kk <= NUM; kk++) {
                        if (kk == k || kk == i || kk == j) {
                            continue;
                        }

                        // 装载约束
                        if (instance.AllCustomers.get(i).demand + instance.AllCustomers.get(j).demand + instance.AllCustomers.get(k).demand
                                + instance.AllCustomers.get(kk).demand > capacity) {
                            continue;
                        }
                        //联盟不重复
                        HashSet<Integer> four_colation = new HashSet<>();
                        four_colation.add(i);
                        four_colation.add(j);
                        four_colation.add(k);
                        four_colation.add(kk);
                        if (four_colations.contains(four_colation)) {
                            continue;
                        }

                        four_colations.add(four_colation);

                        int[] colation = new int[]{i, j, k, kk};
                        double best = Double.MAX_VALUE;
                        Route best_route = new Route();
                        for (int pick1 : colation) {
                            for (int pick2 : colation) {
                                if (pick1 == pick2 || instance.Graph[pick1][pick2].distance > instance.radius) {
                                    continue;
                                }
                                for (int pick3 : colation) {
                                    if (pick1 == pick3 || pick2 == pick3 || instance.Graph[pick2][pick3].distance > instance.radius) {
                                        continue;
                                    }
                                    for (int pick4 : colation) {
                                        if (pick4 == pick3 || pick4 == pick2 || pick4 == pick1 || instance.Graph[pick3][pick4].distance
                                                > instance.radius) {
                                            continue;
                                        }
                                        //LIFO
                                        if (instance.Graph[pick4+NUM][pick3+NUM].distance > instance.radius || instance.Graph[pick3+NUM][pick2+NUM].distance
                                                > instance.radius || instance.Graph[pick2+NUM][pick1+NUM].distance > instance.radius) {
                                            continue ;
                                        }

                                        Route route = new Route();
                                        route.R.add(0);
                                        route.R.add(pick1);
                                        route.R.add(pick2);
                                        route.R.add(pick3);
                                        route.R.add(pick4);
                                        route.R.add(pick4 + NUM);
                                        route.R.add(pick3 + NUM);
                                        route.R.add(pick2 + NUM);
                                        route.R.add(pick1 + NUM);
                                        route.R.add(2 * NUM + 1);
                                        if (instance.feasible(route,false) && route.cost < best) {
                                            best = route.cost;
                                            best_route = route.cloneRoute();
                                        }
                                    }
                                }
                            }
                        }
                        if (best != Double.MAX_VALUE) {
                            best_route.id = id;
                            instance.FOUR_Routes.add(best_route);
                            id ++;
                        }
                        num ++;

                        double tt = (System.currentTimeMillis() - startTime)/1000.0/3600;
                        if (tt > maxT) {
                            break out; //exceeding the time limit
                        }
                    }
                }
            }
        }

        double tt = (System.currentTimeMillis() - startTime)/1000.0/60;
        System.out.printf("Enumerating LIFO 4-routes stopped: iter %d, time %.2f min, routes %d %n",
                num, tt, instance.FOUR_Routes.size());
    }

    public boolean isNewRoute(ArrayList<Route> Routes, Route newRoute) {
        for (Route route : Routes) {
            int first = newRoute.R.get(1), last = newRoute.R.get(newRoute.R.size()-2);
            int org = route.R.get(1), dest = route.R.get(route.R.size()-2);
            boolean flag = (first == org) && (last == dest);
            if (route.R.size() == newRoute.R.size() && flag && Math.abs(route.load - newRoute.load) < 1e-5
                    && Math.abs(route.cost - newRoute.cost) < 1e-5) {
                return false; //already included
            }
        }
        return true; //a new route
    }

    //Solve the optimization model by Gurobi
    public HashMap<Integer,Route> solve_Gurobi(ArrayList<Route> ROUTES) {
        long tt0 = System.currentTimeMillis();
        HashMap<Integer,Route> routeSol = new HashMap<>();
        int NUM = instance.NUM;

        try {
            //Setup model
            GRBEnv env = new GRBEnv();
            GRBModel model = new GRBModel(env);
            model.set(GRB.StringAttr.ModelName, "OPTIMIZATION");

            //Decision variables
            GRBVar[] x = new GRBVar[ROUTES.size()];
            for (int j = 0; j < ROUTES.size(); j++) {
                Route route = ROUTES.get(j);
                x[j] = model.addVar(0.0,1.0,route.cost,GRB.BINARY,"x" + "_" + j);
            }

            //Objective function: sum_{r}C_r*x_r
            model.set(GRB.IntAttr.ModelSense, GRB.MINIMIZE);
            /*GRBLinExpr expr = new GRBLinExpr();
            for (int j = 0; j < ROUTES.size(); j++) {
                Route route = ROUTES.get(j);
                expr.addTerm(route.cost, x[j]);
            }
            model.setObjective(expr, GRB.MINIMIZE);  */

            //Constraint matrix: a_ir
            double[][] constrMatrix = new double[NUM + 1][ROUTES.size()]; //(NUM+1) x R
            for (int j = 0; j < ROUTES.size(); j++) {
                Route route = ROUTES.get(j);
                for (int i = 1; i < route.R.size() / 2; i++) {
                    constrMatrix[route.R.get(i)][j] = 1;
                }
            }

            //Set-covering constraint: sum_{r}a_{ir}*x_{r} = 1, i = 1,...,N;
            for (int i = 1; i <= NUM; i++) {
                GRBLinExpr exprCostr = new GRBLinExpr();
                exprCostr.addTerms(constrMatrix[i], x);
                // model.addConstr(exprCostr, GRB.GREATER_EQUAL, 1.0, "cons" + i);
                model.addConstr(exprCostr, GRB.EQUAL, 1.0, "cons" + i);
            }

            //Solve the model
            model.set(GRB.IntParam.OutputFlag, 0); //no log output
            model.set("MIPGap", "1e-4");
            model.optimize();

            //Check validity of solution
            int status = model.get (GRB.IntAttr.Status);
            if (status == GRB.Status.INF_OR_UNBD || status == GRB.Status.INFEASIBLE){
                System.out.println ("###### The optimization problem is NOT feasible: " + status + " ######");
                System.exit(0);
            }

            //The solution
            for (int j = 0; j < ROUTES.size(); j++) {
                if (Math.abs(x[j].get(GRB.DoubleAttr.X) - 1) <= 1e-5) {
                    routeSol.put(ROUTES.get(j).id, ROUTES.get(j));
                }
            }

            //Optimum
            double obj = model.get(GRB.DoubleAttr.ObjVal);

            //Result
            long tt1 = System.currentTimeMillis();
            double tt = (tt1 - tt0)/1000.0;
            System.out.printf("Exact model solved: status %s, time %.4f sec, cost %.4f %n", status, tt, obj);

            //Clear the model
            model.dispose();
            env.dispose();
        } catch (GRBException e) {
            System.err.println("Error code: " + e.getErrorCode() + ". " + e.getMessage());
        }

        //End
        return routeSol;
    }


    /*
    //Solve the optimization model by Cplex
    public HashMap<Integer,Route> solve_Cplex(ArrayList<Route> Routes) {
        HashMap<Integer,Route> solution = new HashMap<>();

        //定义模型
        IloCplex model = null;
        try {
            model = new IloCplex();
            //定义决策变量
            IloNumVar[] X = model.numVarArray(Routes.size(), 0,1,Bool);

            //生成系数矩阵
            int[][] constraints = new int[instance.NUM+1][Routes.size()];
            for (Route route : Routes){
                for (int j = 1; j < route.R.size() / 2; j++) {
                    int cus = route.R.get(j); //id of the order
                    constraints[cus][route.id] = 1;
                }
            }

            //成本向量
            double[] costs = new double[Routes.size()];
            for (Route route : Routes){
                costs[route.id] = route.cost;
            }

            //添加约束: 每个订单被服务一次
            for(int i = 1; i <= instance.NUM; i++){
                model.addEq(model.scalProd(constraints[i],X),1); //*.addEq; *.addGe
            }

            //添加目标函数
            model.addMinimize(model.scalProd(costs,X));
            model.setOut(null);

            if (!model.solve()) {
                System.err.println("###### The enumeration set-covering model is NOT feasible! ######");
                System.exit(0);
            }

            //记录当前解
            System.out.printf("CPLEX model results: %s, %.4f %n", model.getStatus(), model.getObjValue());
            double[] val = model.getValues(X);
            for(int i = 0; i < val.length; i++){
                if(val[i] == 1){
                    solution.put(i, Routes.get(i));
                }
            }
            model.end();
        } catch (IloException e) {
            e.printStackTrace();
        }
        return solution;
    }
     */


}
