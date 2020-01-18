# Natural Flows API v2 (draft)

## Introduction

The Natural Flow API v1 (https://flow-api.codefornature.org/ or https://rivers.codefornature.org) is well suited
for complex queries with a limited number of returned records. However, we
ran into two major challenges.

1. The (MongoDb-based) query schema is cumbersome, the combined use of queries
and projection is very flexible but turned out to be too complicated for
non-coding users.

2. Queries with a large number of returned records run into time-outs and
memory overruns, manifesting themselves in HTTP 500 and 502 errors.

The version 2 of the flow data API splits querying into two separate phases:

1. An API that allows for querying stream segments by NHDv2 attributes, e.g.
comids (unique ID for each stream segment), river name, geography, stream order, gauging, etc. Version 1 or any
GIS layer containing comids can serve this purpose.

2. An API that supports filtering of the Natural Flow dataset created by
TNC and USGS. It will mainly support querying by lists of comids
that can be obtained from 1 or otherwise. It provides filters for
statistics, variables, years, and months.

The decision to keep the two datasets (NHDv2 and USGS/TNC Natural Flows)
separate guarantees a cleaner and more performative API.

API v2 uses [Scala](https://www.scala-lang.org/),
[Play](https://www.playframework.com/), and [Akka](https://akka.io/)
allowing for streaming and filtering of the entire dataset without
timeouts or memory overruns.

A complete scan/download of the Natural Flow dataset (not subsetted
by comid and including all variables) will take about an hour and yields
~ 1 billion records and 47 Gbyte of data.

The endpoint for the API v2 is
https://flow-api.codefornature.org/api/v2/stream/.

Currently, the only available response Content-type is ```text/csv```.
Transfer-encoding is ```chunked```. Additional Content-types could be added
as long as they support chunked transfer (pure JSON does not since it requires a
root block, for alternatives see https://en.wikipedia.org/wiki/JSON_streaming).
The response will be compressed (```Content-encoding: gzip;```).

Typing the URL into the address bar of a web browser will trigger a download.

Since the data will be streamed, the overall size of the download will be unknown
until finished. Nevertheless, the relatively uniform length of records allows for
reasonable estimates.

## Outlook

A web application will serve as interface. An typical example workflow could be:
A user selects a list of stream segments (comids) by clicking on a watershed and
then download a dataset for that selection. For a prototype see

https://s3.amazonaws.com/rivers.codefornature.org/index.html

## Variables in the dataset

Allowed values for ```variables``` are ```estimated```, ```p10```,
```p90```, ```observed```.

```estimated``` is the predicted natural or unimpaired flow in cubic feet per second based on the mean of 1,000 predictions from the random forest models.  
```p10``` is the lower confidence bound of natural or unimpaired flow in cubic feet per second based on the 10th percentile of 1,000 predictions from the random forest models.  
```p90``` is the upper confidence bound of natural or unimpaired flow in cubic feet per second based on the 90th percentile of 1,000 predictions from the random forest models.
```observed``` is the measured flow in cubic feet per second based measurements taken at stream gages managed by the U.S. Geological Survey.  Observed data is only available for the subset of stream segments with active or historical stream gages.

## Statistics in the dataset

Allowed values for ```statistics``` are ```max```, ```mean```,
```median```, ```min```.

```max``` is the maximum of daily flows for a given month (for observed data, months with <20 daily flows were excluded)
```mean``` is the mean of daily flows for a given month.  
```median``` is the median of daily flows for a given month.
```min``` is the minimum of daily flows for a given month (for observed data, months with <20 daily flows were excluded)

## Query schema: GET requests

The data will be returned in CSV (```Content-Type: text/csv```) with the header row

```
comid,statistics,variable,year,month,value
```

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

https://flow-api.codefornature.org/api/v2/stream/?statistics=max,min&variables=estimated&months=5,6,9&begin_year=1980&end_year=1981

This will return estimated maximal and minimal values for 1980 and 1981 for
the months of May, June, and September for all ~ 130,000 stream segments
(or comids). Since about 1/8 of the data will be returned the download
should take about 8 to 10 minutes on a reasonably fast Internet connection.

For a single stream segment:

https://flow-api.codefornature.org/api/v2/stream/?comids=10000042&variables=estimated

## POST requests

GET requests are limited by the 2047 characters that can be represented in an URLs.
Many use cases may exceed this limit, e.g. getting all stream segments for
an entire watershed. In that case the data can be downloaded via a POST request
with the query data in the body.

Currently, the Content-types ```application/x-www-form-urlencoded``` and ```application/json```
are supported. ```application/x-www-form-urlencoded```  is the default and can be requested
from html forms (without the ```enctype``` attribute).
The query schema is identical to GET requests.

Since the response Content-type is ```text/csv``` a form submissions in which the
```action``` attribute is set to the url will trigger a download dialogue
(tested in Chrome and Firefox).

An example download form could look like this:

```html

<form action="https://flow-api.codefornature.org/api/v2/stream/" method="post">

  <!-- Use type=hidden for values preselected by app interactions -->
  <input type="hidden" name="comids" value="10000042,10000688">

  <!-- Use same name (not id!) for multiple choice -->
  <span>Statistics:</span>

  <label for="min">Min</label>
  <input id="min" type="checkbox" name="statistics" value="min">

  <label for="mean">Mean</label>
  <input id="mean" type="checkbox" name="statistics" value="mean" checked="true">

  <label for="median">Median</label>
  <input id="median" type="checkbox" name="statistics" value="median">

  <label for="max">Max</label>
  <input id="max" type="checkbox" name="statistics" value="max">

  <!-- add additional fields here -->

  <button type="submit">Download</button>

</form>

```

If POST requests are submitted with ```Content-type: application/json;``` the request body
might look like this:

```json
{
    "comids": [
        10000042,
        10000688
     ],
     "statistics": [
         "min",
         "max"
     ],
     "variables": [
         "estimated"
     ],
     "begin_year": 1980,
     "end_year": 1981,
     "months": [
          1,
          2
     ]
}
```

## A note on speed

The data is stored in partioned CSV files on the filesystem.
The first three query parameters ```comids```, ```statistics``` and
```variables``` will determine how many of the roughly 600,000 files
will be scanned by the application.

The use of the parameters years and months doesn't influence the
overall download much time since entire files have to be scanned
no matter how big the subset will be.

Limiting the download as much as possible and only requesting data
as needed will provide the best performance. Be also aware that there
is a maximimum of 1,048,576 rows that can be opened in Microsoft Excel.
One comid provides 9,504 or 12,672 data points (depending on
the absence or presence of observed data) which means that only about
100 stream segments can be loaded if not further filtered down.

## Outstanding features ##

1. Some ```offset``` and ```limit``` parameters for paginated downloads.
2. Support additional Content-types for POST (```application/json```. ```text/plain```)
3. Experiment with Apache Kafka, Parquet, and compression for better, cheaper,
and faster data storage.
4. Re-implement ```years```

## Functional Flows endpoint (draft) ##

I implemented a first version of the functional flow endpoint where the query schema is very similar and it supports either GET or POST with a json body (see above for details). The endpoint is

https://flow-api.codefornature.org/api/v2/ffm/

While querying without parameters it will return the empty csv schema:

```
comid,ffm,wyt,p10,p25,p50,p75,p90,unit,source
```

Provide a list of comids to get data:

https://flow-api.codefornature.org/api/v2/ffm/?comids=10000042,10000688

The supported query fields are:

- ```ffms``` ... list of functional flow metrics to return with the value ds_dur_ws, ds_mag_50, ds_mag_90, ds_tim, fa_mag, fa_tim, peak_10, peak_2, peak_5, peak_dur_10, peak_dur_2, peak_dur_5, peak_fre_10, peak_fre_2, peak_fre_5, sp_dur, sp_mag, sp_tim, wet_bfl_dur, wet_bfl_mag_10, wet_bfl_mag_50, wet_tim" (not all values are available for every stream segment)
- ```wyts``` ... water year types: all, dry, moderate, and wet (where all means the average of all year types and NOT all water year types
- ```sources``` ... with the values model or inferred (I guess not very useful)

e.g.

https://flow-api.codefornature.org/api/v2/ffm/?comids=0&ffms=ds_dur_ws&wyts=dry

To get the dry season duration values for dry water years for all stream segments.

