package userInterface;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.NodeFinder;

public class UtilASTNode {
	public static class MethodInfo {
		public IPath path;
		public String methodKey;
		public int start;
		public int end;
		public int lineStart;
		public int lineEnd;
	}
	
	public static ASTNode astNode(final IMethod method) {
		final ICompilationUnit compilationUnit = method.getCompilationUnit();

		final ASTParser astParser = ASTParser.newParser( AST.JLS4 );
		astParser.setSource( compilationUnit );
		astParser.setKind( ASTParser.K_COMPILATION_UNIT );
		astParser.setResolveBindings( true );
		astParser.setBindingsRecovery( true );

		final ASTNode rootNode = astParser.createAST( null );
		String unitSource;
		int indexOf = -1;
		int length = -1; 
		try {
			unitSource = compilationUnit.getSource();
			String methodSource = method.getSource();
			indexOf = unitSource.indexOf( methodSource );
			length = methodSource.length();
		} catch (JavaModelException e) {
			// TODO Auto-generated catch block
			return null;
		}
		
		final ASTNode currentNode = NodeFinder.perform( rootNode, indexOf, length );

		return currentNode;
	}
	
	public static int getLineNum(String content){
	    int linenum = 0;
	    for(int i = 0; i < content.length(); i++){
	        if('\n' == content.charAt(i)) linenum++;
	    }
	    return linenum;
	}
	
	public static String methodStartLine(String method)
	{
		int charAt = 0;
		if(method.charAt(0) == '\t' || method.charAt(0) == ' ') {
			charAt++;
		}
		else if((method.charAt(0) == '/' && method.charAt(1) == '/')) {
			for(int i = 0; i < method.length(); i++){
				charAt++;
		        if('\n' == method.charAt(i)) { 
		        	break;
		        }
		        
		    }
		}
		else if(method.charAt(0) == '/' && method.charAt(1) == '*') {
			boolean isFound = false;
			for(int i = 3; i < method.length(); i++){
				if(isFound) break;
				charAt++;
		        if('*' == method.charAt(i-1) && '/' == method.charAt(i)) { 
		        	for(int j = charAt; j < method.length(); j++) {
		        		charAt++;
				        if('\n' == method.charAt(j)) { 
				        	isFound = true;
				        	break;
				        }
		        	}
		        }
		    }
		}
		
		String retMethod;
		retMethod = method.substring(charAt, method.length());
		return retMethod;
	}
	

	public static void setStartAndEndLine(final IMethod method, MethodInfo methodInfo) throws JavaModelException {
		final ICompilationUnit compilationUnit = method.getCompilationUnit();
		final String unitSource = compilationUnit.getSource();
		String methodSource = method.getSource();
		
		String methodStartLine = methodStartLine(methodSource); 
		while(!methodStartLine.equals(methodSource)) {
			methodSource = methodStartLine;
			methodStartLine = methodStartLine(methodSource);
		}
		
		int n = unitSource.indexOf(methodStartLine);
		String strBeforeMethod = unitSource.substring(0, n);
		methodInfo.start = n;
		methodInfo.end = n + methodSource.length();
		
		int start = getLineNum(strBeforeMethod);
		int length = getLineNum(methodSource);
		methodInfo.lineStart = start;
		methodInfo.lineEnd = start + length;
	}
}
