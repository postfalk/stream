"""
Remove data entries according issue #59:
ffm in ["peak_dur_2", "peak_dur_5", "peak_fre_2", "peak_fre_5", 
"peak_2", "peak_5", "peak_10"]
AND
source in ["model", "inferred"]
"""
# standard library
import os
# project
import config


def line_iterator(path):
    with open(path) as filehandle:
        for line in filehandle:
            yield line


def filter(line):
    ffms = [
        'peak_dur_2', 'peak_dur_5', 'peak_fre_2', 'peak_fre_5', 
        'peak_2', 'peak_5', 'peak_10']
    parts = line.split(',')
    if not parts[9] in ['inferred', 'model']:
        return True
    if not parts[1] in ffms:
        return True
    print("remove:", line)
    return False


def process(in_path, out_path):
    with open(out_path, 'w') as out_handle:
        for line in line_iterator(in_path):
            if filter(line):
                out_handle.write(line)


def file_iterator():
    """
    Iterate over ffm files.
    """
    filenames = os.listdir(config.OBSERVED_DIRECTORY)
    for filename in filenames:
        yield(filename)


def main():
    os.makedirs(config.OBSERVED_FILTERED, exist_ok=True)
    for filename in file_iterator():
        print(filename)
        process(
            os.path.join(config.OBSERVED_DIRECTORY, filename),
            os.path.join(config.OBSERVED_FILTERED, filename))


if __name__ == "__main__":
    main()
