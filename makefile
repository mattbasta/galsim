# Galactic Simulator Makefile
# Matt Basta

all:	clean build

PACKAGE = galsim
BASENAME = GalSim
TARGET = $(BASENAME).class
SERVERCLASS=GalServer
SERVERFILE=$(SERVERCLASS).class
CLIENTCLASS=Intrepid
CLIENTFILE=$(CLIENTCLASS).class

DEBUG = 1

include makelib

build:		$(CLASSFILES)

clean:		subclean
		/bin/rm -f $(TARGET) *.out *.dif junk junk.cfg

test:	build testserv

test_single:	build
		java $(PACKAGE).$(SERVERCLASS) 256 ${PC} 0

test_worker:	build
		java $(PACKAGE).$(SERVERCLASS) 256 ${PC} 1

test_quad:	build
		java $(PACKAGE).$(SERVERCLASS) 256 ${PC} 4

test_oct:	build
		java $(PACKAGE).$(SERVERCLASS) 256 ${PC} 8

test_sede:	build
		java $(PACKAGE).$(SERVERCLASS) 256 ${PC} 16

server:	build
		java $(PACKAGE).$(SERVERCLASS) 256 ${PC} ${CORES}

testvis:    $(CLASSFILES)
		java -Djava.library.path=lwjgl-2.8.3/native/macosx $(PACKAGE).$(CLIENTCLASS)


deploy:
	rsync -avz -e ssh . mbast810@hermione:~/JavaLang/galsim/

