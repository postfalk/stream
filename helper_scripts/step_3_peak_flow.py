# standard library
import os
# project
import config


def line_generator():
    first_line = True;
    with open(config.NEW_PEAK_FLOW_SOURCE) as handle:
        for line in handle:
            if first_line:
                first_line = False
                continue
            yield(line)


def get_comid(line):
    return line.split(',')[0]


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
        process(line)


if __name__ == "__main__":
    main()
