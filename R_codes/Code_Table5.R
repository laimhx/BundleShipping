
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

LowJS <- as.data.frame(read.csv("bundling/LargeJS_20230912.csv"))
HighJS <- as.data.frame(read.csv("bundling/LargeJS_20230914.csv"))
LowZJ <- as.data.frame(read.csv("bundling/LargeZJ_20230912.csv"))
HighZJ <- as.data.frame(read.csv("bundling/LargeZJ_20230915.csv"))

summary(LowJS)
summary(LowZJ)
summary(HighJS)
summary(HighZJ)




############# Large-scale results #############
###JS case
str(LowJS)
NX <- c(1200, 1400, 1600, 1800, 2000)
mx <- 5

JS <- matrix(0,22,10)
densJ <- vector(mode = "numeric", length = 10)
routJ <- vector(mode = "numeric", length = 10)
for (k in 1:length(NX)) {
  ###Low LTL case
  lc <- 0
  pos <- (LowJS$NUM == NX[k])
  
  JS[1,lc+k] <- mean(LowJS$NUM[pos]) #N
  JS[2,lc+k] <- mean(LowJS$numV[pos]) #V
  JS[3,lc+k] <- mean(LowJS$numE[pos])/10^6 #A
  JS[4,lc+k] <- mean(LowJS$OriginCost[pos])/10^6  #LTL 
  
  densJ[lc+k] <- mean(LowJS$numE[pos]/LowJS$numV[pos])
  
  JS[5,lc+k] <- mean(LowJS$Two_LCS[pos])/10^3
  JS[6,lc+k] <- mean((LowJS$Three_LCS[pos]+LowJS$Four_LCS[pos]))/10^3
  JS[7,lc+k] <- mean(LowJS$ITER_LCS[pos]) #iter 
  JS[8,lc+k] <- mean(LowJS$TIME_LCS[pos]) #time
  
  routJ[lc+k] <- mean(LowJS$Two_LCS[pos]+LowJS$Three_LCS[pos]+LowJS$Four_LCS[pos])/10^3
  
  JS[9,lc+k] <- mean(LowJS$impr_LCS[pos]) #impr 
  JS[10,lc+k] <- mean(LowJS$bundle_LCS[pos]) #ratio
  JS[11,lc+k] <- mean(LowJS$LTL_LCS[pos]) #LTL
  JS[12,lc+k] <- mean(LowJS$MSTL2_LCS[pos]) #MSTL2 
  JS[13,lc+k] <- mean(LowJS$MSTL3_LCS[pos]+LowJS$MSTL4_LCS[pos]) #MSTL3+  
  
  JS[14,lc+k] <- mean(LowJS$delayOrder_LCS[pos]) #lateOrder 
  JS[15,lc+k] <- mean(LowJS$delayTime_avg_LCS[pos]) #lateTime 
  
  JS[16,lc+k] <- mean(LowJS$epsilon_LCS[pos])/10^3 #epsLC 
  JS[17,lc+k] <- mean(LowJS$instb_LCS[pos]) #instbLC 
  JS[18,lc+k] <- mean(LowJS$SAVE_avg_LCS[pos]) #save_avgLC 
  
  JS[19,lc+k] <- mean(LowJS$epsilon_PR[pos])/10^3 #epsPR  
  JS[20,lc+k] <- mean(LowJS$instb_PR[pos]) #instbPR  
  JS[21,lc+k] <- mean(LowJS$epsilon_DR[pos])/10^3  #epsDR  
  JS[22,lc+k] <- mean(LowJS$instb_DR[pos]) #instbDR
  
  ###High LTL case
  lc <- 5
  pos <- (HighJS$NUM == NX[k])
  
  JS[1,lc+k] <- mean(HighJS$NUM[pos]) #N
  JS[2,lc+k] <- mean(HighJS$numV[pos]) #V
  JS[3,lc+k] <- mean(HighJS$numE[pos])/10^6 #A
  JS[4,lc+k] <- mean(HighJS$OriginCost[pos])/10^6  #LTL 
  
  densJ[lc+k] <- mean(HighJS$numE[pos]/HighJS$numV[pos])
  
  JS[5,lc+k] <- mean(HighJS$Two_LCS[pos])/10^3
  JS[6,lc+k] <- mean((HighJS$Three_LCS[pos]+HighJS$Four_LCS[pos]))/10^3
  JS[7,lc+k] <- mean(HighJS$ITER_LCS[pos]) #iter 
  JS[8,lc+k] <- mean(HighJS$TIME_LCS[pos]) #time
  
  routJ[lc+k] <- mean(HighJS$Two_LCS[pos]+HighJS$Three_LCS[pos]+HighJS$Four_LCS[pos])/10^3
  
  JS[9,lc+k] <- mean(HighJS$impr_LCS[pos]) #impr 
  JS[10,lc+k] <- mean(HighJS$bundle_LCS[pos]) #ratio
  JS[11,lc+k] <- mean(HighJS$LTL_LCS[pos]) #LTL
  JS[12,lc+k] <- mean(HighJS$MSTL2_LCS[pos]) #MSTL2 
  JS[13,lc+k] <- mean(HighJS$MSTL3_LCS[pos]+HighJS$MSTL4_LCS[pos]) #MSTL3+  
  
  JS[14,lc+k] <- mean(HighJS$delayOrder_LCS[pos]) #lateOrder 
  JS[15,lc+k] <- mean(HighJS$delayTime_avg_LCS[pos]) #lateTime 
  
  JS[16,lc+k] <- mean(HighJS$epsilon_LCS[pos])/10^3 #epsLC 
  JS[17,lc+k] <- mean(HighJS$instb_LCS[pos]) #instbLC 
  JS[18,lc+k] <- mean(HighJS$SAVE_avg_LCS[pos]) #save_avgLC 
  
  JS[19,lc+k] <- mean(HighJS$epsilon_PR[pos])/10^3 #epsPR  
  JS[20,lc+k] <- mean(HighJS$instb_PR[pos]) #instbPR  
  JS[21,lc+k] <- mean(HighJS$epsilon_DR[pos])/10^3  #epsDR  
  JS[22,lc+k] <- mean(HighJS$instb_DR[pos]) #instbDR
}


###ZJ case
NX <- c(1600, 2000, 2400, 2800, 3200)
mx <- 5

ZJ <- matrix(0,22,10)
densZ <- vector(mode = "numeric", length = 10)
routZ <- vector(mode = "numeric", length = 10)
for (k in 1:length(NX)) {
  ###Low LTL case
  lc <- 0
  pos <- (LowZJ$NUM == NX[k])
  
  ZJ[1,lc+k] <- mean(LowZJ$NUM[pos]) #N
  ZJ[2,lc+k] <- mean(LowZJ$numV[pos]) #V
  ZJ[3,lc+k] <- mean(LowZJ$numE[pos])/10^6 #A
  ZJ[4,lc+k] <- mean(LowZJ$OriginCost[pos])/10^6  #LTL 
  
  densZ[lc+k] <- mean(LowZJ$numE[pos]/LowZJ$numV[pos])
  
  ZJ[5,lc+k] <- mean(LowZJ$Two_LCS[pos])/10^3
  ZJ[6,lc+k] <- mean((LowZJ$Three_LCS[pos]+LowZJ$Four_LCS[pos]))/10^3
  ZJ[7,lc+k] <- mean(LowZJ$ITER_LCS[pos]) #iter 
  ZJ[8,lc+k] <- mean(LowZJ$TIME_LCS[pos]) #time
  
  routZ[lc+k] <- mean(LowZJ$Two_LCS[pos]+LowZJ$Three_LCS[pos]+LowZJ$Four_LCS[pos])/10^3
  
  ZJ[9,lc+k] <- mean(LowZJ$impr_LCS[pos]) #impr 
  ZJ[10,lc+k] <- mean(LowZJ$bundle_LCS[pos]) #ratio
  ZJ[11,lc+k] <- mean(LowZJ$LTL_LCS[pos]) #LTL
  ZJ[12,lc+k] <- mean(LowZJ$MSTL2_LCS[pos]) #MSTL2 
  ZJ[13,lc+k] <- mean(LowZJ$MSTL3_LCS[pos]+LowZJ$MSTL4_LCS[pos]) #MSTL3+  
  
  ZJ[14,lc+k] <- mean(LowZJ$delayOrder_LCS[pos]) #lateOrder 
  ZJ[15,lc+k] <- mean(LowZJ$delayTime_avg_LCS[pos]) #lateTime 
  
  ZJ[16,lc+k] <- mean(LowZJ$epsilon_LCS[pos])/10^3 #epsLC 
  ZJ[17,lc+k] <- mean(LowZJ$instb_LCS[pos]) #instbLC 
  ZJ[18,lc+k] <- mean(LowZJ$SAVE_avg_LCS[pos]) #save_avgLC 
  
  ZJ[19,lc+k] <- mean(LowZJ$epsilon_PR[pos])/10^3 #epsPR  
  ZJ[20,lc+k] <- mean(LowZJ$instb_PR[pos]) #instbPR  
  ZJ[21,lc+k] <- mean(LowZJ$epsilon_DR[pos])/10^3  #epsDR  
  ZJ[22,lc+k] <- mean(LowZJ$instb_DR[pos]) #instbDR
  
  ###High LTL case
  lc <- 5
  pos <- (HighZJ$NUM == NX[k])
  
  ZJ[1,lc+k] <- mean(HighZJ$NUM[pos]) #N
  ZJ[2,lc+k] <- mean(HighZJ$numV[pos]) #V
  ZJ[3,lc+k] <- mean(HighZJ$numE[pos])/10^6 #A
  ZJ[4,lc+k] <- mean(HighZJ$OriginCost[pos])/10^6  #LTL 
  
  densZ[lc+k] <- mean(HighZJ$numE[pos]/HighZJ$numV[pos])
  
  ZJ[5,lc+k] <- mean(HighZJ$Two_LCS[pos])/10^3
  ZJ[6,lc+k] <- mean((HighZJ$Three_LCS[pos]+HighZJ$Four_LCS[pos]))/10^3
  ZJ[7,lc+k] <- mean(HighZJ$ITER_LCS[pos]) #iter 
  ZJ[8,lc+k] <- mean(HighZJ$TIME_LCS[pos]) #time
  
  routZ[lc+k] <- mean(HighZJ$Two_LCS[pos]+HighZJ$Three_LCS[pos]+HighZJ$Four_LCS[pos])/10^3
  
  ZJ[9,lc+k] <- mean(HighZJ$impr_LCS[pos]) #impr 
  ZJ[10,lc+k] <- mean(HighZJ$bundle_LCS[pos]) #ratio
  ZJ[11,lc+k] <- mean(HighZJ$LTL_LCS[pos]) #LTL
  ZJ[12,lc+k] <- mean(HighZJ$MSTL2_LCS[pos]) #MSTL2 
  ZJ[13,lc+k] <- mean(HighZJ$MSTL3_LCS[pos]+HighZJ$MSTL4_LCS[pos]) #MSTL3+  
  
  ZJ[14,lc+k] <- mean(HighZJ$delayOrder_LCS[pos]) #lateOrder 
  ZJ[15,lc+k] <- mean(HighZJ$delayTime_avg_LCS[pos]) #lateTime 
  
  ZJ[16,lc+k] <- mean(HighZJ$epsilon_LCS[pos])/10^3 #epsLC 
  ZJ[17,lc+k] <- mean(HighZJ$instb_LCS[pos]) #instbLC 
  ZJ[18,lc+k] <- mean(HighZJ$SAVE_avg_LCS[pos]) #save_avgLC 
  
  ZJ[19,lc+k] <- mean(HighZJ$epsilon_PR[pos])/10^3 #epsPR  
  ZJ[20,lc+k] <- mean(HighZJ$instb_PR[pos]) #instbPR  
  ZJ[21,lc+k] <- mean(HighZJ$epsilon_DR[pos])/10^3  #epsDR  
  ZJ[22,lc+k] <- mean(HighZJ$instb_DR[pos]) #instbDR
}




###Table 5: large-scale results
data <- cbind(JS, ZJ)
round(data, digits = 2)

 

###running time
t1 <- Sys.time()
tt <- t1-t0
tt





