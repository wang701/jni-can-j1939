SHELL=/bin/bash
NAME:=can-j1939-jni
SONAME=can-j1939

### JAVA_HOME
ifndef JAVA_HOME
	JAVA_HOME=$(shell readlink -f /usr/bin/javac | sed "s:bin/javac::")
endif

# aux function to check if dir exsits
exists() = [[ -e $1 ]];

JAVA=$(JAVA_HOME)/bin/java
JAVAC=$(JAVA_HOME)/bin/javac
JAVAH=$(JAVA_HOME)/bin/javah
JAR=$(JAVA_HOME)/bin/jar

JAVA_SRC:=$(shell find src -type f -and -name '*.java')
JAVA_TEST_SRC:=$(shell find src.test -type f -and -name '*.java')
JNI_SRC:=$(shell find jni -type f -and -regex '^.*\.\(cpp\|h\)$$')

DIRS=$(JAVA_DEST) $(JAVA_TEST_DEST) $(LIB_DEST) $(JAR_DEST) $(OBJ_DEST)
JAVA_DEST=classes
JAVA_TEST_DEST=classes.test
LIB_DEST=libs
OBJ_DEST=obj
JAR_DEST=dist
JNI_DIR=jni
JAR_DEST_FILE=$(JAR_DEST)/$(NAME).jar
JAR_MANIFEST_FILE=META-INF/MANIFEST.MF
JNI_CLASSES=org.isoblue.can.CanSocket org.isoblue.can.CanSocketJ1939
JAVAC_FLAGS=-g -Xlint:all

.DEFAULT_GOAL := all

.PHONY: clean
clean:
	rm -rf $(DIRS)

.PHONY: run
run:
	$(JAVA) -Djava.library.path=./libs -cp $(JAVA_DEST):$(JAVA_TEST_DEST) org.isoblue.can.CanSocketTest

.PHONY: all
all: jni jar

.PHONY: dirs
dirs:
	 mkdir -p $(DIRS)

.PHONY: src
src: dirs $(JAVA_SRC)
	$(JAVAC) $(JAVAC_FLAGS) -d $(JAVA_DEST) $(sort $(JAVA_SRC))

.PHONY: test
test: src $(JAVA_TEST_SRC)
	$(JAVAC) $(JAVAC_FLAGS) -cp $(JAVA_DEST) -d $(JAVA_TEST_DEST) \
		$(sort $(JAVA_TEST_SRC))

.PHONY: jni
jni: src 
	$(JAVAH) -jni -d $(JNI_DIR) -classpath $(JAVA_DEST) \
		$(JNI_CLASSES)
	ndk-build V=1 NDK_PROJECT_PATH=. APP_BUILD_SCRIPT=./Android.mk

.PHONY: jar
jar: jni $(JAR_MANIFEST_FILE)
	$(JAR) cMf $(JAR_DEST_FILE) $(JAR_MANIFEST_FILE) libs -C $(JAVA_DEST) .

.PHONY: check
check: jar test
	$(JAVA) -ea -cp $(JAR_DEST_FILE):$(JAVA_TEST_DEST) \
		-Xcheck:jni \
		org.isoblue.can.CanSocketTest
