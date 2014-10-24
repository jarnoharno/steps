PBSRCDIR := android/proto/src/main/proto
SERVERDIR := server
PBDIR := $(SERVERDIR)/stepsproto

PROTO := $(PBSRCDIR)/steps.proto

.PHONY: all
all: stepsd

$(PBDIR)/%.pb.go: $(PBSRCDIR)/%.proto $(PBDIR)
	protoc --proto_path=$(PBSRCDIR) --gogo_out=$(PBDIR) $<

$(SERVERDIR)/stepsd: $(SERVERDIR)/*.go $(SERVERDIR)/*.c $(SERVERDIR)/*.h \
		$(PBDIR)/steps.pb.go
	cd $(SERVERDIR) && go build -o stepsd

$(PBDIR):
	mkdir -p $@

# conv

.PHONY: conv
conv: conv/conv

conv/conv: conv/conv.go
	cd conv && go build

# stepsd

.PHONY: stepsd
stepsd: stepsd/stepsd

stepsd/stepsd: stepsd/stepsd.go stepsd/stepsproto/steps.pb.go stepsd/server.go \
		stepsd/device.go stepsd/connection.go
	cd stepsd && go build -o stepsd

stepsd/stepsproto/steps.pb.go: $(PBSRCDIR)/steps.proto stepsd/stepsproto
	protoc -I=$(PBSRCDIR) --gogo_out=stepsd/stepsproto $<

stepsd/stepsproto:
	mkdir -p $@

# cpp

.PHONY: cconv
cconv: conv/c/cconv

conv/c/%.pb.h: $(PBSRCDIR)/%.proto
	protoc -I=$(PBSRCDIR) --cpp_out=conv/c $<

conv/c/cconv: conv/c/cconv.cpp conv/c/steps.pb.h
	g++ -O2 -std=c++11 -lprotobuf -o $@ conv/c/cconv.cpp conv/c/steps.pb.cc

# julia

.PHONY: julia
julia: julia/steps_pb.jl

julia/%_pb.jl: proto/%.proto
	protoc -I=proto --julia_out=julia $<

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
www: www/steps.json

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
