package AlgoRun;

public class AlgoParam {
    public static String dataPath;  //实验数据路径
    public static String RoutesFile;  //路网数据文件名
    public static String ProvinceFile;  //省份读取文件
    public static String Exper;

    public static boolean display;
    public static double capacity;  //车辆容量限制

    public static double Level;  //LTL rate level
    public static double FTL;  //TL distance break
    public static double RPM; //TL unit rate per mile
    public static double LUT; //base loading/unloading service time

    public static double LCSTIME;  //LCS running time limit
    public static int SPMAXR;   //LCS subproblem: maximum number of negative-cost routes to generate
    public static int SPMAXN;   //LCS subproblem: maximum number of nonimprovement iteration
    public static double SPTIME; //LCS subproblem: maximum running time limit

    public static String ResultPath;     //文件输出路径
    public static String InstanceResultPath;   //类型实例对应的输出路径
    public static String solPath;        //每个实例对应的输出路径
    public static String csvPath;        //记录所有实例结果的CSV文件输出路径
}

