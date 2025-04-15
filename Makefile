default: build-debug

# Set default warning mode if not specified
# permitted values all,none,summary
# e.g. usage from make cli:
#  		make build warnings=summary
warnings ?= none

help:			## list out commands with descriptions
	@sed -ne '/@sed/!s/## //p' $(MAKEFILE_LIST)

# many of these commands are specific to my macOS setup
# so you may need to install some of these tools like fd, gum

clean: clean-build  	## clean everything
	@echo "--- removing .gradle directories"
	@fd -u -t d '^.gradle$$' -X rm -Rf
	@echo "--- removing .kotlin directories"
	@fd -u -t d '^.kotlin$$' -X rm -Rf
	@echo "--- removing .DS_Store files"
	@fd -u -tf ".DS_Store" -X rm
	@echo "--- remove empty directories, suppressing error messages"
	@fd -u -td -te -X rmdir

clean-build: 		## clean build folders and cache alone
	@echo "--- This script will clean the build folders & cache"
	@echo "--- removing build directories"
	@fd -u -t d '^build$$' -X rm -Rf

kill-ksp: 		## kill kotlin daemon (useful for ksp errors)
	@echo "--- This script will kill your kotlin daemon (useful for ksp errors)"
	@jps | grep -E 'KotlinCompileDaemon' | awk '{print $$1}' | xargs kill -9 || true

build-debug: 		## assemble debug app (without lint)
             ## 		make build-debug warnings=summary
	@echo "--- This script will assemble the debug app (without linting)"
	@./gradlew --warning-mode $(warnings) assembleDebug -x lint

build: 			## assemble full project (without linting)
	@echo "--- This script will assemble the full project (without linting)"
	@./gradlew --warning-mode $(warnings) assemble -x lint

lint: 			## run lint checks
	@echo "--- This script will run lint checks"
	@./gradlew --warning-mode $(warnings) lint

lint-update: 		## update lint baseline
	@echo "--- Update the baseline for lint"
	@./gradlew updateLintBaseline

tests: 			## run unit tests (without lint)
	@echo "Run all unit tests without linting"
	@./gradlew --warning-mode $(warnings) tests -x lint

ktfmt:              ## ktfmt changed files on this branch
	@echo "--- This script will run ktfmt on all changed files"
	@MERGE_BASE=$$(git merge-base HEAD origin/master); \
	MODIFIED_FILES=$$(git diff $$MERGE_BASE --diff-filter=ACMR --name-only --relative -- '*.kt'); \
	for FILE in $$MODIFIED_FILES; do \
		echo "Formatting $$FILE"; \
		ktfmt -F "$$FILE"; \
	done

ktfmt-all:
	@echo "--- This script will run ktfmt on all files"
	@ktfmt -F $(shell find . -name '*.kt')

#tests-screenshots:
#	@echo "Verify all screenshots"
#	./gradlew --warning-mode $(warnings) verifyPaparazziDebug
#
#record-screenshots:
#	@echo "Record all screenshots"
#	./gradlew recordPaparazziDebug
