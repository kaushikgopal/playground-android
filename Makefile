default: debug

# some of these commands are specific to my macOS setup
# so you may need to install some of these tools like fd, gum

# Set default warning mode if not specified
# permitted values all,none,summary
# e.g. usage from make cli:
#  		make debug warnings=summary
warnings ?= none

ensure-deps:
	@if ! command -v rg >/dev/null 2>&1; then \
		echo "\033[1;36m•• Installing ripgrep...\033[0m"; \
		brew install ripgrep; \
	fi
	@if ! command -v fd >/dev/null 2>&1; then \
		echo "\033[1;36m•• Installing fd...\033[0m"; \
		brew install fd; \
	fi

help: ensure-deps		## list out commands with descriptions
	@rg '^([a-zA-Z0-9_-]+):.*?## (.*)$$' Makefile --no-line-number --no-filename --color=never --replace '$$1|$$2' | \
	awk -F'|' '{ \
		if (NR % 2 == 1) \
			printf "%-30s %s\n", $$1":", $$2; \
		else \
			printf "\033[2m%-30s %s\033[0m\n", $$1":", $$2; \
	}'

debug:      ## (default)   assemble debug app -lint
             ## 		make debug warnings=summary
	@echo "\033[2m•••• This script will assemble the debug app (without linting)\033[0m"
	@./gradlew --warning-mode $(warnings) assembleDebug -x lint

all:                    ## assemble full project -lint
	@echo "\033[2m•••• This script will assemble the full project (without linting)\033[0m"
	@./gradlew --warning-mode $(warnings) assemble -x lint


clean: clean-build  	## clean everything
	@echo "\033[2;31m•••• removing .gradle directories\033[0m"
	@fd -u -t d '^.gradle$$' -X rm -Rf
	@echo "\033[2;31m•••• removing .kotlin directories\033[0m"
	@fd -u -t d '^.kotlin$$' -X rm -Rf
	@echo "\033[2;31m•••• removing .DS_Store files\033[0m"
	@fd -u -tf ".DS_Store" -X rm
	@echo "\033[2;31m•••• remove empty directories, suppressing error messages\033[0m"
	@fd -u -td -te -X rmdir

clean-build: 		## clean build folders and cache alone
	@echo "\033[2;31m•••• This script will clean the build folders & cache\033[0m"
	@echo "\033[2;31m•••• removing build directories\033[0m"
	@fd -u -t d '^build$$' -X rm -Rf

kill-ksp: 		## kill kotlin daemon (useful for ksp errors)
	@echo "\033[2;31m•••• This script will kill your kotlin daemon (useful for ksp errors)\033[0m"
	@jps | grep -E 'KotlinCompileDaemon' | awk '{print $$1}' | xargs kill -9 || true

lint: 			## run lint checks
	@echo "\033[2m•••• This script will run lint checks\033[0m"
	@./gradlew --warning-mode $(warnings) lint

lint-update: 		## update lint baseline
	@echo "\033[2m•••• Update the baseline for lint\033[0m"
	@./gradlew updateLintBaseline

tests: 			## run unit tests (without lint)
	@echo "Run all unit tests without linting"
	@./gradlew -x lint --warning-mode $(warnings) testDebugUnitTest

test: 			## run single unit test or all tests (without lint)
                 # make test name=<TestClass>`
	@if [ "$(name)" = "" ]; then \
		echo "\033[2m•••• running all tests because you didn't provide name\033[0m"; \
		make tests; \
	else \
		echo "\033[2m•••• Finding module and package for test: $(name)\033[0m"; \
		if command -v fd >/dev/null 2>&1; then \
			TEST_FILE=$$(fd -t f "$(name).kt" . | head -1); \
		else \
			TEST_FILE=$$(find . -name "$(name).kt" -type f | head -1); \
		fi; \
		if [ "$$TEST_FILE" = "" ]; then \
			echo "Error: Test file $(name).kt not found"; \
			exit 1; \
		fi; \
		echo "\033[1;36mFound test file: $$TEST_FILE\033[0m"; \
		MODULE=$$(echo "$$TEST_FILE" | sed 's|./||' | sed 's|/src/.*||' | sed 's|/|:|g'); \
		PACKAGE=$$(grep "^package " "$$TEST_FILE" | head -1 | sed 's/package //g'); \
		echo "\033[2m•• Module identified: $$MODULE\033[0m"; \
		echo "\033[2m•• Package: $$PACKAGE\033[0m"; \
		echo "\033[2m•• Running test: $$PACKAGE.$(name)\033[0m"; \
		./gradlew -x lint --warning-mode $(warnings) $$MODULE:test --tests $$PACKAGE.$(name); \
	fi

ktfmt:                  ## ktfmt changed files on this branch
	@echo "\033[2m•••• This script will run ktfmt on all changed files\033[0m"
	@MERGE_BASE=$$(git merge-base HEAD origin/master); \
	MODIFIED_FILES=$$(git diff $$MERGE_BASE --diff-filter=ACMR --name-only --relative -- '*.kt'); \
	for FILE in $$MODIFIED_FILES; do \
		echo "Formatting $$FILE"; \
		ktfmt -F "$$FILE"; \
	done

ktfmt-all:
	@echo "\033[2m•••• This script will run ktfmt on all files\033[0m"
	@ktfmt -F $(shell find . -name '*.kt')

#tests-screenshots:
#	@echo "Verify all screenshots"
#	./gradlew --warning-mode $(warnings) verifyPaparazziDebug
#
#record-screenshots:
#	@echo "Record all screenshots"
#	./gradlew recordPaparazziDebug
