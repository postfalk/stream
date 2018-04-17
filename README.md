# Natural Flows API v2 (draft)

## Introduction ##

While the Natural Flow API in its original form https://rivers.codefornature.org/ is well suited for complex queries and a limited number of returned records we encountered two major challenges:

1. The MongoDB-based query schema is cumbersome, especially the combined use of queries and projection is very flexible but too complicated for most of our potential users. This could be improved once the project team could settle on a simple schema that would support most of the use cases.

2. The first iteration of the API runs into limits when a query results in high numbers of returned records, which manifest themselves in timeouts or memory overruns (causing 500 or 502 errors). The application works well for limited downloads up to 2,000 stream segments, e.g. for all gauged streams but it will break for larger geographies like the L.A. basin. The current workaround is a pagination schema that adds to complexity.

The new version of the API will split querying into two different and entirely separate pieces.

1. An API that allows for querying stream segments on the basis of NHDv2 attributes such as name, geography, stream order, gauging, etc. 
2. An API that allows for filtering of the Natural Flow dataset based on a list of stream segments (which can be obtained as a return of 1.) as well as simple filters that subset the data by segments, measurements, variables, months, and dates. 

The design decision to keep the two datasets (NHDv2 and USGS/TNC Natural Flows) separate guarantees a cleaner, more perfomant API, requires less data manipulation, and allows for different providers for the NHDv2 data (e.g. Excel files, Map services, or the first iteration of the API).

The second API has been creates as a powerful application based on Scala, Play, and Akka that allows for streaming and filtering of the entire dataset (47.3 GigaByte) without https timeouts or memory overruns. With the current production setup the complete dataset scan takes about one hour if the Internet speed allows. (I get this time in the office but not on my home Internet). We should be able to improve this performance. For now, it seems to be the slowest acceptable speed. 

The endpoint for the data API is https://rivers.codefornature.org/api/v2/stream/. The content-type ```text/csv``` is currently the only available format. In Typing the URL into the address bar of a browser will trigger a download. Since it is a streaming application, the overall size of the download will be unknown before finished.

## Outlook ##

We will develop an application that will allow users to combine the two API queryies in the workflow supported by a webmap-based GUI. For example a suer could select a list of river segments (i.e. comids) by clicking on a watershed and then downloading a flow dataset for that selection.

## Query schema ##

The data will be returned in CSV (```Content-Type: text/csv```) with the header row

```
comid,measurement,variable,year,month,value
```

Data will be orderd by these fields. 

For filtering the api uses the key words: ```segments```, ```measurements```, ```variables```, and ```months```. Please be aware that all these parameters are plural words since they accepts lists (comma-separated lists without spaces). If a query parameter is not provided, data for all valid values will be returned (unfiltered). 

In addition you can use ```begin_year``` and ```end_year``` to limit the years returned. In this case only one value is allowed

Allowed values for ```segments``` are comids.
Allowed values for ```measurements``` are ```max```, ```mean```, ```median```, ```min```.
Allowed values for ```variables``` are ```estimated```, ```p10```, ```p90```, ```observed```.
Allowed values for ```begin_years``` and ```end_years``` are the years from 1950 to 2015
Allowed values for ```months``` are 1 .. 12

Examples:

https://rivers.codefornature.org/api/v2/stream/?measurements=max,min&variables=estimated&months=5,6,9&begin_year=1980&end_year=1981

This will return estimated maximal and minimal values for 1980 and 1981 in Mai, June, and September for all ~ 130,000 stream segments. Since we scanning about 1/8 of the data that should take about 8 to 10 minutes on a fast Internet connection. 

For a single stream segment:

https://rivers.codefornature.org/api/v2/stream/?segments=10000042&values=estimated

## A note on speed ##

The data is currently stored in partioned CSV files on the filesystem. The first two params ```segments```, ```measurements``` and ```variables``` will determine how many of the roughly 6000,000 files will be scanned by the application. The parameters years and months don't have a large influence on the overall download time since the entire files have to be scanned no matter how big the subset.

In general, best performance can be achieved by limiting the download as much as possible to the required subset. Be also aware that there is a maximimum of 1,048,576 rows that can be opened in Microsoft Excel. One stream segments provides 9,504 or 12,672 data points (depending on the absence or presence of observed data) which means that only about 100 stream segments can be loaded if not further filtered down.

## Outstanding features ##

1. Rate limits/throttling based on authorization.
2. Some ```offset``` and ```limit``` parameters for paginated downloads.
3. Support for very long lists of comids that cannot be represented within a 4kByte GET request. (Implement POST, request by comid list from file).
