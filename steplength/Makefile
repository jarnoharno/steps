BUILD = build

# documentation targets

$(BUILD)/%.pdf: %.tex
	latexmk -pdf -outdir=$(BUILD) $<

all: $(BUILD)/steplength.pdf

BUILD:
	mkdir -p $(BUILD)

cont:
	cd doc; latexmk -pdf -outdir=$(BUILD) -pvc -view=none \
		-interaction=nonstopmode steplength.tex

# clean targets

clean:
	rm -rf $(BUILD)
