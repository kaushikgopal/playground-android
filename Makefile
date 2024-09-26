clean:
	@gum log -l info "This script will clean the build folders & cache"
	@gum log -l debug "removing build directories"
	@fd -u -t d '^build$$' -X rm -Rf
	@gum log -l debug "removing .gradle directories"
	@fd -u -t d '^.gradle$$' -X rm -Rf
	@gum log -l debug "removing .kotlin directories"
	@fd -u -t d '^.kotlin$$' -X rm -Rf
	@gum log -l debug "removing .DS_Store files"
	@fd -u -tf ".DS_Store" -X rm
	@gum log -l debug "remove empty directories, suppressing error messages"
	@fd -u -td -te -X rmdir
