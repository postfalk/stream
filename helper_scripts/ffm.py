"""
A script to serial the functional flow metrics to csv
"""
# This is not particularly pretty
# TODO: Improve if it will not be a one-off
import os
import re
import shutil


BASE_DIR = os.path.join(
    os.path.dirname(__file__), '..', '..', 'data')
ALL_YEAR_DIR = os.path.join(BASE_DIR, 'nhd_ffm_predictions')
WYT_DIR = os.path.join(BASE_DIR, 'nhd_ffm_predictions_wyt')
OUTPUT_DIRECTORY = os.path.join(BASE_DIR, 'ffm')


def check_wyt(first_line):
    parts = first_line.split(',')
    return '"WYT"' in parts


def format_digits(text_field):
    res = re.search('^\d*([.]\d{0,2})?', text_field)
    return res.group(0)


def process(filename):
    print('processing', filename)
    first_line = True
    with open(filename) as fil:
        for line in fil:
            if first_line:
                wyt_present = check_wyt(line)
                first_line = False
            else:
                out_line = ''
                line = line.replace('"', '')
                parts =  line.split(',')
                comid = parts[1]
                if wyt_present:
                    pass
                else:
                    # fill in all as water year type
                    parts = line.split(',')
                    parts = parts[0:2] + ['All'] + parts[2:]
                # use only two digits after the decimal point
                for idx in range(3, 8):
                    parts[idx] = format_digits(parts[idx])
                # put the comid first
                parts[1] = parts[0]
                parts[0] = comid
                # lower case text fields
                for idx in [1, 2, 8]:
                    parts[idx] = parts[idx].lower()
                # change "obs" to "observed"
                if parts[8] == 'obs\n':
                    parts[8] = 'observed\n'
                out_line = ','.join(parts)
                out_file_name = os.path.join(OUTPUT_DIRECTORY, comid + '.csv')
                with open(out_file_name, 'a') as out:
                    out.write(out_line)

def main():
    shutil.rmtree(OUTPUT_DIRECTORY)
    os.makedirs(OUTPUT_DIRECTORY, exist_ok=True)
    file_list = []
    for item in [ALL_YEAR_DIR, WYT_DIR]:
        file_list += [
            os.path.abspath(os.path.join(item, entry))
            for entry in os.listdir(item)]
    files = [fil for fil in file_list if os.path.splitext(fil)[1] == '.csv']
    files.sort()
    for filename in files:
        process(filename)


if __name__ == '__main__':
    main()
