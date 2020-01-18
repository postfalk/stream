"""
Sorting CSV file lines
"""
import os
import config


def sort_file(filename):
    with open(filename) as fil:
        header_line = ''
        lines = []
        first_line = True
        for line in fil:
            if first_line:
                header_line = line
                first_line = False
            else:
                lines.append(line)
    lines.sort()
    with open(filename, 'w') as fil:
        fil.write(header_line)
        for line in lines:
            fil.write(line)


def main():
    filenames = os.listdir(config.OUTPUT_DIRECTORY)
    filenames.sort()
    for filename in filenames:
        print("Sort", filename)
        sort_file(os.path.join(config.OUTPUT_DIRECTORY, filename))


if __name__ == "__main__":
    main()
