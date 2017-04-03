#!/usr/bin/env python3
"""Train vowpal wabbit model"""
import time
import functools
import argparse
import os
import random
import itertools
import atexit
from functools import partial

class Model:
    def __init__(self, l1, l2, model, predictions, values, r):
        self.r = r
        self.l1 = l1
        self.l2 = l2
        self.model = model
        self.predictions = predictions
        self.values = values


def perf(func):
    @functools.wraps(func)
    def wrapped(*args, **kwargs):
        start_time = time.time()
        result = func(*args, **kwargs)
        elapsed_time = time.time() - start_time
        if elapsed_time > 1:
            print("function [{}] finished in {:.03f} s".format(func.__name__, elapsed_time))
        return result
    return wrapped


@perf
def predict(dataset):
    (train, test, valid) = split(dataset)
    l1 = [0, 0.001, 0.002, 0.003]
    l2 = [0, 0.001, 0.002, 0.003]

    models = []
    for p in [20]:
        result = [[0 for x in range(len(l2))] for x in range(len(l1))]

        for l in itertools.product(l1, l2):
            m = model([l[0], l[1], p], train, test)
            models.append(m)
            result[l1.index(l[0])][l2.index(l[1])] = m.r
        print("passes {}".format(p))
        for l in [0.0] + l2:
            print('{:18.09f}'.format(l), end="")
        print()

        for l, row in zip(l1, result):
            print('{:18.09f}'.format(l), end="")
            for val in row:
                print('{:18.09f}'.format(val), end="")
            print()
    return max(models, key=lambda x: x.r), valid


@perf
def model(x, train, test):
    model_file = build_model(train, x[0], x[1], x[2])
    r_test, predictions_test = test_model(test, model_file)
    r_train, predictions_train = test_model(train, model_file)
    print("r test: {:.03f}	r train: {:.03f}	x: {}".format(r_test, r_train, x))
    return Model(x[0], x[1], model_file, predictions_test, test, r_train)


@perf
def test_model(test, wabbit_model):
    predictions = temporary_file("predictions")
    call("vw -d {} -i {} -k -p {} -t --quiet", test, wabbit_model, predictions)
    r = compute_r2(test, predictions)
    return r, predictions


@perf
def build_model(train_file, l1, l2, passes):
    model_file = temporary_file("model")

    l1_arg = " --l1 {}".format(l1) if l1 != 0 else ""
    l2_arg = " --l2 {}".format(l2) if l2 != 0 else ""

    call("vw -d {} --normalized -f {} -k --readable_model hr.txt --loss_function squared --passes {} -c --quiet{}{}",
         train_file,
         model_file,
         passes,
         l1_arg,
         l2_arg
         )

    return model_file


@perf
def average(source):
    source.seek(0)
    count = 0
    total = 0
    for number in source:
        total += float(number.split()[0])
        count += 1
    return total / count


@perf
def compute_r2(label_file, prediction_file):
    n = 0
    sum_y = 0

    with open(label_file, "r") as labels:
        for line in labels:
            n += 1
            sum_y += float(line.split()[0])

    mean_y = sum_y / n

    ss_tot = 0
    ss_res = 0
    with open(label_file, "r") as labels, open(prediction_file, "r") as predictions:
        for line_y, line_y_pred in itertools.zip_longest(labels, predictions):
            if line_y is None or line_y_pred is None:
                raise ValueError("file lengths are different")

            y = float(line_y.split()[0])
            y_prediction = float(line_y_pred)

            ss_tot += (y - mean_y) ** 2
            ss_res += (y_prediction - y) ** 2

    r2 = 1 - ss_res / ss_tot
    return r2


@perf
def convert(label_file, value_file):
    pattern = build_wabbit_pattern(value_file)
    wabbit_file = temporary_file("wabbit")

    with open(label_file, "r") as labels, open(value_file, "r") as values, open(wabbit_file, "w") as converted:
        for line, label in zip(values, labels):
            formatted = pattern.format(label.strip(), *line.split())
            converted.write(formatted)
        return wabbit_file


@perf
def build_wabbit_pattern(value_file):
    with open(value_file, "r") as values:
        column_count = len(values.readline().split())
        pattern = "{} | " + " ".join([str(id) + ":{}" for id in range(column_count)])
        return pattern + "\n"

@perf
def split(full_dataset):
    train = temporary_file("train")
    test = temporary_file("test")
    valid = temporary_file("validation")

    with open(train, "w+") as train_file, open(test, "w+") as test_file, \
            open(valid, "w+") as valid_file, open(full_dataset, "r") as vw:

        proportions = [train_file] * 80 + [test_file] * 15 + [valid_file] * 5
        for line in vw:
            random.choice(proportions).write(line)

    return train, test, valid


@atexit.register
def cleanup_files():
    for f in _temp_files:
        os.remove(f)


_temp_files = []


@perf
def temporary_file(prefix):
    target_dir = "/media/ramdisk/" if os.path.isdir("/media/ramdisk/") else "./tmp_files/"

    if not os.path.isdir(target_dir):
            os.mkdir(target_dir)

    f = open(target_dir + prefix + str(len(_temp_files)), "w+")
    f.close()

    _temp_files.append(f.name)
    return f.name


@perf
def get_args():
    parser = argparse.ArgumentParser()
    parser.add_argument("--values", default="./data/values")
    args = parser.parse_args()
    return args.values


def call(command, *args):
    script = str.format(command, *args)
    os.system(script)


def print_anderson_result(statistic, critical, significance):
    message = \
        "Anderson statistic is "\
        + "rejecting " if statistic > critical else "approving" \
        + " that distribution is normal with" \
          " significance level of {:2.0f}%." \
          " Statistic = {:.03f} critical value = {:.03f}".format(significance, statistic, critical)
    return message


def rebuild_model(model):
    inverted = temporary_file("hash")
    call("vw --quiet -i {} -t -d {} --invert_hash {}".format(model.model, model.values, inverted))
    coeffs = {}
    intercept = 0
    with open(inverted, "r") as invert_hash:
        for line in invert_hash:
            l = line.strip()
                
            if l.startswith("^"):
                index = l.split(":")[1]
                value = l.split(":")[2]
                coeffs[index] = float(value)
            if l.startswith("Constant"):
                value = l.split(":")[2]
                intercept = float(value)
    result = "Y(x) ="
    for key, value in sorted(coeffs.items(), key=lambda x: int(x[0])):
        result += " {:+.03f}*x_{}".format(float(value), key)
    result += " {:+.03f}".format(intercept)
    return result


if __name__ == "__main__":
    best, validation_set = predict(get_args())
    print("The best model is: \n" + rebuild_model(best))
    final_r = test_model(validation_set, best.model)
    print("Testing dataset R squared is {:.03f}".format(final_r[0]))
    
