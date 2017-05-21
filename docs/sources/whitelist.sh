zcat index-000* | parallel --pipe \
 "awk 'BEGIN {srand()} { if (rand() <= 0.0005798) {print $1}}\'" > \
   /media/ssd/final_data/whitelist.txt