"""
Deduplicate modelled FFM
"""
# standard library
import os
from datetime import datetime
# project
import config


break_counter = -1


def file_iterator(directory):
    """
    Iterate over files
    """
    files = os.listdir(directory)
    for file in files:
        yield(os.path.join(directory, file))


def line_iterator(path):
    """
    Iterate over lines in a files
    """
    with open(path) as handle:
        return handle.readlines()


def file_to_dic(path):
    """
    Write all lines to a dict where the key defines the criterion for uniqueness
    """
    dic = {}
    for line in line_iterator(path):
        key = '_'.join(line.split(',')[1:3])
        dic[key] = line
    return dic


def dic_to_file(dic, path):
    """
    Write results back to files
    """
    lst = []
    for key, value in dic.items():
        lst.append(value)
    lst.sort()
    with open(path, 'w') as handle:
        for item in lst:
            handle.write(item)


def check_file(path):
    """
    Check for duplicates to decide whether to process file
    """

    with open(path) as handle:
        lst = ['_'.join(line.split(',')[1:3]) for line in handle.readlines()]
        aset = set(lst)
    return len(lst) != len(aset)


def process_file(path):
    """
    Process a single files
    """
    if check_file(path):
        print('Duplicates detected')
        dic = file_to_dic(path)
        dic_to_file(dic, path)


if __name__ == '__main__':
    print('Remove duplicates')
    start = datetime.now()
    for item in file_iterator(config.OUTPUT_DIRECTORY):
        process_file(item)
        if not break_counter:
            break
        if not break_counter % 1000:
            print(abs(break_counter))
            print(datetime.now() - start)
        break_counter-=1
