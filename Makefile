SHELL=/bin/bash
CXX=g++
NAME:=j1939-java

### JAVA_HOME
ifndef JAVA_HOME
	JAVA_HOME=$(shell readlink -f /usr/bin/javac | sed "s:bin/javac::")
endif

JAVA_INCLUDES=-I$(JAVA_HOME)/include/linux -I$(JAVA_HOME)/include
JAVA=$(JAVA_HOME)/bin/java
JAVAC=$(JAVA_HOME)/bin/javac
JAVAH=$(JAVA_HOME)/bin/javah
JAR=$(JAVA_HOME)/bin/jar
JAVA_SRC:=$(shell find src -type f -and -name '*.java')
JAVA_TEST_SRC:=$(shell find src.test -type f -and -name '*.java')
JNI_SRC:=$(shell find jni -type f -and -regex '^.*\.\(cpp\|h\)$$')
JAVA_DEST=classes
JAVA_TEST_DEST=classes.test
LIB_DEST=lib
JAR_DEST=dist
JAR_DEST_FILE=$(JAR_DEST)/$(NAME).jar
JAR_MANIFEST_FILE=META-INF/MANIFEST.MF
DIRS=stamps $(JAVA_DEST) $(JAVA_TEST_DEST) $(LIB_DEST) $(JAR_DEST)
JNI_DIR=jni
JNI_CLASSES=org.isoblue.can.CanSocket org.isoblue.can.CanSocketJ1939
JAVAC_FLAGS=-g -Xlint:all
CXXFLAGS=-I./include -O2 -g -pipe -Wall -Wp,-D_FORTIFY_SOURCE=2 -fexceptions \
-fstack-protector --param=ssp-buffer-size=4 -fPIC -Wno-unused-parameter \
-pedantic -Wno-long-long -D_REENTRANT -D_GNU_SOURCE \
$(JAVA_INCLUDES)
SONAME=j1939-can
LDFLAGS=-Wl,-soname,$(SONAME)

.DEFAULT_GOAL := all
.LIBPATTERNS :=
.SUFFIXES:

.PHONY: all
all: stamps/create-jar stamps/compile-test

.PHONY: clean
clean:
	$(RM) -r $(DIRS) $(STAMPS) $(filter isobus.h,$(JNI_SRC))

.PHONY: run
run:
	$(JAVA) -Djava.library.path=./lib -cp $(JAVA_DEST):$(JAVA_TEST_DEST) org.isoblue.can.CanSocketTest

stamps/dirs:
	mkdir $(DIRS)
	@touch $@

stamps/compile-src: stamps/dirs $(JAVA_SRC)
	$(JAVAC) $(JAVAC_FLAGS) -d $(JAVA_DEST) $(sort $(JAVA_SRC))
	@touch $@

stamps/compile-test: stamps/compile-src $(JAVA_TEST_SRC)
	$(JAVAC) $(JAVAC_FLAGS) -cp $(JAVA_DEST) -d $(JAVA_TEST_DEST) \
		$(sort $(JAVA_TEST_SRC))
	@touch $@

stamps/generate-jni-h: stamps/compile-src
	$(JAVAH) -jni -d $(JNI_DIR) -classpath $(JAVA_DEST) \
		$(JNI_CLASSES)
	@touch $@

stamps/compile-jni: stamps/generate-jni-h $(JNI_SRC)
	$(CXX) $(CXXFLAGS) $(LDFLAGS) -shared -o $(LIB_DEST)/lib$(SONAME).so \
		$(sort $(filter %.cpp,$(JNI_SRC)))
	@touch $@

stamps/create-jar: stamps/compile-jni $(JAR_MANIFEST_FILE)
	$(JAR) cMf $(JAR_DEST_FILE) $(JAR_MANIFEST_FILE) lib -C $(JAVA_DEST) .
	@touch $@

.PHONY: check
check: stamps/create-jar stamps/compile-test
	$(JAVA) -ea -cp $(JAR_DEST_FILE):$(JAVA_TEST_DEST) \
		-Xcheck:jni \
		org.isoblue.can.CanSocketTest
