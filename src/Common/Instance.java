package Common;

import AlgoRun.AlgoParam;
import static AlgoRun.AlgoParam.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

public class Instance {
    public String customerfile; //当前读取订单文件
    public boolean EnumMethod;  // 是否启动枚举求解路径生成
    public boolean LCMethod;
    public boolean PRMethod;
    public boolean DRMethod;

    public String StartDepot;   //起点仓库
    public String returnDepot;  //终点仓库
    public int NUM;  //总的订单数目
    public int numV;
    public int numE;

    //LIFO	 maxOrder	duration	radius	stop
    public boolean LIFO; //是否要求 last-in-first-out
    public int maxOrders; //一条路径上最大的服务订单数目
    public double timelimit; //车辆运行时间限制
    public double radius; //进行多站点式运输时，两个邻近的取货点或者两个邻近的送货点最大可捆绑距离
    public double stop; //stop-off charge
    public boolean large; //是否为大规模实例

    public HashMap<Integer,Customer> AllCustomers;  //包含所有顾客(节点）的大联盟,用于查询
    public Travel[][] Graph;  //存储网络节点的路网信息 （0车辆出发地，2*NUM+1车辆返回地，大规模实例（2*NUM+1）
    public double[][] Rate;   //存储起终点费率矩阵（不变值）
    public Double[] SignleCost;   //记录每个订单单独配送的成本 不变值 SignleCost[1]表示顾客1单独配送的费用
    public double OriginCost;  //单独配送总成本

    // 结果记录
    public String inst_name;

    //Enumeration algorithm
    public int NUM_enum;
    public int Two_enum;
    public int Three_enum;
    public int Four_enum;
    public double TIME_enum;
    public double TIME_exact;

    public int LTL_enum;
    public int MSTL2_enum;
    public int MSTL3_enum;
    public int MSTL4_enum;
    public double COST_enum;
    public double impr_enum;
    public double bundle_enum;

    public int delayOrder_enum; //基于穷举的精确解中交付延迟的订单数量
    public double delayTime_avg_enum;  //基于穷举的精确解中，对于发生交付延迟的那些订单，计算延迟时间的平均值
    public double delayTime_sd_enum;   // 基于穷举的精确解中，对于发生交付延迟的那些订单，计算延迟时间的标准差

    //Exhausted allocation
    public double epsilon_exact;
    public double MAXDEV_exact;
    public double instb_exact;
    public double SAVE_avg_exact; // 每个参与者平均节省
    public double SAVE_sd_exact;
   
    //LCS algorithm
    public int NUM_LCS;
    public int Two_LCS;
    public int Three_LCS;
    public int Four_LCS;
    public double TIME_LCS;
    public int ITER_LCS;
    public int ITER_SP; //the last search iterations of SP

    public int LTL_LCS;   //单独运输的订单数量
    public int MSTL2_LCS;
    public int MSTL3_LCS;
    public int MSTL4_LCS;
    public double COST_LCS;
    public double impr_LCS;
    public double gap_LCS; //相对于精确解的 gap
    public double bundle_LCS;

    public int delayOrder_LCS;
    public double delayTime_avg_LCS;
    public double delayTime_sd_LCS;

    //LCS allocation
    public double epsilon_LCS;
    public double MAXDEV_LCS;
    public double instb_LCS;
    public double SAVE_avg_LCS;
    public double SAVE_sd_LCS;

    //PR
    public  double epsilon_PR;
    public double MAXDEV_PR;
    public double instb_PR;
    public double SAVE_avg_PR;
    public double SAVE_sd_PR;

    //DR
    public double epsilon_DR;
    public double MAXDEV_DR;
    public double instb_DR;
    public double SAVE_avg_DR;
    public double SAVE_sd_DR;

    public HashMap<Integer,Route> routesEnmu;   //精确求解最优路径结果
    public HashMap<Integer,Double> subtEnum;   // 精确求解路径结果对应的订单超时情况
    public ArrayList<Route> LTL_Routes;    // 记录枚举的所有两个订单的路径
    public ArrayList<Route> TWO_Routes;    // 记录枚举的所有两个订单的路径
    public ArrayList<Route> THREE_Routes;  // 记录枚举的所有三个订单的路径
    public ArrayList<Route> FOUR_Routes;   // 记录枚举的所有4个订单的路径

    public HashMap<Integer,Route> routesLCS; // 启发求解最优路径结果
    public HashMap<Integer,Double> subtLCS;
    public HashSet<Route> LCS_Routes;
    public HashMap<int[], Double> constrLCS;

    public Double[] allocateEnum; // 精确求解成本分配结果
    public Double[] allocateLCS; // 启发式求解成本分配结果
    public Double[] allocatePR;
    public Double[] allocateDR;

    public Instance(String customerfile) {
        this.customerfile = customerfile;
        String[] s = customerfile.split("\\.");
        this.inst_name = s[0];
    }

    public void initial_data() throws ParseException {
        long t0 = System.currentTimeMillis();

        //Read order data
        load_orders(customerfile); // 订单数据读取
        //Read road network data
        load_distance(RoutesFile);  //初始化距离矩阵和费率矩阵

        //Check scale of the instance
        large = (NUM >= 1000);
        double tt = (System.currentTimeMillis() - t0)/1000.0/60.0;
        System.out.printf("Order data of %s is loaded with time %.2f min: %d orders, %d nodes, %d arcs %n",
                inst_name, tt, NUM, numV, numE);

        //LTL costs
        SignleCost = new Double[NUM + 1];
        OriginCost = 0;
        for (Customer cus : AllCustomers.values()) {
            if (cus.id >= 1 && cus.id <= NUM) {
                SignleCost[cus.id] = cus.singleCost;
                OriginCost += cus.singleCost;
            }
        }

        //记录参数初始化
        ParamsInitial();
        if (!check_data()) {
            System.out.println("########### Error in reading data! ###########");
            System.exit(0);
        }
    }

    //Read order data: node set customers include both picks and delis
    public void load_orders(String orderfile) throws ParseException {
        AllCustomers = new HashMap<>();
        String origin, destination; //起终点代码
        int origin_id, destination_id; //对应节点编号
        double weight, serve;
        double early, last, singleCost, maxDelay, lateCost;

        try{
            File file = new File(AlgoParam.dataPath+"/" + orderfile);
            FileInputStream in = new FileInputStream(file);
            InputStreamReader fr = new InputStreamReader(in, "gbk");
            BufferedReader br = new BufferedReader(fr);
            String line ;
            br.readLine();
            line = br.readLine();

            //parameters: start, return, num, LIFO, B, time, rad, stop
            String[] params = line.split(",");
            StartDepot = params[0].replace("\"", "");
            returnDepot = params[1].replace("\"", "");
            NUM = Integer.parseInt(params[2]);
            LIFO = (Integer.parseInt(params[3]) > 0);
            maxOrders = Integer.parseInt(params[4]);
            timelimit = Double.parseDouble(params[5]);
            radius = Double.parseDouble(params[6]);
            stop = Double.parseDouble(params[7]);

            //Add depot node 0
            AllCustomers.put(0,new Customer(0,StartDepot,0,0.0,0.0,0,0,0,0));
            //Add depot node 2N+1
            AllCustomers.put(2*NUM+1, new Customer(2*NUM+1,returnDepot, 0, 0.0, 0.0,
                    0,0,0,0));                       

            br.readLine();
            br.readLine();
            
            int node_id = 1;
            while ((line = br.readLine())!=null){
                String[] items = line.split(",");
                origin = items[1].replace("\"","");
                origin_id = node_id;

                destination = items[3].replace("\"","");
                destination_id = node_id + NUM;
                node_id ++;
                weight = Double.parseDouble(items[7]);

                //loading and unloading service time: LUT = 30 min
                if (weight <= 10) {
                    serve = LUT;
                } else if (weight <= 15) {
                    serve = 1.5*LUT;
                } else {
                    serve = 2*LUT;
                }

                early = (double)(items[8].charAt(items[8].length()-1))-'0'+ Double.parseDouble(items[9].split(":")[0]);
                last = (double)(items[10].charAt(items[10].length()-1))-'0'+ Double.parseDouble(items[11].split(":")[0]);
                singleCost = Double.parseDouble(items[12])*Level;
                maxDelay = Double.parseDouble(items[13]);
                lateCost = Double.parseDouble(items[14]);
                Customer node_pick = new Customer(origin_id,origin,weight,early,last,serve,singleCost,0,0); // 严格时间窗口
                Customer node_deli = new Customer(destination_id,destination,0,early,last,serve,singleCost,maxDelay,lateCost);
                AllCustomers.put(origin_id, node_pick);
                AllCustomers.put(destination_id, node_deli);
            }
            
            //end reading
            br.close();
            fr.close();
            in.close();
        }catch (Exception e) {
            e.printStackTrace();
        }

        System.out.printf("Complete reading order data of %s: %d orders %n",
                inst_name, NUM);
    }

    // 读取routes.csv文件,生成当前距离矩阵
    public void load_distance(String routefile){
        numV = AllCustomers.size();
        numE = NUM;
        HashMap<String[],Travel> roadMmap = new HashMap<>();
        String origin,destination;
        double distance,duration;

        try{
            File file = new File(routefile);
            FileInputStream in = new FileInputStream(file);
            InputStreamReader fr = new InputStreamReader(in, "utf-8");
            BufferedReader br = new BufferedReader(fr);
            String line;
            br.readLine();
            Graph = new Travel[2*NUM+2][2*NUM+2];
            Rate = new double[2*NUM+2][2*NUM+2];

            while ((line = br.readLine())!=null){
                String[] items = line.split(",");
                origin = items[0].replace("\"","");
                destination = items[1].replace("\"","");
                distance = Double.parseDouble(items[2]);
                duration = Double.parseDouble(items[3]);
                String[] location = new String[]{origin,destination};
                Travel dis = new Travel(location,distance,duration);
                roadMmap.put(location,dis);
            }

            for (Customer node1:AllCustomers.values()) {
                String location1 = node1.POI;
                for (Customer node2:AllCustomers.values()) {
                    String location2 = node2.POI;
                    if (location1.equals(location2)) {
                        Graph[node1.id][node2.id] = new Travel(new String[]{location1,location2},0,0);
                        Rate[node1.id][node2.id] = 0;
                    } else {
                        for (String[] key: roadMmap.keySet()) {
                            if (key[0].equals(location1) && key[1].equals(location2)) {
                                Graph[node1.id][node2.id] = roadMmap.get(key);

                                //Set the line-haul rate per km
                                if (Graph[node1.id][node2.id].distance <= FTL) {
                                    Rate[node1.id][node2.id] = RPM;
                                } else {
                                    Rate[node1.id][node2.id] = 0.9*RPM;
                                }

                                //Check the arcs
                                boolean BE = (node1.id <= NUM & node2.id <= NUM) || (node1.id > NUM & node2.id > NUM); //All picks or delis
                                if (BE && Graph[node1.id][node2.id].distance <= radius) {
                                    numE ++;
                                }
                                if (node1.id <= NUM && node2.id > NUM) {
                                    numE ++;
                                }

                                break;
                            }
                        }
                    }
                }
            }
            br.close();
            fr.close();
            in.close();
        }catch (Exception e) {
            e.printStackTrace();
        }
        //End
        System.out.printf("Complete reading shipping network data: %d nodes, %d arcs %n",
                numV, numE);
    }

    public boolean check_data() {
        boolean flag1 = (NUM > 0 && maxOrders > 0 && radius > 0 && timelimit > 0);
        boolean flag2 = (AllCustomers.size() == 2*NUM + 2);
        return flag1 && flag2;
    }

    // 结果记录部分参数初始化
    public void ParamsInitial() {
        if (!large) {  // 小规模实例
            EnumMethod = true;
            LCMethod = true;
            PRMethod = true;
            DRMethod = true;

        } else { // 大规模实例
            EnumMethod = false;
            LCMethod = true;
            PRMethod = true;
            DRMethod = true;
        }

        LTL_Routes = new ArrayList<>();
        TWO_Routes = new ArrayList<>();
        THREE_Routes = new ArrayList<>();
        FOUR_Routes = new ArrayList<>();

        allocateEnum = new Double[NUM+1];
        allocateLCS = new Double[NUM+1];
        allocatePR = new Double[NUM+1];
        allocateDR = new Double[NUM+1];

        //Enumeration algorithm
        NUM_enum = 0;
        Two_enum = 0;
        Three_enum = 0;
        Four_enum = 0;
        TIME_enum = 0;
        TIME_exact = 0;

        LTL_enum = 0;
        MSTL2_enum = 0;
        MSTL3_enum = 0;
        MSTL4_enum = 0;
        COST_enum = 0;
        impr_enum = 0;
        bundle_enum = 0;

        delayOrder_enum = 0; // 基于穷举的精确解中交付延迟的订单数量
        delayTime_avg_enum = 0; //基于穷举的精确解中，对于发生交付延迟的那些订单，计算延迟时间的平均值
        delayTime_sd_enum = 0;   // 基于穷举的精确解中，对于发生交付延迟的那些订单，计算延迟时间的标准差

        epsilon_exact = 0;
        MAXDEV_exact = 0;
        instb_exact = 0;
        SAVE_avg_exact = 0; // 每个参与者平均节省
        SAVE_sd_exact = 0;

        //LCS algorithm
        NUM_LCS = 0;
        Two_LCS = 0;
        Three_LCS = 0;
        Four_LCS = 0;
        TIME_LCS = 0;
        ITER_LCS = 0;
        ITER_SP = 0; //the last search iterations of SP

        LTL_LCS = 0;   //单独运输的订单数量
        MSTL2_LCS = 0;
        MSTL3_LCS = 0;
        MSTL4_LCS = 0;
        COST_LCS = 0;
        impr_LCS = 0;
        gap_LCS = 0;     //相对于精确解的 gap
        bundle_LCS = 0;
        delayOrder_LCS = 0;
        delayTime_avg_LCS = 0;
        delayTime_sd_LCS = 0;

        epsilon_LCS = 0;
        MAXDEV_LCS = 0;
        instb_LCS = 0;
        SAVE_avg_LCS = 0;
        SAVE_sd_LCS = 0;

        epsilon_PR = 0;
        MAXDEV_PR = 0;
        instb_PR = 0;
        SAVE_avg_PR = 0; //
        SAVE_sd_PR = 0;

        epsilon_DR = 0;
        MAXDEV_DR = 0;
        instb_DR = 0;
        SAVE_avg_DR = 0; // 每个参与者平均节省
        SAVE_sd_DR = 0;
    }

    public String makeCsvItem() {
        String str;
        str = inst_name + "," + NUM + "," + numV + ","  + numE + ","
                + LIFO + "," + maxOrders + "," + timelimit + "," + radius + "," + stop + ","
                + OriginCost + "," + NUM_enum + "," + Two_enum + "," + Three_enum + "," + Four_enum + ","
                + TIME_enum + "," + TIME_exact + ","
                + LTL_enum + "," + MSTL2_enum + "," + MSTL3_enum + "," + MSTL4_enum + ","
                + COST_enum + "," + impr_enum + "," + bundle_enum + ","
                + delayOrder_enum + "," + delayTime_avg_enum + "," + delayTime_sd_enum + ","
                + epsilon_exact + "," + MAXDEV_exact + "," + instb_exact + "," + SAVE_avg_exact + "," + SAVE_sd_exact + ","
                + NUM_LCS + "," + Two_LCS + "," + Three_LCS + "," + Four_LCS + ","
                + TIME_LCS + "," + ITER_LCS + "," + ITER_SP + ","
                + LTL_LCS + "," + MSTL2_LCS + "," + MSTL3_LCS + "," + MSTL4_LCS + ","
                + COST_LCS + "," + impr_LCS + "," + gap_LCS + "," + bundle_LCS + ","
                + delayOrder_LCS + "," + delayTime_avg_LCS + "," + delayTime_sd_LCS + ","
                + epsilon_LCS + "," + MAXDEV_LCS + "," + instb_LCS + "," + SAVE_avg_LCS + "," + SAVE_sd_LCS + ","
                + epsilon_PR + "," + MAXDEV_PR + "," + instb_PR + "," + SAVE_avg_PR + "," + SAVE_sd_PR + ","
                + epsilon_DR + "," + MAXDEV_DR + "," + instb_DR + "," + SAVE_avg_DR + "," + SAVE_sd_DR;
        return str;
    }

    // 路径和成本分配结果写入json文件
    public String resultToString(){
        StringBuilder sb = new StringBuilder();

        double cost = COST_LCS;
        HashMap<Integer,Route> routes = routesLCS;
        HashMap<Integer,Double> subt = subtLCS;
        double epsilon = epsilon_LCS;
        Double[] allocate = allocateLCS;

        // results of ILS
        sb.append("\"COST\": \r\n");
        sb.append(cost + "\r\n");
        for(int i:routes.keySet()){
            sb.append(i + ":" + Arrays.toString(routes.get(i).R.toArray()) + "\r\n");
        }
        sb.append("\"Subt_ILS\":\r\n");
        for(int c:subt.keySet()){
            sb.append(c + ":" + subt.get(c) + ",");
        }
        sb.append("\r\n");

        //  results of LCS allocation
        sb.append("\"epsilon\":\r\n");
        sb.append(epsilon + "\r\n");
        for (int i = 1; i < allocate.length; i++) {
            sb.append(i + ":" + allocate[i] + "\r\n");
        }

        return sb.toString();
    }


    //Check feasibility conditions of a route
    public boolean bundle_feasible(Route route){
        int num = route.R.size();

        //LTL route
        if (num <= 4) {
            return true;
        }

        //MSTL route: bundling and time constraints
        if (num > 2*maxOrders + 2) { //bundling size limit
            //int b = (n-2)/2;
            //System.err.printf("###### Error route in bundling size: %d, maxOrder %d ###### %n", b, maxOrders);
            return false;
        }

        for (int i = 1; i <= num/2 - 2; i++) { //bundling radius constraints for pickup
            if (Graph[route.R.get(i)][route.R.get(i + 1)].distance > radius) {
                //double dis = Graph[route.R.get(i)][route.R.get(i + 1)].distance;
                //System.err.printf("###### Error route in pickup bundling radius: %.2f, %.2f ###### %n", dis, radius);
                return false;
            }
        }
        for (int i = num/2; i <= num - 3; i++) { //bundling radius constraints for delivery
            if (Graph[route.R.get(i)][route.R.get(i + 1)].distance > radius) {
                //double dis = Graph[route.R.get(i)][route.R.get(i + 1)].distance;
                //System.err.printf("###### Error route in delivery bundling radius: %.2f, %.2f ###### %n", dis, radius);
                return false;
            }
        }

        //If all feasible
        return true;
    }

    /* Set check = false if it is the first time to calculate the new route
         Set check = true if it is the second time to calculate the given route
         The cost and time are updated while calling for this function
       Feasibility conditions:
                           capacity limit;
                           pickup/delivery radius limit;
                           time-window limits;
                           service duration limits;
    */
    public boolean feasible(Route route,boolean check) {
        //Initial values
        double load = 0, dis = 0, time = 0, subt = 0, cost = 0;

        //Empty routes
        if(route.R.size() <= 2) {
            route.load = load;
            route.dis = dis;
            route.time = time;
            route.subt = subt;
            route.cost = cost;
            return true;
        }

        // 更新路径装载
        for (int i = 1; i < route.R.size()/2; i++){
            load = load + AllCustomers.get(route.R.get(i)).demand;
        }
        if (load > capacity){
            if (check)
                System.err.printf("###### Error route in load capacity: %.2f, %.2f ###### %n",
                    load, capacity);
            return false;
        }

        // 时间窗和最大运行时长约束
        if (route.R.size() == 4) { //LTL route
            cost = SignleCost[route.R.get(1)];
            for(int i = 1; i < route.R.size(); i++){
                int before = route.R.get(i-1);
                int current = route.R.get(i);
                time += Graph[before][current].duration + AllCustomers.get(before).serve/60;
            }
        }

        if (route.R.size() > 4) {
            //The MSTL route
            cost = Graph[route.R.get(1)][route.R.get(route.R.size()-2)].distance * Rate[route.R.get(1)][route.R.get(route.R.size() - 2)]
                    + (route.R.size() - 4) * stop;

            route.setStartTime(AllCustomers.get(route.R.get(1)).early - Graph[0][AllCustomers.get(route.R.get(1)).id].duration);  //路径的出发时间自动定位为第一个订单的出发时间
            double arrive_time = route.startTime; //unit: hour
            //到达顾客i的时间
            for (int i = 1; i < route.R.size() / 2; i++) {
                int before = route.R.get(i - 1), current = route.R.get(i);

                //check pickup radius
                if (before > 0 && current <= NUM && Graph[before][current].distance > radius) {
                    if (check)
                        System.err.printf("###### Error route in pickup bundling radius: %.2f, %.2f ###### %n",
                                Graph[before][current].distance, radius);
                    return false;
                }

                arrive_time = arrive_time + Graph[before][current].duration + AllCustomers.get(before).serve / 60;
                Customer customer = AllCustomers.get(current);
                if (arrive_time <= customer.early) {
                    arrive_time = customer.early;
                }
            }

            for (int i = route.R.size() / 2; i < route.R.size() - 1; i++) {
                int before = route.R.get(i - 1), current = route.R.get(i);

                //check delivery radius
                if (before > NUM && current <= 2*NUM && Graph[before][current].distance > radius) {
                    if (check)
                        System.err.printf("###### Error route in delivery bundling radius: %.2f, %.2f ###### %n",
                                Graph[before][current].distance, radius);
                    return false;
                }

                arrive_time = arrive_time + Graph[before][current].duration + AllCustomers.get(before).serve / 60;
                Customer deli = AllCustomers.get(current);
                Customer pick = AllCustomers.get(current - NUM);
                if (arrive_time <= deli.last + deli.maxDelay) {
                    double delay = Math.max(0, arrive_time - deli.last);
                    cost += deli.lateCost * pick.demand * delay;
                    subt += delay;
                } else {
                    cost = Double.MAX_VALUE;
                    subt = Double.MAX_VALUE;
                    if (check)
                        System.out.printf("####### Infeasible route violate delivery time of order %d: ! ####### %n",
                                deli.id);
                    return false;
                }
            }

            arrive_time = arrive_time + Graph[route.R.get(route.R.size() - 2)][route.R.get(route.R.size() - 1)].duration
                    + AllCustomers.get(route.R.get(route.R.size() - 2)).serve / 60;
            time = arrive_time - route.startTime;
            if (time > timelimit) { //arrive_time > timeEnd ||
                if (check)
                    System.out.printf("####### Infeasible route violate hours of service limit %.2f: %.2f! #######%n",
                        timelimit, time);
                return false;
            }
        }

        //set the cost and time of route
        route.dis = dis;
        route.cost = cost;
        route.time = time;
        route.subt = subt;
        route.load = load;
        return true;
    }


    public HashMap<Integer,Double> generateSubt(HashMap<Integer, Route> Routes){
        HashMap<Integer,Double> subt = new HashMap<>();
        // delay生成
        for (Route route:Routes.values()) {
            // LTL shipping case
            if (route.R.size() == 4) {
                int i = route.R.size()/2;
                Customer c = AllCustomers.get(route.R.get(i));
                double delay = 0;
                subt.put(c.id,delay);
            }

            // MSTL shipping case
            double arrive_time = 0;
            for (int i = 1; i < route.R.size()/2; i++) {
                Customer cus = AllCustomers.get(route.R.get(i));
                arrive_time = arrive_time + Graph[route.R.get(i-1)][route.R.get(i)].duration +
                        AllCustomers.get(route.R.get(i-1)).serve/60;
                if (arrive_time < cus.early) {
                    arrive_time = cus.early;
                }
            }

            for (int i = route.R.size()/2; i < route.R.size()-1; i++) {
                Customer cus = AllCustomers.get(route.R.get(i));
                double delay = 0;
                arrive_time = arrive_time + Graph[route.R.get(i-1)][route.R.get(i)].duration +
                        AllCustomers.get(route.R.get(i-1)).serve/60;
                if (arrive_time < cus.early) {
                    arrive_time = cus.early;
                }
                if (arrive_time > cus.last) {
                    delay = arrive_time - cus.last;
                }
                subt.put(cus.id, delay);
            }
        }
        return subt;
    }

    public void CheckRoutes(HashMap<Integer,Route> Routes, String sol){
        //所有顾客都被访问且只被访问一次
        HashSet<Integer> customers = new HashSet<>();
        for(Route route:Routes.values()){
            if (!feasible(route,false)) {
                System.err.printf("####### Warning: Cost calculation error in Route ID. %d of solution %s!####### %n",
                        route.id, sol);
            }
            for (int i = 1; i < route.R.size()/2; i++) {
                customers.add(route.R.get(i));
            }
        }

        if (customers.size() != NUM) {
            int n = NUM - customers.size();
            System.err.printf("####### CheckRoutes Warning: %d orders are NOT served by solution %s!####### %n",
                    n, sol);
        }
    }


    public void SolutionModify(HashMap<Integer, Route> Routes, String sol){
        //结果修正
        HashSet<Integer> customers = new HashSet<>();
        for (Route route:Routes.values()) {
            for (int i = 1; i < route.R.size()/2; i++) {
                customers.add(route.R.get(i));
            }
        }

        int dummy_id = -1;
        for (int i = 1; i <= NUM; i++) {
            if (!customers.contains(i)) {
                System.out.printf("###### Repair %s solution for missed order %d ###### %n",
                        sol, i);
                Route route = new Route();
                route.id = dummy_id;
                route.R.add(0);
                route.R.add(i);
                route.R.add(i+NUM);
                route.R.add(2*NUM+1);
                route.cost = SignleCost[i];
                if (feasible(route,false)) {
                    Routes.put(route.id, route);
                    dummy_id--;
                }
            }
        }
    }


    public double Calculation (HashMap<Integer,Route> Routes) {
        double totalCost = 0;
        for(Route route: Routes.values()){
            totalCost += route.cost;
        }
        return totalCost;
    }


    public double[] MaxDEV(Double[] assignment,HashMap<int[],Double> Constraints){
        int n = Constraints.size(), k = 0;
        double[] Eps = new double[n];
        double[] Dev = new double[n];
        double[] stb = new double[3]; //eps, dev, instb

        for(int[] coalition:Constraints.keySet()){
            double cost_assign = 0;
            int size = 0;
            for (int i = 1; i <= NUM; i++) {
                if (coalition[i] == 0) {
                    continue;
                }
                cost_assign = cost_assign + assignment[i] * coalition[i];
                size = size + coalition[i];
            }

            double eps = 0, dev = 0;
            if (Math.abs(Constraints.get(coalition)) > 1e-6) {
                eps = cost_assign - Constraints.get(coalition);
                dev = 100 * eps / Constraints.get(coalition);
            }

            //Check validity
            /*if(dev > 50) {
                System.out.printf("Large unstable coalition: (size %d, cost %.4f, eps %.4f) %n",
                        size, Constraints.get(coalition), eps);
            }*/

            Eps[k] = eps;
            Dev[k] = dev;
            k++;
        }

        //analyse the routes stability
        Arrays.sort(Eps);
        Arrays.sort(Dev); // max_deviation = Dev[n-1];
        stb[0] = Eps[n-1];
        stb[1] = Dev[n-1];

        int s = 0;
        for(int i = 0; i < n; i++){
            if (Eps[i] > 1e-6) {
                s += 1;
            }
        }
        stb[2] = 100.0*s/n;

        System.out.printf("*** Max to min relative deviation among routes: (%.4f%%, %.4f%%, %.4f%%, %.4f%%) *** %n",
                Dev[n-1], Dev[n-2], Dev[1], Dev[0]);
        return stb;
    }


}
