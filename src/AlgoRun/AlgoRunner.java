package AlgoRun;

import Common.Instance;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class AlgoRunner {
    public void run() throws ParseException {
        //tasks: ExperJS, ExperZJ, LargeJS, LargeZJ
        AlgoParam.Exper = "ExperJS";
        //LTL rate level: low = 1.0; high = 2.0
        AlgoParam.Level = 1.0;

        switch (AlgoParam.Exper) {
            case "ExperJS" -> {
                AlgoParam.dataPath = "./data/JS";
            }

            case "ExperZJ" -> {
                AlgoParam.dataPath = "./data/ZJ";
            }

            case "LargeJS" -> {
                AlgoParam.dataPath = "./data/LargeJS";
            }

            case "LargeZJ" -> {
                AlgoParam.dataPath = "./data/LargeZJ";
            }
        }

        //Set control parameters
        AlgoParam.capacity = 33;  // 车辆容量约束
        AlgoParam.FTL = 300; //TL distance break
        AlgoParam.RPM = 60;  //TL unit rate per mile
        AlgoParam.LUT = 30;  //base loading/unloading service time: minutes

        AlgoParam.LCSTIME = 60; //LCS maximum searching time limit
        AlgoParam.SPMAXR = 100;  //LCS subproblem: maximum negative-cost route to generate
        AlgoParam.SPMAXN = 25000; //LCS subproblem: maximum number of nonimprovement iteration
        AlgoParam.SPTIME = 5.0; //LCS subproblem: maximum running time limit
        AlgoParam.display = true;

        //inst_JS_1000_1.csv
        AlgoParam.RoutesFile = "./data/Routes.csv";
        AlgoParam.ProvinceFile = "./data/Centers.csv";
        AlgoParam.ResultPath = "./result";
        AlgoParam.InstanceResultPath = AlgoParam.ResultPath  + "/" + AlgoParam.dataPath.split("/")[2];
        AlgoParam.solPath = AlgoParam.InstanceResultPath + "/sol";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        AlgoParam.csvPath = AlgoParam.ResultPath + "/" + AlgoParam.Exper + "_" + sdf.format(new Date()) + ".csv";

        //Create directory for results
        makeResultFolders();
        writeCSV(makeCSVTitle(),true);

        //start experiment runs
        System.out.println("********* Get started with instance set: " + AlgoParam.Exper + " *********");
        Instance[] instances = readInsts();
        runAlgo(instances);
    }

    void runAlgo(Instance[] instances) throws ParseException {
        for (Instance inst : instances) {
            System.out.println("------------------------------------------------------");
            long startTime = System.currentTimeMillis();

            inst.initial_data();
            //inst.stop = 500;
            //inst.EnumMethod = false;
            Algorithms test = new Algorithms(inst);
            test.run();
            writeResult(inst);

            //record running time
            long endTime = System.currentTimeMillis();
            double tt = (endTime-startTime)/1000.0/60.0;
            System.out.printf("********** Test %s with orders %d is completed with time %.2f min **********%n",
                    inst.inst_name, inst.NUM, tt);
        }
    }

    // 文件夹下所有实例读取
    Instance[] readInsts(){
        File dir = new File(AlgoParam.dataPath);
        File[] files = dir.listFiles();
        assert files != null;
        Instance[] instances = new Instance[files.length];
        for(int i = 0; i < files.length; i++){
            instances[i] = new Instance(files[i].getName());
        }
        return instances;
    }

    public static void makeResultFolders() {
        File resultFolder = new File(AlgoParam.ResultPath);
        if (!resultFolder.exists() || !resultFolder.isDirectory())
            resultFolder.mkdirs();
        File algoFolder = new File(AlgoParam.InstanceResultPath);
        if (!algoFolder.exists() || !algoFolder.isDirectory())
            algoFolder.mkdir();
        File solFolder = new File(AlgoParam.solPath);
        if (solFolder.exists() || !solFolder.isDirectory())
            solFolder.mkdir();
    }

    String makeCSVTitle() {
        String str;
        str = "inst_name, NUM, numV, numE, " +
                "LIFO, maxOrders, timelimit, radius, stop, " +
                "OriginCost, NUM_enum, Two_enum, Three_enum, Four_enum, " +
                "TIME_enum, TIME_exact, " +
                "LTL_enum, MSTL2_enum, MSTL3_enum, MSTL4_enum, " +
                "COST_enum, impr_enum, bundle_enum, " +
                "delayOrder_enum, delayTime_avg_enum, delayTime_sd_enum, " +
                "epsilon_exact, MAXDEV_exact, instb_exact, SAVE_avg_exact, SAVE_sd_exact, " +
                "NUM_LCS, Two_LCS, Three_LCS, Four_LCS, " +
                "TIME_LCS, ITER_LCS, ITER_SP, " +
                "LTL_LCS, MSTL2_LCS, MSTL3_LCS, MSTL4_LCS, " +
                "COST_LCS, impr_LCS, gap_LCS, bundle_LCS, " +
                "delayOrder_LCS, delayTime_avg_LCS, delayTime_sd_LCS, " +
                "epsilon_LCS, MAXDEV_LCS, instb_LCS, SAVE_avg_LCS, SAVE_sd_LCS, " +
                "epsilon_PR, MAXDEV_PR, instb_PR, SAVE_avg_PR, SAVE_sd_PR, " +
                "epsilon_DR, MAXDEV_DR, instb_DR, SAVE_avg_DR, SAVE_sd_DR";
        return str;
    }

    public static void writeCSV(String text, boolean title) {
        File csvFile = new File(AlgoParam.csvPath);
        try {
            if (!csvFile.exists()) {
                csvFile.createNewFile();
                write(csvFile, text);
            } else {
                if (!title) append(csvFile, text);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void write(File file, String text) {
        try {
            FileWriter fw = new FileWriter(file);
            fw.write(text + "\r\n");
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void append(File file, String text) {
        try {
            FileWriter fw = new FileWriter(file, true);
            fw.write(text + "\r\n");
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void writeResult(Instance instance){
        writeCSV(instance.makeCsvItem(), false);
        String solPath = AlgoParam.solPath + "/" + instance.inst_name + ".json";
        File solFile = new File(solPath);
        write(solFile, instance.resultToString());
    }


}
