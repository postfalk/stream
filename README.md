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

For the second step we build a powerful streaming up based on Scala, Play, and Akka that allows for streaming and filtering of the entire data set (47.3 GigaByte) without https timeouts or memory overrun. In current production setup the complete dataset scan takes about 1 hour. We should be able to improve this performance. For now, it seems to be the slowest acceptable speed.
