newsurs = open('newsurnames.txt','w')
names = set()
with open('surnames.txt','r') as f:
    for line in f.readlines():
        names.add(line.strip())
for n in sorted(names):
    if len(n)<13:
        newsurs.write(n + '\n')
newsurs.close()
