from fuzzywuzzy import fuzz, process
import pickle
import os

def load_cache(file_name):
    return pickle.load(open(
        os.path.join('data',file_name), 'rb'))


fn_letter = load_cache('by_letter_firstnames.cache')
fn_length = load_cache('by_length_firstnames.cache')
sn_letter = load_cache('by_letter_surnames.cache')
sn_length = load_cache('by_length_surnames.cache')

def best_match(name, namelist):
    _score = -1
    _match = ''
    for n in namelist:
        fuzzscore = fuzz.ratio(name, n)
        if fuzzscore > _score:
            _score = fuzzscore
            _match = n
    return _match


def compare_score(original, fuzz1, fuzz2):
    score1 = fuzz.ratio(original, fuzz1)
    score2 = fuzz.ratio(original, fuzz2)
    print(original+' had the following matches: ' +
          fuzz1+'('+str(score1)+'), '+fuzz2+'(' +
          str(score2)+')')
    best = fuzz1 if score1 > score2 else fuzz2
    print('Best match: '+best)
    return best


def handle_name(name):
    name = name.strip().title()
    first = ''
    sur = ''
    splitted = name.split(' ', 1)
    if len(splitted) > 1:
        # print('More than one name!')
        first = splitted[0]
        sur = ''.join(splitted[1])
    else:
        first = name
    all_names = []
    print('First name: '+first)
    print('Middle/last names: '+sur+'\n')
    firstchar = first[0]
    namelen = len(first)
    best_letter_match = best_match(first, fn_letter[firstchar])
    # print('Best letter match: ' + best_letter_match)
    best_length_match = best_match(first, fn_length[namelen])
    # print('Best length match: ' + best_length_match)
    best_first = compare_score(first, best_letter_match, best_length_match)
    all_names.append(best_first)

    for s in sur.split():
        len_match = best_match(s, sn_length[len(s)])
        # print('Best length match for '+s+': '+len_match)
        let_match = best_match(s, sn_letter[s[0]])
        # print('Best letter match for '+s+': '+let_match)
        best_score = compare_score(s, len_match, let_match)
        all_names.append(best_score)
    new_name = ' '.join(all_names)
    # print('Fuzzy name: '+new_name)
    print(name + ' --> '+new_name)


def main():
    while True:
        name = input('Some name: ')
        handle_name(name)


if __name__ == '__main__':
    main()
