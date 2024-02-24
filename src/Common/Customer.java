package Common;

import java.util.Objects;

public class Customer {
    public int id;   //�ڵ���  pickup�㣺1=<c<=n
    public String POI; // pick�� or deli�����
    public double early;    //����ɵ���ʱ��
    public double last;      //����ɵ���ʱ��
    public double serve;    //����ʱ��
    public double demand;   //����
    public double maxDelay;      // ������������ӳ�
    public double lateCost;     //  ��λ�ӳٳɱ�

    public int route;   //���������䵽��·��
    public double singleCost;    // �������ͳɱ�

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
