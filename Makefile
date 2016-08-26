SHELL := /bin/bash

.PHONY: test

test:
	sbt -mem 1024 test

