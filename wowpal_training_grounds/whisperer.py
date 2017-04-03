#!/usr/bin/env python3
"""Train vowpal wabbit model"""
import argparse
import os
import itertools
import atexit

def predict(dataset):
    build_model(dataset)


def build_model(train_file):
    model_file = temporary_file("model")
    readable_model = temporary_file("readable_model")

   
    call('''vw -d {} \
          --holdout_period 5\
          --normalized -f {}\
          -k\
          --binary\
          --link=logistic\
          --readable_model {}\
          --loss_function logistic\
          -b 25\
          -c\
          --passes 20\
          --l2 0.0000005
          ''',
         train_file,
         model_file,
         readable_model
         )


@atexit.register
def cleanup_files():
    for f in _temp_files:
        os.remove(f)


_temp_files = []


def temporary_file(prefix):
    target_dir = "/media/ramdisk/" if os.path.isdir("/media/ramdisk/") else "./tmp_files/"

    if not os.path.isdir(target_dir):
            os.mkdir(target_dir)

    f = open(target_dir + prefix + str(len(_temp_files)), "w+")
    f.close()

    _temp_files.append(f.name)
    return f.name


def get_args():
    parser = argparse.ArgumentParser()
    parser.add_argument("--values", default="./data/values")
    args = parser.parse_args()
    return args.values


def call(command, *args):
    script = str.format(command, *args)
    os.system(script)

if __name__ == "__main__":
    predict(get_args())
    
