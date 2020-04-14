import os


BASE_DIR = os.path.abspath(os.path.join(
    os.path.dirname(__file__), '..', '..', 'data'))
ALL_YEAR_DIR = os.path.join(BASE_DIR, 'nhd_ffm_predictions')
WYT_DIR = os.path.join(BASE_DIR, 'nhd_ffm_predictions_wyt')
OUTPUT_DIRECTORY = os.path.join(BASE_DIR, 'ffm')


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
