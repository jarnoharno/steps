PBSRCDIR := android/proto/src/main/proto
PBDIR := stepsproto

$(PBDIR)/%.pb.go: $(PBSRCDIR)/%.proto $(PBDIR)
	protoc --proto_path=$(PBSRCDIR) --go_out=$(PBDIR) $<

steps: steps.go $(PBDIR)/steps.pb.go
	go build

$(PBDIR):
	mkdir -p $@

# scripts

runapp:
	cd app
	./gradlew --daemon installDebug && \
		adb shell am start -n com.hiit.steps/.StepsActivity
	cd ..

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
