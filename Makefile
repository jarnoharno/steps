PBSRCDIR := android/proto/src/main/proto
PBDIR := stepsproto

$(PBDIR)/%.pb.go: $(PBSRCDIR)/%.proto $(PBDIR)
	protoc --proto_path=$(PBSRCDIR) --go_out=$(PBDIR) $<

steps: steps.go $(PBDIR)/steps.pb.go
	go build

$(PBDIR):
	mkdir -p $@
