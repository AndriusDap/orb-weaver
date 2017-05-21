#!/usr/bin/env bash
vw\
	 -d training_file.vw\
	 --normalized\
	 -f model.vw\
	 -k\
	 --binary\
	 -b 25\
	 -c\
	--log_multi 20\
	 --passes 20\
	 --l2 1e-6 
echo
echo
echo "========================== Perf Test ======================"
echo
echo
vw -d training_file.vw -t  --binary -i model.vw -r results.txt # --quiet

perf \
 -all  -auc \
 -files test_labels.txt results.txt

