# many of these commands are specific to my macOS setup
# so you may need to install some of these tools like fd, gum

clean: clean-build
	@gum log -l debug "removing .gradle directories"
	@fd -u -t d '^.gradle$$' -X rm -Rf
	@gum log -l debug "removing .kotlin directories"
	@fd -u -t d '^.kotlin$$' -X rm -Rf
	@gum log -l debug "removing .DS_Store files"
	@fd -u -tf ".DS_Store" -X rm
	@gum log -l debug "remove empty directories, suppressing error messages"
	@fd -u -td -te -X rmdir

clean-build:
	@gum log -l info "This script will clean the build folders & cache"
	@gum log -l debug "removing build directories"
	@fd -u -t d '^build$$' -X rm -Rf

kill-ksp:
	@gum log -l info "This script will kill your kotlin daemon (useful for ksp errors)"
	@jps | grep -E 'KotlinCompileDaemon' | awk '{print $$1}' | xargs kill -9 || true
