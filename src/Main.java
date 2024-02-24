import AlgoRun.AlgoRunner;
import java.text.ParseException;

public class Main {
    public static void main(String[] args) throws ParseException {
        long startTime = System.currentTimeMillis();
        new AlgoRunner().run();
        double time = (System.currentTimeMillis()-startTime)/1000.0/3600.0;
        System.out.printf("All instances are completed with: %.2f hours", time);
    }
}
