# Natural Flows API v2 (draft)

## Introduction ##

While the Natural Flow API in its original form https://rivers.codefornature.org/ is well suited for complex queries and a limited number of returned records we encountered two major challenges:

1. The MongoDB-based query schema is cumbersome, especially the combined use of queries and projection is complicated. This can be improved if the project team will settle on a simpler schema that meets pre-defined reuqirements.
2. The first iteration of the API runs into limits when a query results in high numbers of returned records, which manifest themselves in timeouts or memory overrun. The application works well for limited downloads up to 2,000 stream segments, e.g. for all gauged streams but it will break for larger geographies like the L.A. basin. The current work around is a pagination schema that adds to complexity.

The new version of the API will split querying into two different and entirely separate pieces.

1. An API that allows for querying stream segments on the basis of NHDv2 attributes such as name, geography, stream order, gauging, etc. 
2. An API that allows for filtering of the Natural Flow dataset based on a list of stream segments (which can be a return of 1.) as well as simple filters that subset the data by measurements, variables, months, and dates. 

The design decision to keep the two datasets involved (NHDv2 and USGS/TNC Natural Flows) separate guarantees a cleaner, more perfomant API, requires less data manipulation, and allows for different providers for the NHDv2 data (e.g. Excel files, Map services, or the first iteration of the API)

For the second step we build a powerful application based on Scala, Play, and Akka that allows for streaming and filtering of the entire dataset (47.3 GigaByte) without https timeouts or memory overruns. In the current production setup the complete dataset scan takes about 1 hour if the Internet speed allows. (I get this time in the office but not on my home Internet). We should be able to improve this performance. For now, it seems to be the slowest acceptable speed. See new endpoint is https://rivers.codefornature.org/api/v2/stream. Currently ```text/csv``` is the only available format and it will trigger a download.

We are developing an application that allows user to combine the two workflow steps based on a webmap-based GUI. A workflow example would be, selecting a list of river segments, e.g. by clicking on a watershed and then requesting flow data for this watershed by selecting what measurements and variables are desired before downloading the dataset.

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

or for a single stream segment

https://rivers.codefornature.org/api/v2/stream/?segments=1000042

This will return estimated maximal and minimal values for 1980 and 1981 in Mai, June, and September for all ~ 130,000 stream segments. Since we scanning about 1/8 of the data that should take about 8 to 10 minutes on a fast Internet connection. 

## A note about speed ##

The data is currently stored in partioned CSV files on the filesystem. The first two params ```segments```, ```measurements``` and ```variables``` will determine how much of the data will be scanned. The parameters years and months don't have a big influence on the overall download time since the data has to be scanned anyways.

