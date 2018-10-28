/*
 * See https://github.com/adamsiemion/antlr-java-ast/tree/master/src/main
 */

package antlr;

//import org.antlr.v4.runtime.ANTLRInputStream;
//import org.antlr.v4.runtime.CommonTokenStream;
//import org.antlr.v4.runtime.ParserRuleContext;
//import org.antlr.v4.runtime.RuleContext;
//import org.antlr.v4.runtime.tree.ParseTree;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Objects;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

//import ast.antlr.*;
import antlr.grammar.*;


import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.misc.Interval;

public class Utils {
	
	
	/***
	 * Excerpted from "The Definitive ANTLR 4 Reference",
	 * published by The Pragmatic Bookshelf.
	 * Copyrights apply to this code. It may not be used to create training material, 
	 * courses, books, articles, and the like. Contact us if you are in doubt.
	 * We make no guarantees that this code is fit for any purpose. 
	 * Visit http://www.pragmaticprogrammer.com/titles/tpantlr2 for more book information.
	***/
	public static class ExtractMethodListener extends Java8BaseListener {
		Java8Parser parser;
		HashMap<String, String> methods;
//		File

		public ExtractMethodListener(Java8Parser parser) {
			this.parser = parser;
			methods = new HashMap<String, String>();
		}
		
		/**
		 * Returns hashmap of unique method identifier and code
		 * @return
		 */
		public HashMap<String, String> getMethods() {
			return this.methods;
		}

		/** Listen to matches of methodDeclaration */
		@Override
		public void enterMethodDeclaration(Java8Parser.MethodDeclarationContext ctx) {
//			System.out.println(">>>>>>>>>>>>>>> Discovered method!");
//			System.out.print(ctx.getStart().getText());
//			
//			System.out.println(">>>>>>>>>>>>>>>");
////			System.out.print(ctx.toString());
//			System.out.print(ctx.getText());
			
			Token start = ctx.getStart();
			Token stop = ctx.getStop();
			
			System.out.println(start.getLine() + " " + start.getCharPositionInLine());
			System.out.println(stop.getLine() + " " + stop.getCharPositionInLine());
			
			
			/* How do I translate a line number and char offset into file positions? */
			
//			// this works.
//			System.out.println(stop.getInputStream().getText(new Interval(start.getStartIndex(), stop.getStopIndex())));
			
//			System.out.println(stop.getInputStream().getText(ctx.getSourceInterval()));
			
			
			String key;
			String value = stop.getInputStream().getText(new Interval(start.getStartIndex(), stop.getStopIndex()));
			
			key = ctx.methodHeader().getText();
			
			System.out.println(key);
			methods.put(key, value);
			
			
			
			
//			// need parser to get tokens
//			TokenStream tokens = parser.getTokenStream();
//			String type = "void";
//			if (ctx.type() != null) {
//				type = tokens.getText(ctx.type());
//			}
//			String args = tokens.getText(ctx.formalParameters());
//			System.out.println("\t" + type + " " + ctx.Identifier() + args + ";");
		}
	}

	
	
	public static HashMap<String, String> getMethods(String inputFile) throws Exception {
//        String inputFile = null;
//        if ( args.length>0 ) inputFile = args[0];
        InputStream is = System.in;
        if ( inputFile!=null ) {
            is = new FileInputStream(inputFile);
        }
        ANTLRInputStream input = new ANTLRInputStream(is);

//		CharStream input;
//		// not sure why this seems broken
//		input = CharStream.fromFileName(inputFile);
		
		Java8Lexer lexer = new Java8Lexer(input);
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		Java8Parser parser = new Java8Parser(tokens);
		ParserRuleContext tree = parser.compilationUnit(); // parse
		 
		ParseTreeWalker walker = new ParseTreeWalker(); // create standard walker
		ExtractMethodListener extractor = new ExtractMethodListener(parser);
		walker.walk(extractor, tree); // initiate walk of tree with listener
		
		return extractor.getMethods();
	}
//
//	
//
//    public static void main(String[] args) throws IOException {
//        print(parseJava(readFile(new File("examples/java/Demo.java"))), false);
//    }
//
//    private static String readFile(File file) throws IOException {
//        byte[] encoded = Files.readAllBytes(file.toPath());
//        return new String(encoded, Charset.forName("UTF-8"));
//    }
//
//    private static ParserRuleContext parseJava(String code) throws IOException {
//        Java8Lexer lexer = new Java8Lexer(new ANTLRInputStream(code));
//
//        CommonTokenStream tokens = new CommonTokenStream(lexer);
//
//        Java8Parser parser = new Java8Parser(tokens);
//
//        return Objects.requireNonNull(parser.classDeclaration());
//    }
//
//    private static void print(RuleContext ctx, boolean verbose) {
//        explore(ctx, verbose, 0);
//    }
//
//    private static void explore(RuleContext ctx, boolean verbose, int indentation) {
//        boolean toBeIgnored = !verbose && ctx.getChildCount() == 1
//                && ctx.getChild(0) instanceof ParserRuleContext;
//        if (!toBeIgnored) {
//            String ruleName = Java8Parser.ruleNames[ctx.getRuleIndex()];
//            for (int i = 0; i < indentation; i++) {
//                System.out.print("  ");
//            }
//            System.out.println(ruleName + " " + ctx.getText());
//        }
//        for (int i=0;i<ctx.getChildCount();i++) {
//            ParseTree element = ctx.getChild(i);
//            if (element instanceof RuleContext) {
//                explore((RuleContext)element, verbose, indentation + (toBeIgnored ? 0 : 1));
//            }
//        }
//}
	
}
