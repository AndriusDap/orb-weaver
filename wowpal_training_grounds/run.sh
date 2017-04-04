#!/usr/bin/env bash
vw\
	 -d train\
	 --normalized\
	 -f model.vw\
	 -k\
	 --binary\
	 --link=logistic\
	 --loss_function logistic\
	 -b 25\
	 -c\
	 --passes 20\
	 --l2 1e-6 \
	 --quiet 
echo
echo
echo "========================== Perf Test ======================"
echo
echo
vw -d test -t  --binary -i model.vw -r results.txt --quiet

perf \
 -all  -auc \
 -files test_labels results.txt

