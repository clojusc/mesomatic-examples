PROJ = mesomatic-example
ROOT_DIR = $(shell pwd)
REPO = $(shell git config --get remote.origin.url)

include resources/make/code.mk
include resources/make/docs.mk
