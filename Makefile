PBSRCDIR := android/proto/src/main/proto
PBDIR := stepsproto

PROTO := $(PBSRCDIR)/steps.proto

$(PBDIR)/%.pb.go: $(PBSRCDIR)/%.proto $(PBDIR)
	protoc --proto_path=$(PBSRCDIR) --go_out=$(PBDIR) $<

steps: steps.go hub.go conn.go filter.go $(PBDIR)/steps.pb.go
	go build

$(PBDIR):
	mkdir -p $@

# www

node_modules:
	npm install

www/steps.json: $(PROTO)
	proto2js $< > $@

www/bundle.js: js/steps.js node_modules
	browserify $< -o $@

.PHONY: clean-www
clean-www:
	rm -rf node_modules
	rm -f www/steps.json
	rm -f www/steps.js

.PHONY: www
www: www/steps.json www/bundle.js

# scripts

runapp:
	cd android && \
		./gradlew --daemon installDebug && \
		adb shell am start -n com.hiit.steps/.StepsActivity

whoop:
	git checkout upload
	git merge master
	git push whoop upload
	git checkout master
	cat mergewhoop.sh | ssh whoop.pw
	cat runwhoop.sh | ssh whoop.pw

mergewhoop:
	cat mergewhoop.sh | ssh whoop.pw

runwhoop:
	cat runwhoop.sh | ssh whoop.pw

whoopout:
	ssh whoop.pw cat /home/jao/steps/steps.out
