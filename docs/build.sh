#!/usr/bin/env bash
rm -f sample.log
docker run --rm -v $(pwd):/docs ktulatex xelatex --shell-escape sample.tex     
docker run -it --rm -v $(pwd):/docs ktulatex biber sample  
docker run --rm -v $(pwd):/docs ktulatex xelatex --shell-escape sample.tex     
