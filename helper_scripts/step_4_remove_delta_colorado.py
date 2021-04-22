"""
This script removes data we don't trust
- legal delta
- Colorado river basin
"""
# standard library
from csv import DictReader
import os
# project
import config


def comid_generator():
    for item in config.REMOVE_COMIDS_SOURCE:
        with open(item) as handle:
            reader = DictReader(handle)
            for row in reader:
                yield row.get('COMID')


def main():
    with open(config.DELTA_COLORADO_REFERENCE, 'w') as handle:
        for item in comid_generator():
            handle.write(item + '\n')
            path = os.path.join(
                config.OBSERVED_REDUCED, '{}.csv'.format(item))
            try:
                os.remove(path)
                print("Remove", path)
            except FileNotFoundError:
                pass


if __name__ == "__main__":
    main()
