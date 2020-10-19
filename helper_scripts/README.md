# Adding some helper scripts for data serialization

Using Python for speed and convenience

Processing order:

1. ffm.py: format modelled data to be served in a streaming API
2. merge_observed_ffm.py: merge observed data
3. remove_inferred.py: remove data according issue #59
4. peak_flow.py: add new peak flow
5. remove_comids.py: remove comids we don't want to display because of questionable data
