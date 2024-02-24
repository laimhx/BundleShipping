package AlgoRun;

import Common.*;
import Enumeration.*;
import RouteGenerate.*;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;

public class Algorithms {
    public Instance instance;  // 求解实例

    public Algorithms(Instance instance) {
        this.instance = instance;
    }

    public void run() throws ParseException {
        System.out.printf("******** Start test %s: %d orders, %.2f hr, %.2fkm radius, %.2f stop ********%n",
                instance.inst_name, instance.NUM, instance.timelimit, instance.radius, instance.stop);

        //run LCS algorithms
        solveLCS();

        //run enumeration algorithms
        solveExact();
        assignEnum();
        if (!instance.large) {
            instance.gap_LCS = 100*(instance.COST_LCS - instance.COST_enum)/instance.COST_enum;
        }

        //run basic allocations
        assignBasic();

        // Summary results
        double ratio = 100.0 * instance.NUM_LCS/instance.NUM_enum;
        System.out.printf("Optimization summary: LTL %.4f, impr_exact %.4f%%, impr_LCS %.4f%%, gap_LCS %.4f%% %n",
                instance.OriginCost, instance.impr_enum, instance.impr_LCS, instance.gap_LCS);
        System.out.printf("Route generation summary: ratio %.4f%%, bundle %.4f%%, %d 2-routes, %d 3-routes, %d 4-routes -------%n",
                ratio, instance.bundle_LCS, instance.Two_LCS, instance.Three_LCS, instance.Four_LCS);
        System.out.printf("Allocation eps summary: eps_exact %.2f, eps_LCS %.2f, eps_PR %.2f, eps_DR %.2f %n",
                instance.epsilon_exact, instance.epsilon_LCS, instance.epsilon_PR, instance.epsilon_DR);
        System.out.printf("Allocation dev summary: dev_exact %.4f%%, dev_LCS %.4f%%, dev_PR %.4f%%, dev_DR %.4f%% %n",
                instance.MAXDEV_exact, instance.MAXDEV_LCS, instance.MAXDEV_PR, instance.MAXDEV_DR);
        System.out.printf("Complexity summary: enum %.2f min, LCS %d iter, LCS %.2f min %n",
                instance.TIME_enum, instance.ITER_LCS, instance.TIME_LCS);
        System.out.printf("******** Test %s completed: %d orders, %.2f hr, %.2fkm radius, %.2f stop ********%n",
                instance.inst_name, instance.NUM, instance.timelimit, instance.radius, instance.stop);
    }

    // Exact algorithm by enumeration
    public void solveExact() throws ParseException {
        if (!instance.EnumMethod)
            return;

        System.out.println("------------------- Start enumeration algorithm ------------------");
        RouteEnum enmu = new RouteEnum(instance);
        enmu.run();

        // Check route use
        instance.LTL_enum = 0;
        instance.MSTL2_enum = 0;
        instance.MSTL3_enum = 0;
        instance.MSTL4_enum = 0;
        for (Route route:instance.routesEnmu.values()) {
            if (route.R.size() <= 4) {
                instance.LTL_enum ++;
            } else if (route.R.size() <= 6) {
                instance.MSTL2_enum ++;
            } else if (route.R.size() <= 8) {
                instance.MSTL3_enum ++;
            } else {
                instance.MSTL4_enum ++;
            }
        }

        //Route costs
        instance.impr_enum = 100*(instance.OriginCost - instance.COST_enum)/instance.OriginCost;
        instance.bundle_enum = 100.0 * (instance.NUM - instance.LTL_enum)/instance.NUM;

        // Check the delay of orders
        int delayOrder_enum = 0;
        double delayTime_avg_enum = 0;
        double[] delays = new double[instance.NUM+1];
        for (int i = 1; i <= instance.NUM; i++) {
            double delay = instance.subtEnum.get(i+instance.NUM);
            delays[i] = delay;
            delayTime_avg_enum += delay;
            if (delay > 0) {
                delayOrder_enum++;
            }
        }

        instance.delayOrder_enum = delayOrder_enum;
        if (delayOrder_enum > 0) {
            delayTime_avg_enum = delayTime_avg_enum / delayOrder_enum;
        }
        instance.delayTime_avg_enum = delayTime_avg_enum;

        double delayTime_sd_enum = 0;
        for (int i = 1; i <= instance.NUM; i++) {
            if (delays[i] > 0) {
                delayTime_sd_enum += Math.pow((delays[i] - delayTime_avg_enum),2);
            }
        }
        if (delayOrder_enum > 1) {
            delayTime_sd_enum = delayTime_sd_enum / (delayOrder_enum - 1); //this is not standard deviation
            delayTime_sd_enum = Math.sqrt(delayTime_sd_enum);
        }
        instance.delayTime_sd_enum = delayTime_sd_enum;

        // Results of enumeration algorithm
        System.out.printf("----------Enumeration results: LTL %.2f, COST_exact %.2f, impr %.4f%%, bundling %.4f%% ----------%n",
                instance.OriginCost, instance.COST_enum, instance.impr_enum, instance.bundle_enum);
        System.out.printf("The delay of orders: delayOrder %d, delayTime_avg %.2f, delayTime_sd %.2f %n",
                delayOrder_enum, delayTime_avg_enum, delayTime_sd_enum);
        System.out.printf("Routes in enumeration: (1-,2-,3-,4-) = (%d, %d, %d, %d) %n",
                instance.LTL_enum, instance.MSTL2_enum, instance.MSTL3_enum, instance.MSTL4_enum);
    }

    //Cost allocation algorithm enumeration
    public void assignEnum() throws ParseException {
        if(!instance.EnumMethod)
            return;
        System.out.println("----------------- Start solving cost allocation by enumeration! -----------------");
        EnmuAllocate exhaust = new EnmuAllocate(instance);
        exhaust.run();

        // Check individual savings
        double[] sv = ind_saving(instance.allocateEnum, instance.routesEnmu);
        instance.SAVE_avg_exact = sv[0];
        instance.SAVE_sd_exact = sv[1];

        // Results of enumerated allocation
        System.out.printf("--------Enumerated allocation results: epsilon %.4f, maxdev %.4f%%, instb %.4f%%, save %.4f%%--------%n",
                instance.epsilon_exact, instance.MAXDEV_exact, instance.instb_exact, instance.SAVE_avg_exact);
    }


    //Route joint searching algorithm
    public void solveLCS() throws ParseException {
        if(!instance.LCMethod)
            return;
        //Cost allocations
        System.out.println("----------------- Start LCS algorithms -----------------");
        RouteSearch LCS = new RouteSearch(instance);
        LCS.run();

        // Check route use
        instance.LTL_LCS = 0;
        instance.MSTL2_LCS = 0;
        instance.MSTL3_LCS = 0;
        instance.MSTL4_LCS = 0;
        for(Route route: instance.routesLCS.values()){
            if (route.R.size() <= 4) {
                instance.LTL_LCS ++;
            } else if (route.R.size() <= 6) {
                instance.MSTL2_LCS ++;
            } else if (route.R.size() <= 8) {
                instance.MSTL3_LCS ++;
            } else {
                instance.MSTL4_LCS ++;
            }
        }

        // Check the delay of orders
        int delayOrder_LCS = 0;
        double delayTime_avg_LCS = 0;
        double[] delays = new double[instance.NUM+1];
        for (int i = 1; i <= instance.NUM; i++) {
            double delay = instance.subtLCS.get(i+instance.NUM);
            delays[i] = delay;
            delayTime_avg_LCS += delay;
            if (delay > 0) {
                delayOrder_LCS++;
            }
        }
        instance.delayOrder_LCS = delayOrder_LCS;

        if (delayOrder_LCS > 0) {
            delayTime_avg_LCS = delayTime_avg_LCS / delayOrder_LCS;
        }
        instance.delayTime_avg_LCS = delayTime_avg_LCS;

        double delayTime_sd_LCS = 0;
        for (int i = 1; i <= instance.NUM; i++) {
            if (delays[i] > 0) {
                delayTime_sd_LCS += Math.pow((delays[i] - delayTime_avg_LCS),2);
            }
        }
        if (delayOrder_LCS > 1) {
            delayTime_sd_LCS = delayTime_sd_LCS/ (delayOrder_LCS - 1); //this is not standard deviation
            delayTime_sd_LCS = Math.sqrt(delayTime_sd_LCS);
        }
        instance.delayTime_sd_LCS = delayTime_sd_LCS;

        //Optimization results
        instance.impr_LCS = 100*(instance.OriginCost - instance.COST_LCS)/instance.OriginCost;
        instance.bundle_LCS = 100.0 * (instance.NUM - instance.LTL_LCS)/instance.NUM;

        System.out.printf("--------LCS optimization results: COST_LCS %.2f, impr %.4f%%, bundling %.4f%% --------%n",
                instance.COST_LCS, instance.impr_LCS, instance.bundle_LCS);
        System.out.printf("Routes in LCS solution: (1-,2-,3-,4-) = (%d, %d, %d, %d) %n",
                instance.LTL_LCS, instance.MSTL2_LCS, instance.MSTL3_LCS, instance.MSTL4_LCS);
        System.out.printf("The delay of orders: delayOrder %d, delayTime_avg %.2f, delayTime_sd %.2f %n",
                instance.delayOrder_LCS, instance.delayTime_avg_LCS, instance.delayTime_sd_LCS);

        //Allocation results
        double[] sv = ind_saving(instance.allocateLCS, instance.routesLCS);
        instance.SAVE_avg_LCS = sv[0];
        instance.SAVE_sd_LCS = sv[1];

        System.out.printf("-------- LCS allocation: epsilon %.4f, maxdev %.4f%%, instb %.4f%%, save %.4f%% --------%n",
                instance.epsilon_LCS, instance.MAXDEV_LCS, instance.instb_LCS, instance.SAVE_avg_LCS);
    }


    public void assignBasic() throws ParseException {
        System.out.println("--------------- Start basic cost allocation algorithms ---------------");
        BasicAllocate basic = new BasicAllocate(instance);

        // Proportional rule
        if(!instance.PRMethod)
            return;
        basic.Proportional_rule();
        double[] sv = ind_saving(instance.allocatePR, instance.routesLCS);
        instance.SAVE_avg_PR = sv[0];
        instance.SAVE_sd_PR = sv[1];
        System.out.printf("-------- PR allocation: epsilon %.4f, maxdev %.4f%%, instb %.4f%%, save %.4f%% --------%n",
                instance.epsilon_PR, instance.MAXDEV_PR, instance.instb_PR, instance.SAVE_avg_PR);

        // Dual rule
        if(!instance.DRMethod)
            return;
        basic.Dual_rule();
        sv = ind_saving(instance.allocateDR, instance.routesLCS);
        instance.SAVE_avg_DR = sv[0];
        instance.SAVE_sd_DR = sv[1];
        System.out.printf("-------- DR allocation: epsilon %.4f, maxdev %.4f%%, instb %.4f%%, save %.4f%% --------%n",
                instance.epsilon_DR, instance.MAXDEV_DR, instance.instb_DR, instance.SAVE_avg_DR);
    }

    public double[] ind_saving(Double[] assignment, HashMap<Integer,Route> Routes) {
        // Check individual savings
        double[] sv = new double[2]; //avg, std
        double SAVE_avg_exact = 0;
        double SAVE_sd_exact = 0;
        ArrayList<Double> save = new ArrayList< >();
        for(Route route: Routes.values()){
            if(route.R.size() > 4){
                for(int i = 1; i < route.R.size()/2; i++){
                    int c = route.R.get(i);
                    save.add(100* (instance.SignleCost[c] - assignment[c])/instance.SignleCost[c]);
                }
            }
        }

        for(double s:save){
            SAVE_avg_exact += s;
        }
        SAVE_avg_exact = SAVE_avg_exact / save.size();

        for(double s:save){
            SAVE_sd_exact += Math.pow((s - SAVE_avg_exact),2);
        }
        SAVE_sd_exact = SAVE_sd_exact / (save.size()-1); //this is not standard deviation
        SAVE_sd_exact = Math.sqrt(SAVE_sd_exact);

        sv[0] = SAVE_avg_exact;
        sv[1] = SAVE_sd_exact;
        return sv;
    }


}
