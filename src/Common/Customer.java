package Common;

import java.util.Objects;

public class Customer {
    public int id;   //节点编号  pickup点：1=<c<=n
    public String POI; // pick点 or deli点编码
    public double early;    //最早可到达时间
    public double last;      //最晚可到达时间
    public double serve;    //服务时长
    public double demand;   //需求
    public double maxDelay;      // 最大允许配送延迟
    public double lateCost;     //  单位延迟成本

    public int route;   //订单被分配到的路径
    public double singleCost;    // 单独配送成本

    public Customer( ) {
    }

    public Customer(int id,String POI,double demand,Double early,Double last,double serve,double singleCost,double maxDelay,double lateCost) {
        this.id = id;
        this.POI = POI;
        this.demand = demand;
        this.early = early;
        this.last = last;
        this.serve = serve;
        this.singleCost = singleCost;
        this.maxDelay = maxDelay;
        this.lateCost = lateCost;
    }

    public Customer clonecustomer( ){
        Customer clone = new Customer();
        clone.id = this.id;
        clone.POI = this.POI;
        clone.early = this.early;
        clone.last = this.last;
        clone.serve = this.serve;
        clone.demand = this.demand;
        clone.singleCost = this.singleCost;
        clone.maxDelay = this.maxDelay;
        clone.lateCost = this.lateCost;
        clone.route = this.route;
        return clone;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Customer customer = (Customer) o;
        return id == customer.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
