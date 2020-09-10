"""
Merges new observed data. This is for sure a little bit messy because of
Corona Virus crises but for now the quickest way to get the data in.

Run this script after ffm.py
"""
# standard library
import os
# project
import config


def find_duplicates(first, second):
    first_parts = first.split(',')
    second_parts = second.split(',')
    first_value = (
        first_parts[1] + first_parts[2] + first_parts[10] +
        first_parts[11] + first_parts[12])
    second_value = (
        second_parts[1] + second_parts[2] + second_parts[10] +
        second_parts[11] + second_parts[12])
    return (first_value == second_value)


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
                if duplicate:
                    print('Duplicates removed', path, '\n', old_line, line)
                old_line = line
                first_line = False
                if not duplicate:
                    output_file.write(line)
    else:
        print('Skip', path)


def add_observed():
    for item in config.ADDITIONAL_DATA_FILES:
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
                    first_record = False
                    old = parts[0]
                    lines = []
                    modelled_filename = os.path.join(
                        config.OUTPUT_DIRECTORY, parts[0] + '.csv')
                    with open(modelled_filename) as modelled_file:
                        lines = [lin for lin in modelled_file]
                lines.append(line)
            save_file(parts[0], lines)


def add_all_others():
    """
    Add and reformat all other files
    """
    files = os.listdir(config.OUTPUT_DIRECTORY)
    for filename in files:
        in_path = os.path.join(config.OUTPUT_DIRECTORY, filename)
        comid = filename.replace('.csv', '')
        with open(in_path) as fil:
            lines = [line for line in fil]
        save_file(comid, lines, overwrite=False)


def main():
    add_observed()
    add_all_others()


if __name__ == '__main__':
    main()
