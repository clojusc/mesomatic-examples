PROJ = mesomatic-hello
ROOT_DIR = $(shell pwd)
REPO = $(shell git config --get remote.origin.url)

include resources/make/docs.mk

