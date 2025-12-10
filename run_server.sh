#!/bin/bash
cd "$(dirname "$0")/server"

# Clean and compile
rm -rf bin
mkdir -p bin
javac -d bin concurrent/*.java *.java

# Run server
java -cp bin ServerMain