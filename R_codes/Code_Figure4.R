
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



############# Cost allocations #############
### (epsilon, stb): exact, LCS, PR, DR
###epsilon: exact, LCS
ID <- c(1:nrow(ExperJS))
EM <- ExperJS$epsilon_exact
LC <- ExperJS$epsilon_LCS
X0 <- data.frame(ID, EM, LC)
X <- data.frame(melt(X0, id="ID"))
net <- rep('JS', times = nrow(X))
case <- paste(net, as.character(X$variable), sep='-')
JS <- data.frame(X[,c("ID", "value")], case)

ID <- nrow(ExperJS) + c(1:nrow(ExperZJ))
EM <- ExperZJ$epsilon_exact
LC <- ExperZJ$epsilon_LCS
X0 <- data.frame(ID, EM, LC)
X <- data.frame(melt(X0, id="ID"))
net <- rep('ZJ', times = nrow(X))
case <- paste(net, as.character(X$variable), sep='-')
ZJ <- data.frame(X[,c("ID", "value")], case)

data <- rbind(JS, ZJ)
data$case <- factor(data$case, order=TRUE, levels=c("JS-EM", "JS-LC", "ZJ-EM", "ZJ-LC"))
summary(data)

n1 <- sum(ExperJS$epsilon_exact > 0)
n2 <- sum(ExperZJ$epsilon_exact > 0)
c(n1, n2)
c(mean(ExperJS$epsilon_exact), sd(ExperJS$epsilon_exact))
c(mean(ExperZJ$epsilon_exact), sd(ExperZJ$epsilon_exact))

c(mean(ExperJS$epsilon_DR), sd(ExperJS$epsilon_DR))
c(mean(ExperZJ$epsilon_DR), sd(ExperZJ$epsilon_DR))

data %>% group_by(case) %>% summarise_all(mean)


tt1 <- expression(paste('JS: ', epsilon[EM]))
tt2 <- expression(paste('JS: ', epsilon[LC]))
tt3 <- expression(paste('ZJ: ', epsilon[EM]))
tt4 <- expression(paste('ZJ: ', epsilon[LC]))
tty <- expression(epsilon)
fig1 <- ggplot(data, aes(x=case, y=value)) + 
  stat_boxplot(geom="errorbar", colour="DodgerBlue", width=0.25, size=0.6) + 
  geom_boxplot(width=0.3, size=0.6, notch=FALSE, colour="DodgerBlue", outlier.colour="red", 
               outlier.size=2.0) + 
  stat_summary(fun="mean", geom="point", shape=7, size=2.5, fill="DodgerBlue") +
  labs(x='Case', y=tty, title='(a) Stability deviation of least-core allocations') + 
  scale_x_discrete(label=c(tt1, tt2, tt3, tt4)) +
  scale_y_continuous(breaks=seq.int(0, 80, 10)) +
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


###epsilon: PR, DR
ID <- c(1:nrow(ExperJS))
PR <- ExperJS$epsilon_PR
DR <- ExperJS$epsilon_DR
X0 <- data.frame(ID, PR, DR)
X <- data.frame(melt(X0, id="ID"))
net <- rep('JS', times = nrow(X))
case <- paste(net, as.character(X$variable), sep='-')
JS <- data.frame(X[,c("ID", "value")], case)

ID <- nrow(ExperJS) + c(1:nrow(ExperZJ))
PR <- ExperZJ$epsilon_PR
DR <- ExperZJ$epsilon_DR
X0 <- data.frame(ID, PR, DR)
X <- data.frame(melt(X0, id="ID"))
net <- rep('ZJ', times = nrow(X))
case <- paste(net, as.character(X$variable), sep='-')
ZJ <- data.frame(X[,c("ID", "value")], case)

data <- rbind(JS, ZJ)
data$case <- factor(data$case, order=TRUE, levels=c("JS-PR", "JS-DR", "ZJ-PR", "ZJ-DR"))
summary(data)


tt1 <- expression(paste('JS: ', epsilon[PR]))
tt2 <- expression(paste('JS: ', epsilon[DR]))
tt3 <- expression(paste('ZJ: ', epsilon[PR]))
tt4 <- expression(paste('ZJ: ', epsilon[DR]))
tty <- expression(epsilon)
fig2 <- ggplot(data, aes(x=case, y=value)) + 
  stat_boxplot(geom="errorbar", colour="DodgerBlue", width=0.25, size=0.6) + 
  geom_boxplot(width=0.3, size=0.6, notch=FALSE, colour="DodgerBlue", outlier.colour="red", 
               outlier.size=2.0) + 
  stat_summary(fun="mean", geom="point", shape=7, size=2.5, fill="DodgerBlue") +
  labs(x='Case', y=tty, title='(b) Stability deviation of comparison rules') + 
  scale_x_discrete(label=c(tt1, tt2, tt3, tt4)) +
  scale_y_continuous(breaks=seq.int(0, 5000, 1000)) +
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
fig2


###stb: exact, LCS
ID <- c(1:nrow(ExperJS))
EM <- ExperJS$instb_exact
LC <- ExperJS$instb_LCS
X0 <- data.frame(ID, EM, LC)
X <- data.frame(melt(X0, id="ID"))
net <- rep('JS', times = nrow(X))
case <- paste(net, as.character(X$variable), sep='-')
JS <- data.frame(X[,c("ID", "value")], case)

ID <- nrow(ExperJS) + c(1:nrow(ExperZJ))
EM <- ExperZJ$instb_exact
LC <- ExperZJ$instb_LCS
X0 <- data.frame(ID, EM, LC)
X <- data.frame(melt(X0, id="ID"))
net <- rep('ZJ', times = nrow(X))
case <- paste(net, as.character(X$variable), sep='-')
ZJ <- data.frame(X[,c("ID", "value")], case)


data <- rbind(JS, ZJ)
data$case <- factor(data$case, order=TRUE, levels=c("JS-EM", "JS-LC", "ZJ-EM", "ZJ-LC"))
summary(data)

tt1 <- expression(paste('JS: ', instb[EM]))
tt2 <- expression(paste('JS: ', instb[RG]))
tt3 <- expression(paste('ZJ: ', instb[EM]))
tt4 <- expression(paste('ZJ: ', instb[RG]))
tty <- expression(instb)
fig3 <- ggplot(data, aes(x=case, y=value)) + 
  stat_boxplot(geom="errorbar", colour="DodgerBlue", width=0.25, size=0.6) + 
  geom_boxplot(width=0.3, size=0.6, notch=FALSE, colour="DodgerBlue", outlier.colour="red", 
               outlier.size=2.0) + 
  stat_summary(fun="mean", geom="point", shape=7, size=2.5, fill="DodgerBlue") +
  labs(x='Case', y=tty, title='(c) Instability proportions of least-core (Unit: %)') + 
  scale_x_discrete(label=c(tt1, tt2, tt3, tt4)) +
  scale_y_continuous(breaks=seq.int(0, 20, 2.5)) +
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
fig3


###stb: PR, DR
ID <- c(1:nrow(ExperJS))
PR <- ExperJS$instb_PR
DR <- ExperJS$instb_DR
X0 <- data.frame(ID, PR, DR)
X <- data.frame(melt(X0, id="ID"))
net <- rep('JS', times = nrow(X))
case <- paste(net, as.character(X$variable), sep='-')
JS <- data.frame(X[,c("ID", "value")], case)

ID <- nrow(ExperJS) + c(1:nrow(ExperZJ))
PR <- ExperZJ$instb_PR
DR <- ExperZJ$instb_DR
X0 <- data.frame(ID, PR, DR)
X <- data.frame(melt(X0, id="ID"))
net <- rep('ZJ', times = nrow(X))
case <- paste(net, as.character(X$variable), sep='-')
ZJ <- data.frame(X[,c("ID", "value")], case)

data <- rbind(JS, ZJ)
data$case <- factor(data$case, order=TRUE, levels=c("JS-PR", "JS-DR", "ZJ-PR", "ZJ-DR"))
summary(data)

tt1 <- expression(paste('JS: ', instb[PR]))
tt2 <- expression(paste('JS: ', instb[DR]))
tt3 <- expression(paste('ZJ: ', instb[PR]))
tt4 <- expression(paste('ZJ: ', instb[DR]))
tty <- expression(instb)
fig4 <- ggplot(data, aes(x=case, y=value)) + 
  stat_boxplot(geom="errorbar", colour="DodgerBlue", width=0.25, size=0.6) + 
  geom_boxplot(width=0.3, size=0.6, notch=FALSE, colour="DodgerBlue", outlier.colour="red", 
               outlier.size=2.0) + 
  stat_summary(fun="mean", geom="point", shape=7, size=2.5, fill="DodgerBlue") +
  labs(x='Case', y=tty, title='(d) Instability proportions of comparision rules (Unit: %)') + 
  scale_x_discrete(label=c(tt1, tt2, tt3, tt4)) +
  scale_y_continuous(breaks=seq.int(0, 35, 5)) +
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
fig4



###Figure 4: allocations
library(grid)
grid.newpage()
pushViewport(viewport(layout = grid.layout(2,2)))
vplayout <- function(x,y) {
  viewport(layout.pos.row = x, layout.pos.col = y)
}
print(fig1, vp = vplayout(1,1))
print(fig2, vp = vplayout(1,2))
print(fig3, vp = vplayout(2,1))
print(fig4, vp = vplayout(2,2))
#export size: 9 x 13





###running time
t1 <- Sys.time()
tt <- t1-t0
tt





