"""
Merges new observed data.
Run this script after step_1_ffm.py
"""
# standard library
import os
import shutil
# project
import config
from step_1_ffm import format_digits


def find_duplicates(first, second):
    """
    Check whether two lines can be considered duplicates
    """
    first_parts = first.split(',')
    second_parts = second.split(',')
    first_value = first_parts[0] + first_parts[1] + first_parts[2]
    second_value = second_parts[0] + second_parts[1] + second_parts[2]
    return first_value == second_value


def fill_commas(line):
    ct = len(line.split(','))
    line = line.replace('\n', '')
    for item in range(13 - ct):
        line = line + ','
    return line + '\n'


def save_file(comid, lines, overwrite=True):
    os.makedirs(config.OBSERVED_DIRECTORY, exist_ok=True)
    path = os.path.join(config.OBSERVED_DIRECTORY, comid + '.csv')
    lines.sort()
    if (not os.path.isfile(path)) or overwrite:
        print('Write', path, len(lines), 'records')
        old_line = ''
        first_line = True
        duplicate = False
        with open(path, 'w') as output_file:
            for line in lines:
                line = fill_commas(line)
                if not first_line:
                    duplicate = find_duplicates(old_line, line)
                first_line = False
                if duplicate:
                    # print('Duplicates removed', path, '\n', old_line, line)
                    pass
                else:
                    output_file.write(line)
                old_line = line
    else:
        print('Skip', path)


def add_observed(observed_data=[]):
    for item in observed_data:
        print('Process', item)
        with open(item) as observed_file:
            old = ''
            first_line = True
            first_record = True
            lines = []
            for line in observed_file:
                # skip the first header line
                if first_line:
                    first_line = False
                    continue
                # start over when the comid changes
                parts = line.split(',')
                if old != parts[0]:
                    if not first_record:
                        save_file(old, lines)
                        pass
                    first_record = False
                    old = parts[0]
                    lines = []
                    modelled_filename = os.path.join(
                        config.OUTPUT_DIRECTORY, parts[0] + '.csv')
                    with open(modelled_filename) as modelled_file:
                        lines = [lin for lin in modelled_file]
                for idx, part in enumerate(parts[3:8]):
                    parts[idx+3] = format_digits(part)
                new_line = ','.join(parts)
                lines.append(new_line)
            save_file(parts[0], lines)


def main():
    print('Making a copy of the data from prior step')
    #shutil.copytree(
    #    config.OUTPUT_DIRECTORY, config.OBSERVED_DIRECTORY,
    #    dirs_exist_ok=True)
    print('Applying following files')
    print(config.ADDITIONAL_DATA_FILES)
    print('Add observed data')
    add_observed(observed_data=config.ADDITIONAL_DATA_FILES)


if __name__ == '__main__':
    main()
