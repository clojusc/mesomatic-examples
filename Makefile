PROJ = mesomatic-example
ROOT_DIR = $(shell pwd)
REPO = $(shell git config --get remote.origin.url)

include dev-resources/make/code.mk
include dev-resources/make/docs.mk
