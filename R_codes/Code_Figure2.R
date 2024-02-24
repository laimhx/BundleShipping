
################################################################################
rm(list = ls())
#####import packages
library(readxl)
library(permute)
library(grid)
library(ggplot2)
library(rgl)
library(parallel)
library(gcookbook)
library(magrittr)
library(reshape2)
library(plyr)
library(dplyr)
library(tidyverse)
library(lubridate)
library(ggthemes)
library(openxlsx)
library(readr)
library(gmodels)
library(leafletCN)
library(leaflet)
library(geosphere)
library(htmltools)
library(htmlwidgets)
library(webshot)
library(mapview)


###timer
t0 <- Sys.time()

######## Import the R-data ########
setwd('C:/Supplements/')
Centers <- read.csv("Centers.csv")
summary(Centers)



######## The shipping network ########
###depots
pos <- (Centers$type == 'start depot' | Centers$type == 'return depot')
depots <- Centers[pos,]

Centers$type <- factor(Centers$type, order=TRUE, 
                       levels=c('start depot', 'return depot', 'origin point', 'destination point'))

###map
palx <- colorFactor(c("OrangeRed", "DodgerBlue", "Green", "Purple"), 
                    domain = c('start depot', 'return depot', 'origin point', 
                               'destination point'))

Map <- leaflet(Centers) %>% addTiles() %>%
  addProviderTiles(providers$CartoDB.Positron) %>%
  addCircleMarkers(data = Centers, ~long, ~lat, radius = 2.5,  
                   stroke = TRUE, color = ~palx(type), weight = 1.5, opacity = 1, 
                   fill = TRUE, fillColor = ~palx(type), fillOpacity = 1) %>%
  addCircles(data = depots, ~long, ~lat, radius = 3000, weight = 1.0, stroke = TRUE, 
             color = 'black', opacity = 1, fill = FALSE) %>% 
  addLegend(data = Centers, position = c("topright"), pal = palx, values = ~type, opacity = 1.0, 
            title = "The types of locations", 
            labels = c('start depot', 'return depot', 'origin point', 'destination point')) %>%
  setView(lng=120.5484, lat=31.55266, zoom=8.5)
Map




###running time
t1 <- Sys.time()
tt <- t1-t0
tt




