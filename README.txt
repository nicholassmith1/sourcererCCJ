

About sourcererCCJ:

sourcererCCJ is a fork of the project SourcererCCI, an Eclipse plugin implementation of the SourcererCC code clone detection algorithm. SourcererCCJ seeks to eliminate any Eclipse-specific dependencies in sourcererCCI will keeping all functionality implemented strickly in Java to maximize cross-platform compatability.


#############################
To run:

1. Compile the JAR

	$ ant cyclone

2. Run the JAR

	$ java -jar dist/CyClone.jar


#############################
To generate new grammar:

1. find or create grammar specification (https://github.com/antlr/grammars-v4) and add to grammar/
2. Generate the specific lexer and parser in the correct location with correct package (https://stackoverflow.com/questions/1655854/how-to-specify-a-target-package-for-antlr)

	$ java -cp lib/antlr-4.7.1-complete.jar org.antlr.v4.Tool -package antlr.grammar grammar/JavaLexer.g4 -o src/antlr/
	$ java -cp lib/antlr-4.7.1-complete.jar org.antlr.v4.Tool -package antlr.grammar -lib src/antlr/grammar/ grammar/JavaParser.g4 -o src/antlr/

3. Add produced files to git repo
4. ??? connect to antlr.utils somehow ???




#############################
Reference:


https://github.com/antlrjavaparser/antlr-java-parser

http://media.pragprog.com/titles/tpantlr2/listener.pdf


https://www.antlr.org/api/Java/org/antlr/v4/runtime/ParserRuleContext.html
