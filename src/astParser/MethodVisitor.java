package astParser;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.dom.*;

public class MethodVisitor extends ASTVisitor {
    int m_nStatementCount;
    PrintWriter out;

    public MethodVisitor(String filename) {
        FileWriter fstream = null;
        try {
            fstream = new FileWriter(filename + ".csv", true);
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        out = new PrintWriter(fstream);
    }

    // FileWriter fstream = new FileWriter();
    // FileWriter fstream = new FileWriter
    // ("C:\\Users\\Hitesh\\workspace\\FindBugsXMLParser\\resources\\methodbugdensity.csv");
    // PrintWriter out = new PrintWriter(fstream);
    //

    @Override
    public boolean visit(MethodDeclaration node) {
        m_nStatementCount = 0;
        return true;
    }

    public void endVisit(MethodDeclaration node) {
        if (null != node) {
            IMethodBinding imb = (IMethodBinding) node.resolveBinding();
            if (null != imb) {
                IMethod method = (IMethod) imb.getJavaElement();
                if (null != method) {
                    StringBuilder s = new StringBuilder();
                    if (null != method.getDeclaringType()) {
                        s.append(
                                method.getDeclaringType()
                                        .getFullyQualifiedName()).append(".")
                                .append(method.getElementName());
                       // System.out.println(s + "," + m_nStatementCount);
                        out.println(s + "," + m_nStatementCount);
                        out.flush();
                    }
                }else{
                    System.out.println("method NUll ************"+ imb.getName());
                }
            }else{
                System.out.println("IMB NULL **************" + node.getName() + node.getParent().getNodeType());
            }
        }
    }

    public boolean visit(AssertStatement node) {
        m_nStatementCount++;
        return true;
    }

    public boolean visit(Block node) {
        m_nStatementCount++;
        return true;
    }

    public boolean visit(BreakStatement node) {
        m_nStatementCount++;
        return true;
    }

    public boolean visit(ConstructorInvocation node) {
        m_nStatementCount++;
        return true;
    }

    public boolean visit(ContinueStatement node) {
        m_nStatementCount++;
        return true;
    }

    public boolean visit(DoStatement node) {
        m_nStatementCount++;
        return true;
    }

    public boolean visit(EmptyStatement node) {
        m_nStatementCount++;
        return true;
    }

    public boolean visit(ExpressionStatement node) {
        m_nStatementCount++;
        return true;
    }

    public boolean visit(ForStatement node) {
        m_nStatementCount++;
        return true;
    }

    public boolean visit(IfStatement node) {
        m_nStatementCount++;
        return true;
    }

    public boolean visit(LabeledStatement node) {
        m_nStatementCount++;
        return true;
    }

    public boolean visit(ReturnStatement node) {
        m_nStatementCount++;
        return true;
    }

    public boolean visit(SuperConstructorInvocation node) {
        m_nStatementCount++;
        return true;
    }

    public boolean visit(SwitchCase node) {
        m_nStatementCount++;
        return true;
    }

    public boolean visit(SwitchStatement node) {
        m_nStatementCount++;
        return true;
    }

    public boolean visit(SynchronizedStatement node) {
        m_nStatementCount++;
        return true;
    }

    public boolean visit(ThrowStatement node) {
        m_nStatementCount++;
        return true;
    }

    public boolean visit(TryStatement node) {
        m_nStatementCount++;
        return true;
    }

    public boolean visit(TypeDeclarationStatement node) {
        m_nStatementCount++;
        return true;
    }

    public boolean visit(VariableDeclarationStatement node) {
        m_nStatementCount++;
        return true;
    }

    public boolean visit(WhileStatement node) {
        m_nStatementCount++;
        return true;
    }

    public boolean visit(EnhancedForStatement node) {
        m_nStatementCount++;
        return true;
    }
}