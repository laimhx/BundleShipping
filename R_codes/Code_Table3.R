
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



####### Complexity summary ####### 
###LIFO case
NX <- c(300, 400, 500)
str(ExperJS)

JS <- matrix(0,length(NX),20)
for(k in c(1:length(NX))) {
  pos <- (ExperJS$NUM >= NX[k] & ExperJS$NUM < NX[k]+100)
  
  numX <- ExperJS$NUM[pos]
  RX <- ExperJS$radius[pos]
  VX <- ExperJS$numV[pos]
  EX <- ExperJS$numE[pos]/10^4
  dX <- ExperJS$numE[pos]/ExperJS$numV[pos]
  
  EM2X <- ExperJS$Two_enum[pos]/10^3
  EM3X <- (ExperJS$Three_enum[pos]+ExperJS$Four_enum[pos])/10^3
  
  RG2X <- ExperJS$Two_LCS[pos]/10^3
  RG3X <- (ExperJS$Three_LCS[pos]+ExperJS$Four_LCS[pos])/10^3
  
  CX <- ExperJS$OriginCost[pos]/10^4
  
  JS[k,] <- c(mean(numX), sd(numX), mean(RX), sd(RX), mean(VX), sd(VX), 
              mean(EX), sd(EX), mean(dX), sd(dX), mean(EM2X), sd(EM2X), 
              mean(EM3X), sd(EM3X), mean(RG2X), sd(RG2X), mean(RG3X), sd(RG3X), 
              mean(CX), sd(CX))
}

ZJ <- matrix(0,length(NX),20)
for(k in c(1:length(NX))) {
  pos <- (ExperZJ$NUM >= NX[k] & ExperZJ$NUM < NX[k]+100)
  numX <- ExperZJ$NUM[pos]
  RX <- ExperZJ$radius[pos]
  VX <- ExperZJ$numV[pos]
  EX <- ExperZJ$numE[pos]/10^4
  dX <- ExperZJ$numE[pos]/ExperZJ$numV[pos]
  
  EM2X <- ExperZJ$Two_enum[pos]/10^3
  EM3X <- (ExperZJ$Three_enum[pos]+ExperZJ$Four_enum[pos])/10^3
  
  RG2X <- ExperZJ$Two_LCS[pos]/10^3
  RG3X <- (ExperZJ$Three_LCS[pos]+ExperZJ$Four_LCS[pos])/10^3
  
  CX <- ExperZJ$OriginCost[pos]/10^4
  
  ZJ[k,] <- c(mean(numX), sd(numX), mean(RX), sd(RX), mean(VX), sd(VX), 
              mean(EX), sd(EX), mean(dX), sd(dX), mean(EM2X), sd(EM2X), 
              mean(EM3X), sd(EM3X), mean(RG2X), sd(RG2X), mean(RG3X), sd(RG3X), 
              mean(CX), sd(CX))
}



###Table 3: complexity summary
compleX <- rbind(JS, ZJ)
round(compleX, digits = 2)


 

###running time
t1 <- Sys.time()
tt <- t1-t0
tt





