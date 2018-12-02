package antlr;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import antlr.grammar.JavaLexer;
import antlr.grammar.JavaParser;
import antlr.grammar.JavaParserBaseListener;

public class JavaMethodExtractor implements MethodExtractorI {
	/***
	 * Excerpted from "The Definitive ANTLR 4 Reference",
	 * published by The Pragmatic Bookshelf.
	 * Copyrights apply to this code. It may not be used to create training material, 
	 * courses, books, articles, and the like. Contact us if you are in doubt.
	 * We make no guarantees that this code is fit for any purpose. 
	 * Visit http://www.pragmaticprogrammer.com/titles/tpantlr2 for more book information.
	***/
	public static class ExtractMethodListener extends JavaParserBaseListener {
		JavaParser parser;
		HashMap<String, String> methods;

		public ExtractMethodListener(JavaParser parser) {
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
		public void enterMethodDeclaration(JavaParser.MethodDeclarationContext ctx) {
//			System.out.println(">>>>>>>>>>>>>>> Discovered method!");
//			System.out.print(ctx.getStart().getText());
//			
//			System.out.println(">>>>>>>>>>>>>>>");
////			System.out.print(ctx.toString());
//			System.out.print(ctx.getText());
			
			Token start = ctx.getStart();
			Token stop = ctx.getStop();
			
//			System.out.println(start.getLine() + " " + start.getCharPositionInLine());
//			System.out.println(stop.getLine() + " " + stop.getCharPositionInLine());
			
			
			/* How do I translate a line number and char offset into file positions? */
			
//			// this works.
//			System.out.println(stop.getInputStream().getText(new Interval(start.getStartIndex(), stop.getStopIndex())));
			
//			System.out.println(stop.getInputStream().getText(ctx.getSourceInterval()));
			
			
			String key;
			String value = stop.getInputStream().getText(new Interval(start.getStartIndex(), stop.getStopIndex()));

			key = ctx.IDENTIFIER().getText();
			
			/* TODO - maybe useful */
//			System.out.println(key);
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
	
	public Map<String, String> getMethods(String inputFile) {
		HashMap<String, String> methods = new HashMap<>();
        
		try {
	        InputStream is = System.in;
	        if (inputFile != null) {
	            is = new FileInputStream(inputFile);
	        }
	        
			ANTLRInputStream input = new ANTLRInputStream(is);
			
			JavaLexer lexer = new JavaLexer(input);
			CommonTokenStream tokens = new CommonTokenStream(lexer);
			JavaParser parser = new JavaParser(tokens);
			ParserRuleContext tree = parser.compilationUnit(); // parse
			 
			ParseTreeWalker walker = new ParseTreeWalker(); // create standard walker
			ExtractMethodListener extractor = new ExtractMethodListener(parser);
			walker.walk(extractor, tree); // initiate walk of tree with listener
			methods.putAll(extractor.getMethods());
		} catch (FileNotFoundException e) {
			;
		} catch (IOException e) {
			;
//			e.printStackTrace();
		}
		
		return methods;
	}	
}
