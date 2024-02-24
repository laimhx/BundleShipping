package Common;

import java.util.ArrayList;
import java.util.Objects;

public class Route  {
    public int id;
    public double startTime;       // 路径出发时间
    public ArrayList<Integer> R;  //路径的节点容器
    public double dis;  //长度
    public double subt;    //路径超时
    public double time;    //车辆运行时间
    public double load;   //装载货物量
    public double cost;
    public double gamma;   // 路径对应的SP问题目标值

    public Route() {
        this.R = new ArrayList<>();
    }


    public Route cloneRoute( ){
        Route clone = new Route();
        clone.id = this.id;
        clone.dis = this.dis;
        clone.subt = this.subt;
        clone.time =this. time;
        clone.load = this.load;
        clone.cost = this.cost;
        clone.gamma = this.gamma;
        clone.startTime = this.startTime;
        clone.R = new ArrayList<>(this.R);
        return clone;
    }

    public double getStartTime() {
        return startTime;
    }

    public void setStartTime(double startTime) {
        this.startTime = startTime;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public ArrayList<Integer> getR() {
        return R;
    }

    public void setR(ArrayList<Integer> r) {
        R = r;
    }

    public double getDis() {
        return dis;
    }

    public void setDis(double dis) {
        this.dis = dis;
    }

    public double getSubt() {
        return subt;
    }

    public void setSubt(double subt) {
        this.subt = subt;
    }

    public double getTime() {
        return time;
    }

    public void setTime(double time) {
        this.time = time;
    }

    public double getLoad() {
        return load;
    }

    public void setLoad(double load) {
        this.load = load;
    }

    public double getCost() {
        return cost;
    }

    public void setCost(long cost) {
        this.cost = cost;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Route route = (Route) o;
        return Objects.equals(R, route.R);
    }

    @Override
    public int hashCode() {
        return Objects.hash(R);
    }
}
