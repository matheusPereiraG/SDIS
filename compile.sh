#!/bin/sh
rm -rf build
mkdir -p build
find -name "*.java" > sources.txt
javac -d build @sources.txt