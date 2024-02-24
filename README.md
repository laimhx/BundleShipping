#### I. Java program running method
This is the Java program for solving bundle shipping problems. 
The main file is Main.java. Run this file would generate the results for a given set of data instances.
Specify the data set in file AlgoRunner.java.

#### II. Data description

##### 1. Vehicle and cost
**(1) capacity: 33 pallets**

1 pallet = 1,200 lbs = 544 kg
20000 lbs = 17 pallets

**(2)  The TL rate per km **
$$
\begin{equation*}
 r_{ij} = 
 \left\{
 \begin{array}{ll}
 50, & \hbox{if $d_{ij}<=250$ km;} \\
 40, & \hbox{if $d_{ij}>250$ km.}
 \end{array}
 \right.
\end{equation*}
$$
其中 $d_{ij}$ 是路线驾驶距离，单位: km.。

**(3) Stop-off charges**
$f=$ 500 CNY/stop。

**(4) load/unload service time**
每个订单的装货/卸货时间：
$$
\begin{equation*}
 serve(w) = 
 \left\{
 \begin{array}{ll}
 45 \; min, & \hbox{if $w \leq 10$ pallets;} \\
 60 \; min, & \hbox{if $w \leq 15$ pallets;} \\
 90 \; min, & \hbox{if $w > 15$ pallets.}
 \end{array}
 \right.
\end{equation*}
$$

单位：min (分钟)。

**(5) MSTL routes**
- 任何一条 MSTL route 的车辆从depot的出发时间可以为早上8:00，也可以为中午12:00；
- 车辆数量不限制；
- 选择单独LTL模式运输的订单不使用任何车辆，按照传统模式运输；

**(6) 其他说明**
- 每个订单的时间窗，即$[最早提货时间 t_{i}^{o}, 最晚送货时间 t_{i}^{d}]$ 或者为 $[8:00, 20:00]$ 或者为 $[12:00, 24:00]$
- 所有运输都要求在当天内完成
- 每辆车自行确定出发时间：一般，如果所提取的第一个订单最早提货时间  $t_{i}^{o}=12:00$，则该车辆应该选择12:00的出发时刻。

##### **2. The shipping network data**
**（1）Centers.csv**
| 属性     | 描述             | 备注                                            |
| -------- | ---------------- | ----------------------------------------------- |
| id       | POI code         |                                                 |
| name     | 地点名称         | 货物起始地或目的地位置                          |
| city     | 市               |                                                 |
| district | 所在城市的行政区 | 用省份邮编前三位表示                            |
| adcode   | 地区邮编         |                                                 |
| province | 所在省份         | 以邮政编码前三位表示                            |
| long     | 经度             |                                                 |
| lat      | 纬度             |                                                 |
| type     | 站点类型         | 类型：start depot, return depot, shipping point |

**（2）route.csv**
| 属性        | 描述     | 备注                       |
| ----------- | -------- | -------------------------- |
| origin      | 线路起点 | POI 位置，代表一个物流节点 |
| destination | 线路终点 | POI 位置，代表一个物流节点 |
| distance    | 驾驶距离 | 单位：km                   |
| duration    | 驾驶时间 | 单位：hour                 |

**注意：**
- 订单数量大于600以上的算例，称为**大规模**问题，不运行枚举算法
- 订单数量不超过600的算例，称为**小规模**问题，需要运行枚举算法

##### **3. Input data format (示例)**
**File name**: inst_JS_300_1.csv

**Parameters:** 

| startdepot | returndepot | numOrders | radius | maxOrder | maxTime | LIFO |
| ---------- | ----------- | --------- | ------ | -------- | ------- | ---- |
| B01FE15XQH | B0FFGPRVIT  | 357       | 40     | 3        | 12      | 1    |

**Orders:** 

| 属性        | 描述                                     | 备注                       |
| ----------- | ---------------------------------------- | -------------------------- |
| id          | 货运订单编号                             |                            |
| origin      | 货物起始地                               | POI 位置，代表一个物流节点 |
| org_city    | 起始地所在城市                           | 城市名称拼音               |
| destination | 货物目的地                               | POI 位置，代表一个物流节点 |
| dest_city   | 目的地所在城市                           | 城市名称拼音               |
| distance    | 直达驾驶距离                             | 单位：km                   |
| duration    | 直达驾驶时间                             | 单位：hour                 |
| pallet      | 货物数量                                 | 单位：pallet               |
| to          | 最早提货时刻                             | 08:00, 或者为12:00         |
| td          | 交货截止时刻                             | 20:00, 或者为 24:00        |
| LTL         | 零担模式单独运输费用                     | 单位：CNY (人民币)         |
| maxDelay    | 针对所有订单设定的一个共同的最大延迟时间 | 单位：hour                 |
| lateCost    | 订单的单位延迟费用                       | 单位：CNY/(pallet*hour)    |

##### **4 Output data format (示例)**
**File name**: result_JS_20221102.csv

**Results:** 
| 数据属性               | 定义                                                         | 单位         |
| ---------------------- | ------------------------------------------------------------ | ------------ |
| ID                     | 算例的ID，即输入数据的csv名称                                |              |
| numOrders              | 订单数量                                                     |              |
| radius                 | bundling 距离限制                                            | km           |
| maxOrder               | bundling 订单数量限制                                        |              |
| maxTime                | bundling 车辆服务时间限制                                    | hour         |
| LIFO                   | loading 规则                                                 |              |
| COST_LTL               | 零担模式单独运输总成本                                       | CNY (人民币) |
|                        |                                                              |              |
| ROUTE_2                | 枚举服务2个order的route数量                                  |              |
| ROUTE_3                | 枚举服务3个order的route数量                                  |              |
| ROUTE_4                | 枚举服务4个order的route数量                                  |              |
| NUM_route              | 所有枚举出的可行route数量（不含单独运输的情形）              |              |
| COST_enum              | 基于枚举的精确优化算法解的最优成本                           | CNY (人民币) |
| TIME_enum              | 基于枚举的精确优化算法计算时间（包含枚举路径和求解CPLEX模型的时间） | minute       |
| LTL_enum               | 基于枚举的精确解中单独运输的订单数量                         |              |
| MSTL2_enum             | 基于枚举的精确解中服务2个order的route数量                    |              |
| MSTL3_enum             | 基于枚举的精确解中服务3个order的route数量                    |              |
| MSTL4_enum             | 基于枚举的精确解中服务4个order的route数量                    |              |
|                        |                                                              | minute       |
| **delayOrder_enum**    | **基于枚举的精确解中交付延迟的订单数量**                     |              |
| **delayTime_avg_enum** | **基于枚举的精确解中，对于发生交付延迟的那些订单，计算延迟时间的平均值** | **hour**     |
| **delayTime_sd_enum**  | **基于枚举的精确解中，对于发生交付延迟的那些订单，计算延迟时间的标准差** | **hour**     |
|                        |                                                              |              |
| NUM_local              | ILS启发式算法总共搜索的局部最优解个数                        |              |
| LTL_ILS                | ILS启发式算法解中单独运输的订单数量                          |              |
| MSTL2_ILS              | ILS启发式算法解中服务2个order的route数量                     |              |
| MSTL3_ILS              | ILS启发式算法解中服务3个order的route数量                     |              |
| MSTL4_ILS              | ILS启发式算法解中服务4个order的route数量                     |              |
| COST_ILS               | ILS启发式算法解的近似最优成本                                | CNY (人民币) |
| ITER_ILS               | ILS启发式算法终止时的迭代次数                                |              |
| TIME_ILS               | ILS启发式算法终止时的计算时间                                | minute       |
| **delayOrder_ILS**     | **ILS启发式算法解中交付延迟的订单数量**                      |              |
| **delayTime_avg_ILS**  | **ILS启发式算法解中，对于发生交付延迟的那些订单，计算延迟时间的平均值** | **hour**     |
| **delayTime_sd_ILS**   | **ILS启发式算法解中，对于发生交付延迟的那些订单，计算延迟时间的标准差** | **hour**     |
|                        |                                                              |              |
| epsilon_exact          | 基于枚举稳定性约束的MP问题分配解中 $\epsilon$ 的值           | CNY (人民币) |
| MAXDEV_exact           | 基于枚举稳定性约束的MP问题分配解中稳定性相对偏离最大值： $100*(\sum_{i\in \mathcal{R}}\xi_{i} - \Gamma(\mathcal{R})) / \Gamma(\mathcal{R})$ | %            |
| instb_exact            | 基于枚举稳定性约束的MP问题分配解中违反稳定性约束的 route 比例 | %            |
| SAVE_avg_exact         | 基于枚举稳定性约束的MP问题分配解中, 对于所有被 MSTL route 服务的 order，计算成本节约 $100*(LTL cost - allocated \; cost) / LTL cost$ 的平均值 | %            |
| SAVE_sd_exact          | 基于枚举稳定性约束的MP问题分配解中, 对于所有被 MSTL route 服务的 order，计算成本节约 100*(LTL cost - allocated cost) / LTL cost的标准差 | %            |
| TIME_exact             | 基于枚举稳定性约束的MP问题求解时间 (包含枚举稳定性约束和求解CPLEX模型的时间） | minute       |
|                        |                                                              |              |
| epsilon_LCS            | LCS算法迭代求解稳定分配解中 $\epsilon$ 的值                  | CNY (人民币) |
| MAXDEV_LCS             | LCS算法迭代求解稳定分配解中稳定性相对偏离最大值： $100*(\sum_{i\in \mathcal{R}}\xi_{i} - \Gamma(\mathcal{R})) / \Gamma(\mathcal{R})$ | %            |
| instb_LCS              | LCS算法迭代求解稳定分配解中违反稳定性约束的 route 比例       | %            |
| SAVE_avg_LCS           | 应用ILS算法求解近似解，应用LCS算法迭代求解稳定分配解，对于所有被 MSTL route 服务的 order，计算成本节约 $100*(LTL cost - allocated \; cost) / LTL cost$ 的平均值 | %            |
| SAVE_sd_LCS            | 应用ILS算法求解近似解，应用LCS算法迭代求解稳定分配解，对于所有被 MSTL route 服务的 order，计算成本节约 100*(LTL cost - allocated cost) / LTL cost的标准差 | %            |
| CONSTR_LCS             | LCS算法迭代求解稳定分配问题中总共加入的稳定性约束个数        |              |
| CONSTR_LCS_initial     | LCS算法迭代求解稳定分配问题中初始加入的稳定性约束个数        |              |
| ITER_LCS               | LCS算法迭代求解稳定分配终止时的迭代次数                      |              |
| TIME_LCS               | LCS算法迭代求解稳定分配计算时间                              | minute       |
| ITER_SP                | 最后一轮求解子问题SP时MSLS算法迭代次数                       |              |
|                        |                                                              |              |
| epsilon_SH             | Shapley value 分配解中 $\epsilon$ 的值                       | CNY (人民币) |
| MAXDEV_SH              | Shapley value  分配解中稳定性相对偏离最大值： $100*(\sum_{i\in \mathcal{R}}\xi_{i} - \Gamma(\mathcal{R})) / \Gamma(\mathcal{R})$ | %            |
| instb_SH               | Shapley value 分配解中违反稳定性约束的 route 比例            | %            |
| SAVE_avg_SH            | 应用ILS算法求解近似解，应用 Shapley value 分配解，对于所有被 MSTL route 服务的 order，计算成本节约 $100*(LTL cost - allocated \; cost) / LTL cost$ 的平均值 | %            |
| SAVE_sd_SH             | 应用ILS算法求解近似解，应用 Shapley value 分配解，对于所有被 MSTL route 服务的 order，计算成本节约 100*(LTL cost - allocated cost) / LTL cost的标准差 | %            |
|                        |                                                              |              |
| epsilon_PR             | 基于重量比例式分配解中 $\epsilon$ 的值                       | CNY (人民币) |
| MAXDEV_PR              | 基于重量比例式分配解中稳定性相对偏离最大值： $100*(\sum_{i\in \mathcal{R}}\xi_{i} - \Gamma(\mathcal{R})) / \Gamma(\mathcal{R})$ | %            |
| instb_PR               | 基于重量比例式分配解中违反稳定性约束的 route 比例            | %            |
| SAVE_avg_PR            | 应用ILS算法求解近似解，应用基于重量比例式分配解，对于所有被 MSTL route 服务的 order，计算成本节约 $100*(LTL cost - allocated \; cost) / LTL cost$ 的平均值 | %            |
| SAVE_sd_PR             | 应用ILS算法求解近似解，应用基于重量比例式分配解，对于所有被 MSTL route 服务的 order，计算成本节约 100*(LTL cost - allocated cost) / LTL cost的标准差 | %            |
|                        |                                                              |              |
| epsilon_DR             | 对偶规则分配解中 $\epsilon$ 的值                             | CNY (人民币) |
| MAXDEV_DR              | 对偶规则分配解中稳定性相对偏离最大值： $100*(\sum_{i\in \mathcal{R}}\xi_{i} - \Gamma(\mathcal{R})) / \Gamma(\mathcal{R})$ | %            |
| instb_DR               | 对偶规则分配解中违反稳定性约束的 route 比例                  | %            |
| SAVE_avg_DR            | 应用ILS算法求解近似解，应用对偶规则分配解，对于所有被 MSTL route 服务的 order，计算成本节约 $100*(LTL cost - allocated \; cost) / LTL cost$ 的平均值 | %            |
| SAVE_sd_DR             | 应用ILS算法求解近似解，应用对偶规则分配解，对于所有被 MSTL route 服务的 order，计算成本节约 100*(LTL cost - allocated cost) / LTL cost的标准差 | %            |
|                        |                                                              |              |
