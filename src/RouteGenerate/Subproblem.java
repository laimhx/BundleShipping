package RouteGenerate;

import AlgoRun.AlgoParam;
import Common.*;
import static AlgoRun.AlgoParam.*;
import static AlgoRun.AlgoParam.SPMAXN;
//import static AlgoRun.AlgoParam.capacity;

import java.text.ParseException;
import java.util.*;


public class Subproblem {
    public Instance instance;
    public Double[] assignment;
    public HashSet<HashSet<Integer>> coalSearch; //Record the coalitions already searched or infeasible
    public ArrayList<Selection> twoSelect;
    public HashSet<HashSet<Integer>> coalInitial; //Record the visited initial coalitions
    public ArrayList<Integer> members;
    public int startNum;
    public int MAXB;
    public int num_insert;
    public int num_replace;
    public int num_exchange;
    public int num_remove;
    public double eps;


    public Subproblem(Instance inst,Double[] assign,HashSet<HashSet<Integer>> coals,ArrayList<Selection> select,
                      ArrayList<Integer> all,int start) {
        this.instance = inst;
        this.assignment = assign;
        this.coalSearch = coals;
        this.twoSelect = select;
        this.members = all;
        this.startNum = start;
        this.MAXB = (int) (1.5*instance.maxOrders);//maximum size of routes in searching
        this.eps = 1e-5;
    }


    //根据当前成本分配寻找违反当前成本分配的路径
    public HashSet<Route> run() throws ParseException {
        HashSet<Route> newRoute = new HashSet<>();

        //Reset the initial two routes
        coalInitial = new HashSet<>();
        //twoSelect = new ArrayList<>();
        for (Selection sel:twoSelect) {
            if (sel.use < 1e6) {
                sel.use = 1;
            }
        }

        // Record the search process
        int[] sizR = new int[SPMAXR]; //length of neg routes
        double[] gamma = new double[SPMAXR];//negative-cost value of route

        // Start the search
        int iter = 0, numNew = 0, noRoute = 0;
        double timeSP = 0, SP_ini = 0, SP_srch = 0, SP_reopt = 0;
        num_insert = 0; num_replace = 0; num_exchange = 0; num_remove = 0;

        long StartTime = System.currentTimeMillis();
        while (numNew < SPMAXR && noRoute < SPMAXN && timeSP <= SPTIME) {
            long tt0 = System.currentTimeMillis();

            // Generate a random initial route
            Route initial;
            if (startNum <= 2) {
                initial = initial2Route();
            } else {
                initial = initial3Route();
            }
            if (initial.R.isEmpty()) {
                break;
            }
            // Record time for initialization
            SP_ini += (System.currentTimeMillis() - tt0)/1000.0;

            // Local search process: Insert, Replace, Intra_Exchange, Remove
            tt0 = System.currentTimeMillis();
            Route localOpt = localSearch(initial);

            // Record time for local search
            SP_srch += (System.currentTimeMillis() - tt0)/1000.0;
            if(!instance.feasible(localOpt,false)) {
                System.out.printf("###### INFEASIBLE localopt route: size %d, load %.4f, cost %.4f, value %.4f. ######%n",
                        localOpt.R.size(), localOpt.load, localOpt.cost, localOpt.gamma);
                continue;
            }

            //Reoptimize the unstable routes: LIFO
            tt0 = System.currentTimeMillis();
            Route route_best;
            if (instance.LIFO) {
                route_best = getBestRoute_LIFO(localOpt);
            } else {
                route_best = getBestRoute(localOpt);
            }
            //Record time for reoptimization
            SP_reopt += (System.currentTimeMillis() - tt0)/1000.0;

            // Check a new unstable route
            HashSet<Integer> coal = new HashSet<>();
            for(int i = 1; i < route_best.R.size()/2; i++){
                coal.add(route_best.R.get(i));
            }
            if (!coalSearch.contains(coal) && route_best.gamma < -eps){
                //Add a new route
                newRoute.add(route_best);
                //Add the new coalitions
                coalSearch.add(coal);

                //record the stability of routes
                sizR[numNew] = route_best.R.size()/2-1;
                gamma[numNew] = route_best.gamma;

                numNew ++;
                noRoute = 0;
            } else {
                noRoute ++;
            }
            //the current size of new routes
            iter ++;
            timeSP = (System.currentTimeMillis() - StartTime)/1000.0/60;
        }

        //check the new routes
        if (numNew > 0) {
            int[] newR = Arrays.copyOfRange(sizR,0,numNew);
            Arrays.sort(newR);
            int minR = newR[0], maxR = newR[numNew-1];

            double[] newDev = Arrays.copyOfRange(gamma,0,numNew);
            Arrays.sort(newDev);
            double minv = newDev[0], maxv = newDev[numNew-1];

            System.out.printf("SP routes and deviation: maxR %d, minR %d; maxGama %.4f, minGama %.4f %n",
                    maxR, minR, maxv, minv);
            System.out.printf("Subproblem time complexity: (init %.2f, local %.2f, reopt %.2f) seconds %n",
                    SP_ini, SP_srch, SP_reopt);
        }

        instance.ITER_SP = iter;
        System.out.printf("Operators' total usage: Insert %d, Replace %d, Exchange %d, Remove %d %n",
                num_insert, num_replace, num_exchange, num_remove);
        System.out.printf("****** Subproblem solved: (iter %d, newRoute %d, noRoute %d, time %.2f min) ******%n",
                iter, numNew, noRoute, timeSP);
        return newRoute;
    }


    public Route getBestRoute(Route origin){
        double best = origin.cost;
        Route route_best = origin.cloneRoute();

        ArrayList<Integer> picks = new ArrayList<>();
        for (int i = 1; i < origin.R.size()/2; i++) {
            picks.add(origin.R.get(i));
        }
        ArrayList<Integer> delis = new ArrayList<>();
        for (int i = origin.R.size()/2; i<origin.R.size()-1; i++) {
            delis.add(origin.R.get(i));
        }

        //枚举pick点排列
        ArrayList<ArrayList<Integer>> picks_list = new ArrayList<>();
        LinkedList<Integer> pick_path = new LinkedList<>();
        boolean[] pick_used = new boolean[picks.size()];
        picks_list = dfs(picks,0,pick_path,pick_used,picks_list);

        //枚举deli点排列
        ArrayList<ArrayList<Integer>> delis_list = new ArrayList<>();
        LinkedList<Integer> deli_path = new LinkedList<>();
        boolean[] deli_used = new boolean[delis.size()];
        delis_list = dfs(delis,0,deli_path,deli_used,delis_list);

        for (ArrayList<Integer> pl : picks_list) {
            for (ArrayList<Integer> dl : delis_list) {
                Route search = new Route();
                search.id = origin.id;
                search.R.add(0);
                search.R.addAll(pl);
                search.R.addAll(dl);
                search.R.add(2*instance.NUM+1);

                if (instance.feasible(search,false) && search.cost < best) {
                    best = search.cost;
                    route_best = search.cloneRoute();
                }
            }
        }

        //compute the deviation value
        setGamma(route_best);
        return route_best;
    }


    public Route getBestRoute_LIFO(Route origin){
        double best = origin.cost;
        Route route_best = origin.cloneRoute();

        ArrayList<Integer> picks = new ArrayList<>();
        for(int i = 1; i < origin.R.size()/2; i++){
            picks.add(origin.R.get(i));
        }

        //枚举pick点排列
        ArrayList<ArrayList<Integer>> picks_list = new ArrayList<>();
        LinkedList<Integer> pick_path = new LinkedList<>();
        boolean[] pick_used = new boolean[picks.size()];
        picks_list = dfs(picks,0,pick_path,pick_used,picks_list);

        for (ArrayList<Integer> pl : picks_list) {
            ArrayList<Integer> dl = new ArrayList<>();
            for (int j = pl.size() - 1; j >= 0; j--) { //LIFO deli根据pick序列生成
                dl.add(pl.get(j) + instance.NUM);
            }

            Route search = new Route();
            search.id = origin.id;
            search.R.add(0);
            search.R.addAll(pl);
            search.R.addAll(dl);
            search.R.add(2 * instance.NUM + 1);

            if (instance.feasible(search, false) && search.cost < best) {
                best = search.cost;
                route_best = search.cloneRoute();
            }
        }

        //compute the deviation value
        setGamma(route_best);
        return route_best;
    }


    // 递归枚举
    public ArrayList<ArrayList<Integer>> dfs(ArrayList<Integer> nodes,int depth,LinkedList<Integer> path,boolean[] used,ArrayList<ArrayList<Integer>> res){
        if (depth == nodes.size()) {
            res.add(new ArrayList<>(path));
        }
        for (int i = 0; i < nodes.size(); i++) {
            if (!used[i]) {
                path.add(nodes.get(i));
                used[i] = true;
                depth = depth + 1;
                dfs(nodes, depth, path, used, res);
                depth = depth - 1;
                used[i] = false;
                path.removeLast();
            }
        }
        return res;
    }


    //Randomly generate 2-order route
    public Route initial2Route() throws ParseException {
        int randN = 500;

        //sort the usage of routes
        Collections.sort(twoSelect, new Comparator<Selection>() {
            @Override
            public int compare(Selection o1, Selection o2) {
                return o2.use - o1.use;
            }
        });

        //select the least used routes
        Random r = new Random();
        int end_index = twoSelect.size() - 1;
        int bound = Math.min(randN, twoSelect.size());
        int ind = r.nextInt(bound);

        Selection sel = twoSelect.get(end_index - ind); //选择的联盟
        int k = sel.ID;
        sel.use += 1;
        Route route = instance.TWO_Routes.get(k);

        //compute the deviation value
        setGamma(route);
        //end
        return route;
    }


    //Randomly generate 3-order route
    public Route initial3Route() throws ParseException {
        Route route = new Route();
        int randN = 500;
        double timelimit; //random initialization time limit (unit: s)
        if (instance.large) {
            timelimit = 30;
        } else {
            timelimit = 15;
        }

        //sort the usage of routes
        Collections.sort(twoSelect, new Comparator<Selection>() {
            @Override
            public int compare(Selection o1, Selection o2) {
                return o2.use - o1.use;
            }
        });

        long tt0 = System.currentTimeMillis();
        double time = 0.0; int iter = 0;
        out:while (time <= timelimit) {
            time = (System.currentTimeMillis() - tt0)/1000.0;
            if (time > timelimit) {
                break;
            }

            // 从出现频率较小前10的twoCoalition中随机选择一个联盟
            Random rn = new Random();
            int end_index = twoSelect.size() - 1;
            int bound = Math.min(randN, twoSelect.size());
            int ind = rn.nextInt(bound);

            Selection sel = twoSelect.get(end_index - ind); //选择的联盟
            int k = sel.ID;
            sel.use += 1;

            Route origin = instance.TWO_Routes.get(k);
            int o1 = origin.R.get(1); // the first order
            int o2 = origin.R.get(2); // the second order            

            //Randomly add a third order
            Collections.shuffle(members);
            int noRoute = 0;
            for (int o3 : members) {
                if (o3 == o1 || o3 == o2) {
                    continue;
                }

                HashSet<Integer> coal = new HashSet<>();
                coal.add(o1); coal.add(o2); coal.add(o3);
                if (coalInitial.contains(coal)) {
                    noRoute = 0;
                    continue;
                }
                if (instance.AllCustomers.get(o1).demand + instance.AllCustomers.get(o2).demand +
                        instance.AllCustomers.get(o3).demand > capacity) {
                    continue;
                }
                if (instance.Graph[o1][o3].distance > instance.radius &&
                        instance.Graph[o3][o1].distance > instance.radius &&
                        instance.Graph[o2][o3].distance > instance.radius &&
                        instance.Graph[o3][o2].distance > (MAXB-1)*instance.radius) {
                    continue;
                }
                if (instance.Graph[o1+instance.NUM][o3+instance.NUM].distance > instance.radius &&
                        instance.Graph[o3+instance.NUM][o1+instance.NUM].distance > instance.radius &&
                        instance.Graph[o2+instance.NUM][o3+instance.NUM].distance > instance.radius &&
                        instance.Graph[o3+instance.NUM][o2+instance.NUM].distance > instance.radius) {
                    continue;
                }

                if (instance.LIFO) {
                    for (int p = 1; p <= origin.R.size()/2; p++) {
                        int d = origin.R.size() + 1 - p;
                        Route route_clone = origin.cloneRoute();
                        addcustomer(route_clone, p, d, o3);

                        //compute the cost of route and check feasibility
                        if (instance.feasible(route_clone,false)) {
                            route = route_clone;
                            coalInitial.add(coal);
                            break out;
                        }
                    }

                } else {
                    for (int p = 1; p <= origin.R.size()/2; p++) {
                        for (int d = origin.R.size()/2 + 1; d <= origin.R.size(); d++) {
                            Route route_clone = origin.cloneRoute();
                            addcustomer(route_clone, p, d, o3);

                            if (instance.feasible(route_clone,false)) {
                                route = route_clone;
                                coalInitial.add(coal);
                                break out;
                            }
                        }
                    }
                }
                //no feasible extension
                noRoute += 1;
            }

            // No feasible extensions for this 2-order route and remove
            if (noRoute >= instance.NUM) {
                sel.use = (int) 1e6;
            }
            iter ++;
        }

        if (route.R.isEmpty()) {
            System.out.printf("######## No feasible initial 3-order routes: iter %d, time %.2f ######## %n",
                    iter, time);
        } else {
            //compute the deviation value
            setGamma(route);
        }

        // return the initial
        return route;
    }


    //Local search subroutine
    public Route localSearch(Route initial) {        
        double best = initial.gamma;  // 搜索局部最佳解
        Route bestRoute = initial.cloneRoute();

        int i = 1, minL = 6; //size of a two-order route
        while (i < bestRoute.R.size()/2) {
            if (best <= -eps) {
                break;
            }
            //Select an order
            int cus = bestRoute.R.get(i);
            Route current;

            //Try operators: Insert, Replace, Intra_Exchange, Remove
            if (instance.LIFO) {
                current = Insert_LIFO(bestRoute);
            } else {
                current = Insert(bestRoute);
            }
            num_insert ++;

            if (current.gamma >= best) { //no improvement
                if (instance.LIFO) {
                    current = Intra_Exchange_LIFO(bestRoute,cus);
                } else {
                    current = Intra_Exchange(bestRoute,cus);
                }
                num_exchange ++;
            }

            if (current.gamma >= best) { //no improvement
                current = Replace(bestRoute,cus);
                num_replace ++;
            }

            if (current.gamma >= best && current.R.size() > minL) { //no improvement
                current = Remove(bestRoute,cus);
                num_remove ++;
            }

            //First-improvement search strategy
            if (current.gamma < best) {
                bestRoute = current.cloneRoute();
                best = current.gamma;
                i = 1; //reset and search again
            } else {
                i ++; //search the next
            }
        }

        //end
        return bestRoute;
    }

    //增加一个参与者
    // OutPut: 返回改善路径，若无，返回原路径
    public Route Insert(Route origin)  {
        if ((origin.R.size()-2)/2 >= MAXB){ //最大装载订单数约束
            return origin;
        }

        double origin_value = origin.gamma;
        HashSet<Integer> contain = new HashSet<>();
        for (int i = 1; i < origin.R.size()/2; i++) {
            contain.add(origin.R.get(i));
        }

        //randomize the order
        Collections.shuffle(members);

        //insert a random order
        for (int id : members) {
            if (contain.contains(id) || instance.AllCustomers.get(id).demand + origin.load > capacity) {
                continue;
            }

            double best = Double.MAX_VALUE;  // 参与者i插入当前路径的最佳成本
            Route bestRoute = origin.cloneRoute();  //参与者i插入当前路径的形成的最佳路径
            for (int p = 1; p <= origin.R.size() / 2; p++) {
                for (int d = origin.R.size()/2 + 1; d <= origin.R.size(); d++) {
                    Route route_clone = origin.cloneRoute();
                    addcustomer(route_clone, p, d, id);

                    //select the best position
                    if (instance.feasible(route_clone,false) && route_clone.cost < best) {
                        bestRoute = route_clone;
                        best = route_clone.cost;
                    }
                }
            }

            //Return the best insertion route
            setGamma(bestRoute);
            if (bestRoute.gamma < origin_value) {
                return bestRoute; //找到改善插入
            }
        }

        //no change and return the initial
        return origin;
    }


    public Route Insert_LIFO(Route origin)  {
        if ((origin.R.size()-2)/2 >= MAXB){ //最大装载订单数约束
            return origin;
        }

        double origin_value = origin.gamma;
        HashSet<Integer> contain = new HashSet<>();
        for(int i = 1; i < origin.R.size()/2; i++){
            contain.add(origin.R.get(i));
        }

        // randomize the order
        Collections.shuffle(members);

        //insert a random order
        for (int id : members) {
            if (contain.contains(id) || instance.AllCustomers.get(id).demand + origin.load > capacity) {
                continue;
            }

            double best = Double.MAX_VALUE;  // 参与者i插入当前路径的最佳成本
            Route bestRoute = origin.cloneRoute();  //参与者i插入当前路径的形成的最佳路径
            for (int p = 1; p <= origin.R.size() / 2; p++) {
                int d = origin.R.size() + 1 - p;
                Route route_clone = origin.cloneRoute();
                addcustomer(route_clone, p, d, id);

                //select the best position
                if (instance.feasible(route_clone,false) && route_clone.cost < best) {
                    bestRoute = route_clone;
                    best = route_clone.cost;
                }
            }

            //Return the best insertion route
            setGamma(bestRoute);
            if (bestRoute.gamma < origin_value) {
                return bestRoute; //找到改善插入
            }
        }

        //no change and return the initial
        return origin;
    }

    //从当前联盟中移除参与者cus
    // Input:当前联盟，目标值，需要移除的参与者
    // OutPut: 返回改善路径，若无，返回原路径
    // 不需要区分LIFO算子
    public Route Remove(Route origin,int cus){
        if (origin.R.size() == 2) {
            return origin;
        }

        double origin_value = origin.gamma;
        Route clone = origin.cloneRoute();
        removecustomer(clone, cus);

        //check feasibility
        clone.gamma = Double.MAX_VALUE;
        if (instance.feasible(clone,false)) {
            setGamma(clone);
        }

        if (clone.gamma < origin_value) {
            return clone;
        }

        //no change and return the initial
        return origin;
    }

    // 将参与者cus从当前联盟中替换
    // OutPut: 返回改善路径（First)，若无，返回原路径,不需要区分LIFO算子
    public Route Replace(Route origin,int cus) {
        double origin_value = origin.gamma;

        HashSet<Integer> contain  = new HashSet<>();
        for (int i = 1; i < origin.R.size()/2; i++) {
            contain.add(origin.R.get(i));
        }
        int pick = -1, deli = -1;  // cus在原路径的位置
        for (int i = 1; i < origin.R.size()/2; i++) {
            if (origin.R.get(i) == cus) {
                pick = i;
                break;
            }
        }
        for (int j = origin.R.size()/2; j < origin.R.size()-1; j++) {
            if (origin.R.get(j) == cus+instance.NUM) {
                deli = j;
                break;
            }
        }

        Route route_remove = origin.cloneRoute();
        removecustomer(route_remove, cus); //移除cus顾客     

        // randomize the order
        Collections.shuffle(members);

        for (int id : members) {
            if (contain.contains(id) || route_remove.load+instance.AllCustomers.get(id).demand > capacity) {
                continue;
            }

            Route route_add = route_remove.cloneRoute();
            addcustomer(route_add, pick, deli, id); //update the route cost
            if (!instance.feasible(route_add,false)) {
                continue;
            }

            //Return the first improving route
            setGamma(route_add);
            if (route_add.gamma < origin_value) {
                return route_add;
            }
        }

        //no change and return the initial
        return origin; 
    }

    // 顾客 c 路径内交换 (不改变参与者，改变访问顺序）
    // 返回改善路径，否则返回原路径
    public Route Intra_Exchange(Route origin,int cus){
        Route route_ex = origin.cloneRoute();
                
        //Determine the original positions
        int pick_ind = -1, deli_ind = -1; 
        for (int i = 1; i < origin.R.size()-1; i++) {
            if (origin.R.get(i) == cus) {
                pick_ind = i;
            }
            if (origin.R.get(i) == cus+instance.NUM) {
                deli_ind = i;
            }
        }
        
        //Exchange pickup positions    
        for (int i = 1; i < route_ex.R.size() / 2; i++) {
            if (i != pick_ind) {
                int id = route_ex.R.get(i); //order to exchange
                Route route_pick = route_ex.cloneRoute();
                
                route_pick.R.remove(pick_ind);
                route_pick.R.add(pick_ind, id);
                route_pick.R.remove(i);
                route_pick.R.add(i, cus);

                if (!instance.feasible(route_pick,false)) {
                    continue;
                }

                //Return the first improving route
                if (route_pick.cost - route_ex.cost < 0) {
                    route_ex = route_pick;
                    break;
                }
            }
        }

        //Exchange delivery positions
        for (int j = route_ex.R.size()/2; j < route_ex.R.size()-1; j++) {
            if (j != deli_ind) {
                int id = route_ex.R.get(j);
                Route route_deli = route_ex.cloneRoute();                
                
                route_deli.R.remove(deli_ind);
                route_deli.R.add(deli_ind, id);
                route_deli.R.remove(j);
                route_deli.R.add(j, cus+instance.NUM);

                if (!instance.feasible(route_deli,false)) {
                    continue;
                }

                //Return the first improving route
                if (route_deli.cost - route_ex.cost < 0) {
                    route_ex = route_deli;                    
                    break;
                }
            }
        }

        //Check route change
        if (route_ex.cost < origin.cost) {
            setGamma(route_ex);
        }
        return route_ex; 
    }

    // 顾客 c 路径内交换 (不改变参与者，改变访问顺序）
    // 返回改善路径，否则返回原路径
    public Route Intra_Exchange_LIFO(Route origin,int cus){
        Route route_ex = origin.cloneRoute();

        //Determine the original positions
        int pick_ind = -1, deli_ind = -1; //确定原来pick位置
        for (int i = 1; i < origin.R.size() / 2; i++) {
            if (origin.R.get(i) == cus) {
                pick_ind = i;
                deli_ind = origin.R.size()-1-i;
            }
        }
         
        for (int pk = 1; pk < route_ex.R.size() / 2; pk++) {
            if (pk != pick_ind) {
                int id = route_ex.R.get(pk);
                Route route = route_ex.cloneRoute();

                //Exchange pickup positions   
                route.R.remove(pick_ind);
                route.R.add(pick_ind, id);
                route.R.remove(pk);
                route.R.add(pk, cus);

                //Exchange delivery positions
                int deli = route_ex.R.size()-1-pk; // pick + deli = R.size()-1
                route.R.remove(deli_ind);
                route.R.add(deli_ind,id + instance.NUM);
                route.R.remove(deli);
                route.R.add(deli,cus+instance.NUM);

                if (!instance.feasible(route,false)) {
                    continue;
                }

                //Return the first improving route
                if (route.cost - route_ex.cost < 0) {
                    route_ex = route;
                    break;
                }
            }
        }

        //Check route change
        if (route_ex.cost < origin.cost) {
            setGamma(route_ex);
        }
        return route_ex;
    }


    //将顾客添加到路径并更新路径信息（id，startTime,load,dis,subt,time,cost,R)(重新计算）
    // 不是差量计算
    public void addcustomer(Route route,int pos_pick,int pos_deli,int Cus){
        //先添加pickup再添加deliverly点保证顺序正确
        route.R.add(pos_pick,Cus);
        route.R.add(pos_deli,Cus + instance.NUM);
    }

    //将顾客添从路径中移除更新路径信息（id,startTime,load,dis,subt,time,cost,R)（id，startTime,load,dis,subt,time,cost,R)(重新计算）
    // 不是差量计算
    public void removecustomer(Route route,int Cus){
        route.R.remove((Integer) Cus);
        route.R.remove((Integer) (Cus+instance.NUM));
    }

    //Compute teh gamma value of routes
    public void setGamma(Route route) {
        route.gamma = route.cost + assignment[0];
        for (int i = 1; i < route.R.size()/2; i++) {
            int cus = route.R.get(i);
            route.gamma -= assignment[cus];
        }
    }



}
