
################################################################################
rm(list = ls())
#####import packages
library(readxl)
library(gmodels)
library(permute)
library(lattice)
library(grid)
library(ggplot2)
library(cowplot)
library(rgl)
library(gcookbook)
library(magrittr)
library(reshape2)
library(plyr)
library(dplyr)
library(tidyverse)
library(ggthemes)
library(openxlsx)
library(readr)


###timer
t0 <- Sys.time()


####### Import the R-data ####### 
setwd('C:/Supplements/')

ExperJS <- as.data.frame(read.csv("ExperJS_20230912.csv"))
ExperZJ <- as.data.frame(read.csv("ExperZJ_20230912.csv"))

summary(ExperJS)
summary(ExperZJ)




####### Optimization results ####### 
NX <- c(300, 400, 500)
str(ExperJS)

JS <- matrix(0,length(NX),14)
for(k in c(1:length(NX))) {
  pos <- (ExperJS$NUM >= NX[k] & ExperJS$NUM < NX[k]+100)
  Impr_enum <- ExperJS$impr_enum[pos]
  Bundle_enum <- ExperJS$bundle_enum[pos]
  L_enum <- ExperJS$LTL_enum[pos]
  M2_enum <- ExperJS$MSTL2_enum[pos]
  M3_enum <- ExperJS$MSTL3_enum[pos] + ExperJS$MSTL4_enum[pos]
  Delay_enum <- ExperJS$delayOrder_enum[pos]
  Time_enum <- ExperJS$delayTime_avg_enum[pos]
  
  Impr_LCS <- ExperJS$impr_LCS[pos]
  Bundle_LCS <- ExperJS$bundle_LCS[pos]
  L_LCS <- ExperJS$LTL_LCS[pos]
  M2_LCS <- ExperJS$MSTL2_LCS[pos]
  M3_LCS <- ExperJS$MSTL3_LCS[pos] + ExperJS$MSTL4_LCS[pos]
  Delay_LCS <- ExperJS$delayOrder_LCS[pos]
  Time_LCS <- ExperJS$delayTime_avg_LCS[pos]
  
  JS[k,] <- c(mean(Impr_enum), mean(Bundle_enum), mean(L_enum), mean(M2_enum), 
              mean(M3_enum), mean(Delay_enum), mean(Time_enum), 
              mean(Impr_LCS), mean(Bundle_LCS), mean(L_LCS), mean(M2_LCS), 
              mean(M3_LCS), mean(Delay_LCS), mean(Time_LCS))
}


ZJ <- matrix(0,length(NX),14)
for(k in c(1:length(NX))) {
  pos <- (ExperZJ$NUM >= NX[k] & ExperZJ$NUM < NX[k]+100)
  Impr_enum <- ExperZJ$impr_enum[pos]
  Bundle_enum <- ExperZJ$bundle_enum[pos]
  L_enum <- ExperZJ$LTL_enum[pos]
  M2_enum <- ExperZJ$MSTL2_enum[pos]
  M3_enum <- ExperZJ$MSTL3_enum[pos] + ExperZJ$MSTL4_enum[pos]
  Delay_enum <- ExperZJ$delayOrder_enum[pos]
  Time_enum <- ExperZJ$delayTime_avg_enum[pos]
  
  Impr_LCS <- ExperZJ$impr_LCS[pos]
  Bundle_LCS <- ExperZJ$bundle_LCS[pos]
  L_LCS <- ExperZJ$LTL_LCS[pos]
  M2_LCS <- ExperZJ$MSTL2_LCS[pos]
  M3_LCS <- ExperZJ$MSTL3_LCS[pos] + ExperZJ$MSTL4_LCS[pos]
  Delay_LCS <- ExperZJ$delayOrder_LCS[pos]
  Time_LCS <- ExperZJ$delayTime_avg_LCS[pos]
  
  ZJ[k,] <- c(mean(Impr_enum), mean(Bundle_enum), mean(L_enum), mean(M2_enum), 
              mean(M3_enum), mean(Delay_enum), mean(Time_enum), 
              mean(Impr_LCS), mean(Bundle_LCS), mean(L_LCS), mean(M2_LCS), 
              mean(M3_LCS), mean(Delay_LCS), mean(Time_LCS))
}





###Table 4: optimization results
optimize <- rbind(JS, ZJ)
round(optimize, digits = 2)


 

###running time
t1 <- Sys.time()
tt <- t1-t0
tt





