
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



############# Individual savings #############
str(ExperJS)

ID <- c(1:nrow(ExperJS))
EM <- ExperJS$SAVE_avg_exact
LC <- ExperJS$SAVE_avg_LCS
X0 <- data.frame(ID, EM, LC)
X <- data.frame(melt(X0, id="ID"))
net <- rep('JS', times = nrow(X))
case <- paste(net, as.character(X$variable), sep='-')
JS <- data.frame(X[,c("ID", "value")], case)

ID <- nrow(ExperJS) + c(1:nrow(ExperZJ))
EM <- ExperZJ$SAVE_avg_exact
LC <- ExperZJ$SAVE_avg_LCS
X0 <- data.frame(ID, EM, LC)
X <- data.frame(melt(X0, id="ID"))
net <- rep('ZJ', times = nrow(X))
case <- paste(net, as.character(X$variable), sep='-')
ZJ <- data.frame(X[,c("ID", "value")], case)

data <- rbind(JS, ZJ)
data$case <- factor(data$case, order=TRUE, levels=c("JS-EM", "JS-LC", "ZJ-EM", "ZJ-LC"))
summary(data)

data %>% group_by(case) %>% summarise_all(mean)

####Average savings
tt1 <- expression(paste('JS: ', save[EM]^avg))
tt2 <- expression(paste('JS: ', save[LC]^avg))
tt3 <- expression(paste('ZJ: ', save[EM]^avg))
tt4 <- expression(paste('ZJ: ', save[LC]^avg))
tty <- expression(save^avg)
fig1 <- ggplot(data, aes(x=case, y=value)) + 
  stat_boxplot(geom="errorbar", colour="DodgerBlue", width=0.25, size=0.6) + 
  geom_boxplot(width=0.3, size=0.6, notch=FALSE, colour="DodgerBlue", outlier.colour="red", 
               outlier.size=2.0) + 
  stat_summary(fun="mean", geom="point", shape=7, size=2.5, fill="DodgerBlue") +
  labs(x='Case', y=tty) + 
  scale_x_discrete(label=c(tt1, tt2, tt3, tt4)) +
  scale_y_continuous(breaks=seq.int(15, 30, 2.5)) +
  theme_bw() + 
  theme(panel.grid.major = element_blank(), 
        panel.grid.minor = element_line(colour="Black", linetype="dashed", size=0.25),
        legend.position=c(1,1), legend.justification=c(1,1), 
        legend.background=element_blank(),
        legend.key=element_blank(), legend.key.height=unit(0.5,"cm"), 
        legend.title=element_text(size=rel(1.15)), 
        legend.text=element_text(size=rel(1.15)),
        plot.title = element_text(hjust=0.5, size=rel(1.5),family="Times"), 
        axis.title.x = element_text(size=rel(1.5),family="Times"),
        axis.title.y = element_text(size=rel(1.5),family="Times"),
        axis.text.x = element_text(size=rel(1.25)),
        axis.text.y = element_text(size=rel(1.25)))
fig1




###Figure 5: individual savings
fig1
#export size: 7 x 10



### running time
t1 <- Sys.time()
tt <- t1-t0
tt





