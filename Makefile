GXX 		= javac
RUNSUITE    = ../jwc/bin/run-tests
BINDIR		= bin
SRCDIR		= luju/src/main/java
TARGET      = lj

test: all
	@$(RUNSUITE) || (rm -rf $(TARGET) ; exit 1)

all:
	find $(SRCDIR) -name "*.java" > sources.txt
	$(GXX) -d $(BINDIR) @sources.txt
