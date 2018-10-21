package userInterface;

import javax.swing.JOptionPane;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;

public class VersionCheck {
	public static boolean checkVersion() {
		try {
			//Add depenencies that need to be checked whether it loads well.
			
			ASTParser parser = ASTParser.newParser(AST.JLS4);
		}
		catch (Exception e) {
			JOptionPane.showMessageDialog( null, e.getMessage(),"Version Mismatch",JOptionPane.CLOSED_OPTION); 
			return false;
		}		
		
		return true;
	}
}
