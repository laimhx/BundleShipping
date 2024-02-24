
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

####Import the R-data
setwd('C:/Supplements/')

Routes <- as.data.frame(read.csv("Routes.csv"))
datJS <- as.data.frame(read_csv("freightJS.csv", skip = 3))
datZJ <- as.data.frame(read_csv("freightZJ.csv", skip = 3))

summary(Routes)
summary(datJS)
summary(datZJ)



####### Basic data statistics ####### 
baseStat <- function(data){
  X <- matrix(0,10,4)
  
  ##Network connection
  indO <- unique(data$origin)
  indD <- unique(data$destination)
  
  NV <- 2*nrow(data)+2
  NE <- 0
  
  ###small Radius
  rad <- 50
  
  ## the proximity of origins
  no <- length(indO)
  adjO <- matrix(0,no,no)
  
  for (k1 in 1:no){
    ko1 <- indO[k1]
    
    for (k2 in 1:no){
      ko2 <- indO[k2]
      pos <- (Routes$origin==ko1) & (Routes$destination==ko2)
      
      if (ko1 == ko2) {
        x <- 0
      } else {
        x <- Routes$distance[pos]
      }
      
      adjO[k1,k2] <- (x <= rad) & (x > 0)
      NE <- NE + adjO[k1,k2]
    }
  }
  
  
  ## the proximity of destinations
  nd <- length(indD)
  adjD <- matrix(0,nd,nd)
  
  for (k1 in 1:nd){
    ko1 <- indD[k1]
    
    for (k2 in 1:nd){
      ko2 <- indD[k2]
      pos <- (Routes$origin==ko1) & (Routes$destination==ko2)
      
      if (ko1==ko2) {
        x <- 0
      } else {
        x <- Routes$distance[pos]
      }
      
      adjD[k1,k2] <- (x <= rad) & (x > 0)
      NE <- NE + adjD[k1,k2]
    }
  }
  
  IO <- vector(mode="numeric", length=no)
  ID <- vector(mode="numeric", length=nd)
  for(k in 1:no) {
    x1 <- adjO[k,]
    x2 <- adjO[,k]
    IO[k] <- sum(x1) + sum(x2)
  }
  
  for(k in 1:nd) {
    x1 <- adjD[k,]
    x2 <- adjD[,k]
    ID[k] <- sum(x1) + sum(x2)
  }
  
  X[1,] <- c(mean(IO), sd(IO), min(IO), max(IO))
  X[2,] <- c(mean(ID), sd(ID), min(ID), max(ID))
  
  ###large Radius
  rad <- 100
  
  ## the proximity of origins
  no <- length(indO)
  adjO <- matrix(0,no,no)
  
  for (k1 in 1:no){
    ko1 <- indO[k1]
    
    for (k2 in 1:no){
      ko2 <- indO[k2]
      pos <- (Routes$origin==ko1) & (Routes$destination==ko2)
      
      if (ko1==ko2) {
        x <- 0
      } else {
        x <- Routes$distance[pos]
      }
      
      adjO[k1,k2] <- (x <= rad) & (x > 0)
      NE <- NE + adjO[k1,k2]
    }
  }
  
  
  ## the proximity of destinations
  nd <- length(indD)
  adjD <- matrix(0,nd,nd)
  
  for (k1 in 1:nd){
    ko1 <- indD[k1]
    
    for (k2 in 1:nd){
      ko2 <- indD[k2]
      pos <- (Routes$origin==ko1) & (Routes$destination==ko2)
      
      if (ko1==ko2) {
        x <- 0
      } else {
        x <- Routes$distance[pos]
      }
      
      adjD[k1,k2] <- (x <= rad) & (x > 0)
      NE <- NE + adjD[k1,k2]
    }
  }
  
  IO <- vector(mode="numeric", length=no)
  ID <- vector(mode="numeric", length=nd)
  for(k in 1:no) {
    x1 <- adjO[k,]
    x2 <- adjO[,k]
    IO[k] <- sum(x1) + sum(x2)
  }
  
  for(k in 1:nd) {
    x1 <- adjD[k,]
    x2 <- adjD[,k]
    ID[k] <- sum(x1) + sum(x2)
  }
  
  X[3,] <- c(mean(IO), sd(IO), min(IO), max(IO))
  X[4,] <- c(mean(ID), sd(ID), min(ID), max(ID))
  
  ##order data
  X[5,] <- c(mean(data$distance), sd(data$distance), min(data$distance), max(data$distance))
  X[6,] <- c(mean(data$duration), sd(data$duration), min(data$duration), max(data$duration))
  X[7,] <- c(mean(data$pallet), sd(data$pallet), min(data$pallet), max(data$pallet))
  X[8,] <- c(mean(data$lateCost), sd(data$lateCost), min(data$lateCost), max(data$lateCost))
  X[9,] <- c(mean(data$maxDelay), sd(data$maxDelay), min(data$maxDelay), max(data$maxDelay))
  X[10,] <- c(mean(data$LTL), sd(data$LTL), min(data$LTL), max(data$LTL))
  
  ###final result
  return(X)
}


###Table 2: summary view
X1 <- baseStat(datJS)
X2 <- baseStat(datZJ)
baseX <- cbind(X1, X2)
round(baseX, digits = 2)
 

###running time
t1 <- Sys.time()
tt <- t1-t0
tt





