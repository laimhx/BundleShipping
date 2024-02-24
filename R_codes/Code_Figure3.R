
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



############# Computational efficiency #############
str(ExperJS)

ID <- c(1:nrow(ExperJS))
case <- rep("JS", times = nrow(ExperJS))
gap <- ExperJS$gap_LCS
CON <- 100*ExperJS$NUM_LCS/ExperJS$NUM_enum
enum <- ExperJS$TIME_enum
RG <- ExperJS$TIME_LCS
JS <- data.frame(ID, case, gap, CON, enum, RG)

ID <- nrow(ExperJS) + c(1:nrow(ExperZJ))
case <- rep("ZJ", times = nrow(ExperZJ))
gap <- ExperZJ$gap_LCS
CON <- 100*ExperZJ$NUM_LCS/ExperZJ$NUM_enum
enum <- ExperZJ$TIME_enum
RG <- ExperZJ$TIME_LCS
ZJ <- data.frame(ID, case, gap, CON, enum, RG)

data <- rbind(JS, ZJ)
data$case <- factor(data$case, order=TRUE, levels=c("JS", "ZJ"))
summary(data)

data %>% group_by(case) %>% summarise_all(mean)

c(mean(data$gap), sd(data$gap))
sum(data$gap <= 1e-5)

summary(JS)
sd(JS$gap)

summary(ZJ)
sd(ZJ$gap)

c(mean(data$RG), sd(data$RG))



##optimality gaps 
ttx <- expression(gap[RG])
fig1 <- ggplot(data, aes(x=gap, after_stat(count), fill=case)) + 
  geom_histogram(binwidth=0.01, colour="black", position = "identity", alpha=0.8) +
  labs(x=ttx, y='counts', fill="Network case", title='(a) The optimality losses (Unit: %)') + 
  scale_fill_manual(values=c("OrangeRed", "DodgerBlue")) + 
  scale_x_continuous(breaks=seq.int(0, 0.1, 0.01)) + 
  scale_y_continuous(breaks=seq.int(0, 25, 2.5)) +
  theme_bw() + 
  theme(panel.grid.major = element_line(colour="Black", linetype="dashed", size=0.25), 
        panel.grid.minor = element_blank(),
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


## constraints
ttx <- expression(constr[RG])
fig2 <- ggplot(data, aes(x=CON, after_stat(count), fill=case)) + 
  geom_histogram(binwidth=5, colour="black", position="identity", alpha=0.8) +
  labs(x=ttx, y='count', fill="Network case", title='(b) The proportions of generated routes (Unit: %)') + 
  scale_fill_manual(values=c("OrangeRed", "DodgerBlue")) + 
  scale_x_continuous(breaks=seq.int(0, 50, 5)) + 
  scale_y_continuous(breaks=seq.int(0, 10, 1)) +
  theme_bw() +  
  theme(panel.grid.major = element_line(colour="Black", linetype="dashed", size=0.25), 
        panel.grid.minor = element_blank(),
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


###Enumeration time 
tty <- expression(time[EM])
fig3 <- ggplot(data, aes(x=case, y=enum)) + 
  stat_boxplot(geom="errorbar", colour="DodgerBlue", width=0.25, size=0.6) + 
  geom_boxplot(width=0.3, size=0.6, notch=FALSE, colour="DodgerBlue", outlier.colour="red", 
               outlier.size=2.0) + 
  stat_summary(fun="mean", geom="point", shape=7, size=2.5, fill="DodgerBlue") +
  labs(x='Network case', y=tty, title='(c) The enumeration time (Unit: minute)') + 
  scale_y_continuous(breaks=seq.int(0, 800, 100)) +
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


###RGJS time 
tty <- expression(time[RG])
fig4 <- ggplot(data, aes(x=case, y=RG)) + 
  stat_boxplot(geom="errorbar", colour="DodgerBlue", width=0.25, size=0.6) + 
  geom_boxplot(width=0.3, size=0.6, notch=FALSE, colour="DodgerBlue", outlier.colour="red", 
               outlier.size=2.0) + 
  stat_summary(fun="mean", geom="point", shape=7, size=2.5, fill="DodgerBlue") +
  labs(x='Network case', y=tty, title='(d) The RGJS running time (Unit: minute)') + 
  scale_y_continuous(limits=c(0, 6),breaks=seq.int(0, 6, 1)) +
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





###Figure 3: fig-efficiency
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





