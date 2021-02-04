"""
A script to re-serialize functional flow metrics files
"""
# This is not particularly pretty
# TODO: Improve if not a one-off
import os
import re
import shutil
import config


BREAK_AFTER = -1


def check_wyt(first_line):
    parts = first_line.split(',')
    return '"WYT"' in parts


def format_digits(text_field):
    """
    Formatting numbers to three valid digits (including trailing
    zeros). This seems to be terrible and slow. Improve
    """
    ret = '{}'.format(float('%.3g' % float(text_field)))
    length = 5 if ret[0] == '-' else 4
    while len(ret) < length:
        if not '.' in ret:
            ret += '.'
        else:
            ret += '0'
    while len(ret) > length and '.' in ret and ret[-1] == '0':
        ret = ret[:-1]
    if ret[-1] == '.':
        ret = ret[:-1]
    return ret


def process(pathname):
    print('processing', pathname)
    first_line = True
    # we need this for ffm name override from filename
    filename = os.path.split(pathname)[1]
    ct = BREAK_AFTER
    with open(pathname) as fil:
        for line in fil:
            ct -= 1
            if ct == 0:
                break
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
                    if float(parts[idx]) < 0:
                        parts[idx] = '0'
                    parts[idx] = format_digits(parts[idx])
                # put the comid first
                parts[1] = parts[0]
                parts[0] = comid
                # lower case text fields
                for idx in [1, 2, 8]:
                    parts[idx] = parts[idx].lower()
                # remap variable names
                parts[1] = config.FFM_MAPPINGS.get(parts[1], parts[1])
                # override ffm by filename
                parts[1] = config.FFM_OVERWRITE_BY_FILENAME.get(
                    filename, parts[1])
                # change "obs" to "observed"
                parts[8] = config.SOURCE_MAPPINGS.get(parts[8], parts[8])
                parts = parts[0:8] + [
                    config.UNIT_DIC.get(parts[1], '')] + [parts[8]]
                out_line = ','.join(parts)
                # add additional fields not used by modelled values
                out_line = out_line.replace('\n', ',,,,,\n')
                out_file_name = os.path.join(
                    config.OUTPUT_DIRECTORY, comid + '.csv')
                # print(out_line, end='')
                # print(parts[1], parts[2])
                if not (parts[1], parts[2]) in config.BLACK_LIST:
                    with open(out_file_name, 'a') as out:
                        out.write(out_line)
                else:
                    print('SKIP', (parts[1], parts[2]))


def main():
    print('Input directory', config.WYT_DIR)
    print('Output directory', config.OUTPUT_DIRECTORY)
    shutil.rmtree(config.OUTPUT_DIRECTORY, ignore_errors=True)
    os.makedirs(config.OUTPUT_DIRECTORY, exist_ok=True)
    file_list = []
    for item in [config.ALL_YEAR_DIR, config.WYT_DIR]:
        file_list += [
            os.path.abspath(os.path.join(item, entry))
            for entry in os.listdir(item)]
    files = [fil for fil in file_list if os.path.splitext(fil)[1] == '.csv']
    files.sort()
    for filename in files:
        process(filename)


if __name__ == '__main__':
    main()
