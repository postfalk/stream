# standard library
import os
# project
import config
from step_1_ffm import format_digits


def line_generator():
    first_line = True;
    with open(config.NEW_PEAK_FLOW_SOURCE) as handle:
        for line in handle:
            if first_line:
                first_line = False
                continue
            parts = line.replace('\n', '').split(',')
            for idx, item in enumerate(parts[3:8]):
                parts[idx+3] = format_digits(item)
            while len(parts) < 15:
                parts += ['']
            yield(','.join(parts) + '\n')


def get_comid(line):
    return line.split(',')[0]z


def process(line):
    comid = get_comid(line)
    path = os.path.join(config.OBSERVED_FILTERED, comid + '.csv')
    print(comid)
    with open(path) as handle:
        lines = [ln for ln in handle]
    if line not in lines:
        lines.append(line)
    lines.sort()
    with open(path, 'w') as handle:
        for line in lines:
            handle.write(line)


def main():
    for line in line_generator():
        # print(line)
        process(line)


if __name__ == "__main__":
    main()
