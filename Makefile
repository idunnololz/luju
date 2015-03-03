GXX 		= javac
BINDIR		= bin
SRCDIR		= luju/src/main/java

all:
	mkdir -p $(BINDIR)
	find $(SRCDIR) -name "*.java" > sources.txt
	$(GXX) -d $(BINDIR) @sources.txt

zip:
	rm luju.zip
	mkdir -p zip_temp
	cp -a --parents $(SRCDIR) zip_temp
	cp Makefile zip_temp
	cp -a joosc zip_temp
	cp -a --parents luju/res zip_temp
	cd zip_temp;zip -r luju *
	mv zip_temp/luju.zip luju.zip
	rm -rf zip_temp
