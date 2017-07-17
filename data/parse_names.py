import pickle
from collections import defaultdict


name_files = ['firstnames.txt', 'surnames.txt']

for namefile in name_files:
    with open(namefile) as f:
        by_name_length = defaultdict(list)
        by_name_letter = defaultdict(list)
        all_names = set()

        for name in f.readlines():
            name = name.strip()
            all_names.add(name)

        for name in all_names:
            by_name_length[len(name)].append(name)
            by_name_letter[name[0]].append(name)

        for namelen, namelist in by_name_length.items():
            by_name_length[namelen] = sorted(namelist)

        for namelen, namelist in by_name_letter.items():
            by_name_letter[namelen] = sorted(namelist)

        # store the two dictionaries as pickle files
        proper_name = namefile[:len(namefile)-4] + '.cache'
        print(proper_name)
        pickle.dump(by_name_length, open('by_length_'+proper_name, 'wb'))
        pickle.dump(by_name_letter, open('by_letter_'+proper_name, 'wb'))

