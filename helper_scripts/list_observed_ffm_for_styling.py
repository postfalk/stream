"""
Extract comids for ffm observed stream segment styling
"""
import config


def main():
    print('Extract observed ffm')
    res = set()
    for filename in config.ADDITIONAL_DATA_FILES:
        first_line = True
        with open(filename) as filehandler:
            for line in filehandler:
                if first_line:
                    first_line = False
                else:
                    comid = line.split(',')[0]
                    res.add(comid)
    res_list = list(res)
    res_list.sort()
    print(config.FFM_REFERENCE)
    with open(config.FFM_REFERENCE, 'w') as reference:
        for item in res_list:
            reference.write(item)
            reference.write('\n')


if __name__ == '__main__':
    main()
