#!/bin/bash
echo -e "$(sed 's/+/ /g;s/%\(..\)/\\x\1/g;' || true)" || true
