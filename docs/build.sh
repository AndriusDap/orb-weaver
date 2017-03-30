#!/usr/bin/env bash
mkdir -p ./out
rm -f out/paper.log
docker run --rm -v $(pwd):/docs ktulatex xelatex --shell-escape --output-directory=./out paper.tex

if grep -q "Please (re)run Biber on the file" out/paper.log
then
    docker run --rm -v $(pwd):/docs ktulatex biber --input-directory out --output-directory out paper
    docker run --rm -v $(pwd):/docs ktulatex xelatex --shell-escape --output-directory=./out paper.tex
fi

while grep -q "Please rerun LaTeX" out/paper.log
do
    rm -f build/paper.log
    docker run --rm -v $(pwd):/docs ktulatex xelatex --shell-escape --output-directory=./out paper.tex
done
