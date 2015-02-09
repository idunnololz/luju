GXX 		= javac
BINDIR		= bin
SRCDIR		= luju/src/main/java

all:
	find $(SRCDIR) -name "*.java" > sources.txt
	$(GXX) -d $(BINDIR) @sources.txt
