#!/usr/bin/env bash
rm -f paper.log
docker run --rm -v $(pwd):/docs ktulatex xelatex --shell-escape paper.tex
docker run -it --rm -v $(pwd):/docs ktulatex biber paper
docker run --rm -v $(pwd):/docs ktulatex xelatex --shell-escape paper.tex
while grep -q "Please rerun LaTeX" paper.log
do
    rm -f paper.log
    docker run --rm -v $(pwd):/docs ktulatex xelatex --shell-escape paper.tex
done
