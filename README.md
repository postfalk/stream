# Natural Flows API v2 draft

## Introduction ##

While the Natural Flow API in its original form https://rivers.codefornature.org/ is well suited for complex queries and a limited number of returned records we encountered two major challenges. 

1. The MongoDB-based query schema is rather cumbersome, especially the combined use of queries and projection is overcomplicated. 
2. The data download runs into limitswhen requesting bigger amount of data. The reason is that MongoDB needs to load the data on which it operates on into memory which is limited. It works well for limited downloads up to 2,000 stream segments, e.g. for all gauged streams but it will break down for larger geographies like the L.A. basin.

The new version of the API will split querying into two different pieces. 

1. Query for stream segments on the basis of NHDv2 attributes such as name, geography, stream order, gauging, etc. 
2. Filter the Natural Flow dataset based on a list of stream segments as well as simple filter that subset the data further by measurements, variables, months, and dates. 

Both query steps will be based on independant APIs which keep the two datasets (NHDv2 and USGS/TNC Natural Flows) seperate. It will be up to the user to link the required subsets. We are currently building a GUI that will allow a web-based workflow that will make it easy to get data for a particular area or time span. 

For the first step, querying for stream segments the first iteration of the API can stand in, especially if the ```data``` attribute is excluded from the projection: https://rivers.codefornature.org/api/data/?exclude=data (TODO: currently does not work). Any map service based on NHDv2 can be used to achieve that rasult. (we might prototype right of ArcGIS Online). 

For the second step we build a powerful streaming up based on Scala, Play, and Akka that allows for streaming and filtering of the entire data set (47.3 GigaByte) without https timeouts or memory overrun. In current production setup the complete dataset scan takes about 1 hour. We should be able to improve this performance. For now, it seems to be the slowest acceptable speed. See new endpoint is https://rivers.codefornature.org/api/v2/stream. Currently ```text/csv``` is the only available format and it will trigger a download.

## Query schema ##

The data will be returned in CSV with the header column

```
comid,measurement,variable,year,month,value
```

Data will be returned in order of those fields. 


For filtering the api uses the key words: ```comids```, ```measurements```, ```variables```, ```years```, ```months```. Please be aware that this parameters are plural words since they accepts lists. If a query parameter is not provided, data for all valid data will be returned (unfiltered). 

Allowed values for ```segments``` are comids
Allowed values for ```measurements``` are ```max, mean, median, min```.
Allowed values for ```variables``` are ```estimated, p10, p90, observed```.
Allowed values for ```years``` are the years from 1950 to 2015
Allowed values for ```months``` are 1 .. 12

Examples:

https://rivers.codefornature.org/api/v2/stream/?measurements=max,min&variables=estimated&years=1980,1981&months=5,6,9

This will return estimated maximal and minimal values for 1980 and 1981 in Mai, June, and September for all ~ 130,000 stream segments. Since we scanning about 1/8 of the data that should take about 8 to 10 minutes on a fast Internet connection. 

## A note about speed ##

The data is currently stored in partioned CSV files on the file system. The first two params ```measurements``` and ```variables``` will determine how much of the data will be scanned. The parameters years and months don't have a big influence on the overall download time since the data has to be scanned anyways.
