# Natural Flows API v2 (draft)

## Introduction ##

The Natural Flow API v1 (https://rivers.codefornature.org/) is well suited
for complex queries with a limited number of returned records. However, there 
have been two challenges.

1. The query schema is cumbersome, the combined use of queries and projection
is very flexible but turned out to be too complicated for non-coding users.

2. Queries with a large number of returned records run into time-outs and
memory overruns, manifesting themselves in HTTP 500 and 502 errors.

The version 2 of the flow data API splits querying into two separate phases.

1. An API that allows for querying stream segments by NHDv2 attributes, e.g.
comids, river name, geography, stream order, gauging, etc.

2. An API that supports filtering of the Natural Flow dataset created by
TNC and USGS. It will mainly support querying by lists of comids
that can be obtained from 1 or otherwise. It provides filters for certain
statistics, variables, years, and months.

The decision to keep the two datasets (NHDv2 and USGS/TNC Natural Flows)
separate will guarantee a cleaner, more performative API, requires less
data manipulation.

The second API uses a stack of [Scala](https://www.scala-lang.org/), 
[Play](https://www.playframework.com/), and [Akka](https://akka.io/) 
allowing for streaming and filtering of the entire dataset without 
timeouts or memory overruns. 

With the current production setup a complete dataset scan takes about one hour
if the Internet speed allows. 

The endpoint for the data API is 
https://rivers.codefornature.org/api/v2/stream/. The only available Content-type is
```text/csv``` is the. Typing the URL into the address bar of a browser
will trigger a download. 

Since it is a streaming application, the overall size of the download will be
unknown until finished.

## Outlook ##

We are developing an application for easily interface with the to step 
workflow trough a web-based application. An typical example workflow could be:
A user selects a list of stream segments (comids) by clicking on a watershed and 
then download a dataset for that selection.

## Variables in the dataset ##

(Coming soon)

## Query schema ##

The data will be returned in CSV (```Content-Type: text/csv```) with the header row

```
comid,statistics,variable,year,month,value
```

The data ata will be orderd by these fields.

For filtering the query parameters ```comids```, ```statistics```,
```variables```, and ```months``` are available. Please note that all of these
parameters are plural words since they accepts lists (comma-separated lists
without spaces). If a query parameter is NOT provided, data for all valid
values of this parameter will be returned (unfiltered is the default).

Additional query parameters are ```begin_year``` and ```end_year``` to limit
range of years returned. For these parameter only one valid is allowed.

Allowed values for ```comids``` are comids as used in the NHVD v2 dataset.

Allowed values for ```statistics``` are ```max```, ```mean```,
```median```, ```min```.

Allowed values for ```variables``` are ```estimated```, ```p10```,
```p90```, ```observed```.

Allowed values for ```begin_year``` and ```end_year``` are 1950 to 2015

Allowed values for ```months``` are 1 .. 12

Examples:

https://rivers.codefornature.org/api/v2/stream/?statistics=max,min&variables=estimated&months=5,6,9&begin_year=1980&end_year=1981

This will return estimated maximal and minimal values for 1980 and 1981 for
the months of May, June, and September for all ~ 130,000 stream segments
(or comids). Since about 1/8 of the data will be returned the download
should take about 8 to 10 minutes on a reasonably fast Internet connection.

For a single stream segment:

https://rivers.codefornature.org/api/v2/stream/?comids=10000042&variables=estimated

## A note on speed ##

The data is currently stored in partioned CSV files on the filesystem.
The first three query parameters ```comids```, ```statistics``` and
```variables``` will determine how many of the roughly 600,000 files
will be scanned by the application. 

The use of the parameters years and months doesn't have influence on
the overall download time since entire files have to be scanned
no matter how big the subset will be. It might be an advantage to
filter on a very slow Internet connection.

Limiting the download as much as possible and only requesting data
as needed will provide the best performance. Be also aware that there
is a maximimum of 1,048,576 rows that can be opened in Microsoft Excel.
One comid provides 9,504 or 12,672 data points (depending on
the absence or presence of observed data) which means that only about
100 stream segments can be loaded if not further filtered down.

## Outstanding features ##

1. Some ```offset``` and ```limit``` parameters for paginated downloads.
2. Support for very long lists of comids that cannot be represented 
within a 4kByte GET request. (Implement POST, request by comid list from file).
