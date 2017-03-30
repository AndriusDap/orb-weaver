#!/usr/bin/env bash
docker run --rm -v $(pwd):/docs ktulatex xelatex --shell-escape sample.tex     
