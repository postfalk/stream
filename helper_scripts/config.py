import os


# BASE_DIR = os.path.abspath(os.path.join(
#    os.path.dirname(__file__), '..', '..', 'data'))
BASE_DIR = os.environ.get(
    'BASE_DIR') or '/Volumes/flow_data/rivers_stream'
ALL_YEAR_DIR = os.path.join(BASE_DIR, 'nhd_ffm_predictions')
ADDITIONAL_DATA_FILES = [
    os.path.join(BASE_DIR, 'ffm_observed_20201208.csv')]
# see issue #48
#ADDITIONAL_DATA_FILES = [
#    os.path.join(
#    BASE_DIR, 'usgs_altered_ffc_percentiles_Updated_Schema_20200410.csv'),
#    os.path.join(
#    BASE_DIR, 'usgs_ref_ffc_percentiles_Updated_Schema.csv')]
WYT_DIR = os.path.join(BASE_DIR, 'nhd_ffm_predictions_wyt')
OUTPUT_DIRECTORY = os.environ.get(
    'OUTPUT_DIRECTORY') or os.path.join(BASE_DIR, 'ffm')
OBSERVED_DIRECTORY = os.environ.get(
    'OBSERVED_DIRECTORY') or os.path.join(BASE_DIR, 'ffm_observed')
OBSERVED_REDUCED = os.environ.get(
    'OBSERVED_REDUCED') or os.path.join(BASE_DIR, 'ffm_observed_reduced')
OBSERVED_FILTERED = os.environ.get(
    'OBSERVED_FILTERED') or os.path.join(BASE_DIR, 'ffm_observed_filtered')
OBSERVED_NEW_PEAK = os.environ.get(
    'OBSERVED_NEW_PEAK') or os.path.join(BASE_DIR, 'ffm_observed_peak')
FFM_REFERENCE = os.path.join(
    BASE_DIR, 'COMID_with_observed_functional_flow_data.csv')
DELTA_COLORADO_REFERENCE = os.path.join(
    BASE_DIR, 'delta_colorado_reference.csv')
NEW_PEAK_FLOW_SOURCE = os.path.join(
    BASE_DIR, 'updates_for_V1_1_0.csv')
REMOVE_COMIDS_SOURCE = [
    'extra_data/Colorado_river_comid_to_remove.csv',
    'extra_data/nhdplus_v2_comid_in_legal_delta.csv']


# Don't transfer these ffms, wyt combinations to the
# final output
BLACK_LIST = [
    ('peak_2', 'dry'),
    ('peak_2', 'moderate'),
    ('peak_2', 'wet'),
    ('peak_5', 'dry'),
    ('peak_5', 'moderate'),
    ('peak_5', 'wet'),
    ('peak_10', 'dry'),
    ('peak_10', 'moderate'),
    ('peak_10', 'wet'),]


UNIT_DIC = {
    'fa_mag': 'cfs',
    'fa_tim': 'water year day',
    'fa_dur': 'days',
    'wet_bfl_mag_10': 'cfs',
    'wet_bfl_mag_50': 'cfs',
    'wet_bfl_dur': 'days',
    'wet_tim': 'water year day',
    'sp_mag': 'cfs',
    'sp_tim': 'water year day',
    'sp_dur': 'days',
    'sp_roc': 'percent',
    'ds_mag_50': 'cfs',
    'ds_mag_90': 'cfs',
    'ds_tim': 'water year day',
    'ds_dur_ws': 'days',
    'peak_2': 'cfs',
    'peak_5': 'cfs',
    'peak_10': 'cfs',
    'peak_dur_2': 'days',
    'peak_dur_5': 'days',
    'peak_dur_10': 'days',
    'peak_fre_2': 'occurrences',
    'peak_fre_5': 'occurrences',
    'peak_fre_10': 'occurrences'
}

FFM_MAPPINGS = {
    'peak_50': 'peak_2',
    'peak_20': 'peak_5',
}

SOURCE_MAPPINGS = {
    'obs\n': 'inferred\n'
}

FFM_OVERWRITE_BY_FILENAME = {
    'Peak_Dur_10_NHD_pred_range.csv': 'peak_dur_10',
    'Peak_Dur_20_NHD_pred_range.csv': 'peak_dur_5',
    'Peak_Dur_50_NHD_pred_range.csv': 'peak_dur_2',
    'Peak_Fre_10_NHD_pred_range.csv': 'peak_fre_10',
    'Peak_Fre_20_NHD_pred_range.csv': 'peak_fre_5',
    'Peak_Fre_50_NHD_pred_range.csv': 'peak_fre_2'
}
