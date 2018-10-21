package userInterface;

import java.io.FileNotFoundException;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.annotation.PreDestroy;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.ViewPart;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import userInterface.UtilASTNode.MethodInfo;
import astParser.MethodVisitor;

class CloneList {
	private String currentName;
	private LinkedList<CloneList> children;
	private String uniqueId;
	private Map<String, CloneList> map;
	private CloneList parent;
	private int level;
	
	public CloneList(List<String> list) {
		this.currentName = "Detected Clones";
		map = new HashMap<String, CloneList>();
		children = new LinkedList<CloneList>();
		level = 0;
		
		for(String str : list) {		
			String path = str.substring(0, str.indexOf('#'));
	    	String methodKey = str.substring(str.indexOf('#') + 1);
			
			String prjName = path.substring(0, str.indexOf('$'));
			String prjPath = path.substring(str.indexOf('$') + 1);
			
			String fileName = "";
			while(!fileName.equals(prjPath)) {
				fileName = prjPath;
				prjPath = prjPath.substring(prjPath.indexOf('$') + 1);
			}
			
			if(!map.containsKey(prjName)) {
				CloneList newProject = new CloneList(prjName, "", this, 0);
				addChild(prjName, newProject);
			}
			
			CloneList curProject = (CloneList) map.get(prjName);
			if(!curProject.hasChild(fileName)) {
				CloneList newFile = new CloneList(fileName, "", curProject, 1);
				curProject.addChild(fileName, newFile);
			}
			
			CloneList file = curProject.getChild(fileName);
			String realPath = path.replace('$', '/');
			String methodName = methodKey.substring(methodKey.indexOf('.') + 1);
			methodName = methodName.substring(0, methodName.indexOf('+'));
			methodName += "  (" + realPath + ")";
			
			CloneList method = new CloneList(methodName, str, file, 2);
			file.addChild(fileName, method);
		}
	}
	
	public CloneList(String currentName, String uniqueId, CloneList parent, int level) {
		this.level = level;
		this.currentName = currentName;
		this.uniqueId = uniqueId;		
		this.parent = parent;
		
		map = new HashMap<String, CloneList>();
		children = new LinkedList<CloneList>();
	}
	
	public String getName() { return currentName; }
	public String getId() { return uniqueId; }
	public Boolean hasChildren() { return !children.isEmpty(); }
	public LinkedList<CloneList> getChildren() { return children; }
	public Boolean hasChild(String name) { return map.containsKey(name);}
	public CloneList getChild(String name) { return map.get(name); }
	public CloneList getParent() { return parent; }
	public int getLevel() { return level; }
	public void addChild(String name, CloneList cl) {
		children.add(cl);
		map.put(name, cl);
	}
};	


public class ResultView extends ViewPart {
	private TreeViewer viewer;
	private Image projectImage;
	private Image fileImage;
	private Image methodImage;
	private WatchUserBehavior wub;
	IMarker marker = null;
	public ResultView() {
	}

	public void createPartControl(Composite parent) {
	    createProjectImage();
	    createFileImage();
	    createMethodImage();
	    viewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
	    viewer.setContentProvider(new ViewContentProvider());
	    viewer.setLabelProvider(new ViewLabelProvider());
	    hookDoubleClickAction();
	}
	
	private Action doubleClickAction;
	private void hookDoubleClickAction() {
		viewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {				
				TreeSelection ts = (TreeSelection)event.getSelection();
				CloneList cl = (CloneList) ts.getFirstElement();
				if(cl.getLevel() == 2) {
					
					moveFunction(cl.getId());
				}
			}
		});
	}
	
	private void createProjectImage() {
		Bundle bundle = FrameworkUtil.getBundle(ViewLabelProvider.class);
		URL url = FileLocator.find(bundle, new Path("icons/folder.png"), null);
		ImageDescriptor imageDcr = ImageDescriptor.createFromURL(url);
		this.projectImage = imageDcr.createImage();
	}
		
	private void createFileImage() {
		Bundle bundle = FrameworkUtil.getBundle(ViewLabelProvider.class);
		URL url = FileLocator.find(bundle, new Path("icons/page_white_text.png"), null);
		ImageDescriptor imageDcr = ImageDescriptor.createFromURL(url);
		this.fileImage = imageDcr.createImage();
	}
	
	private void createMethodImage() {
		Bundle bundle = FrameworkUtil.getBundle(ViewLabelProvider.class);
		URL url = FileLocator.find(bundle, new Path("icons/page_copy.png"), null);
		ImageDescriptor imageDcr = ImageDescriptor.createFromURL(url);
		this.methodImage = imageDcr.createImage();
	}
	
	public void setContent(List<String> list, WatchUserBehavior wub) {
		this.wub = wub;
		CloneList cl = new CloneList(list);
		viewer.setInput(cl);
		viewer.refresh();
		viewer.expandToLevel(2);
	}
	
	class ViewContentProvider implements ITreeContentProvider {
	    public void inputChanged(Viewer v, Object oldInput, Object newInput) {
	    }

	    @Override
	    public void dispose() {
	    	return;
	    }

	    @Override
	    public Object[] getElements(Object inputElement) {
	    	CloneList cl = (CloneList)inputElement;
	    	List<CloneList> list = cl.getChildren();
	    	CloneList[] array = new CloneList[list.size()];
	    	list.toArray(array);
	    	
	    	return (CloneList[]) array;
	    }

	    @Override
	    public Object[] getChildren(Object parentElement) {
	    	CloneList list = (CloneList) parentElement;
	    	List<CloneList> childrenList = list.getChildren();
	    	CloneList[] array = new CloneList[childrenList.size()];
	    	childrenList.toArray(array);
	    	return (CloneList[]) array;
	    }

	    @Override
	    public Object getParent(Object element) {
	    	CloneList cl = (CloneList) element;
	    	return (CloneList) cl.getParent();
	    }

	    @Override
	    public boolean hasChildren(Object element) {
	    	CloneList list = (CloneList) element;
	    	Boolean t = list.hasChildren();
	    	return t;
	    }
	}

	class ViewLabelProvider extends StyledCellLabelProvider {
		@Override
		public void update(ViewerCell cell) {
			CloneList element = (CloneList) cell.getElement();
			StyledString text = new StyledString();
			text.append(element.getName());
			
			if(element.getLevel() == 0) {
				cell.setImage(projectImage);
				text.append(" (" + element.getChildren().size() + ") ", StyledString.COUNTER_STYLER);
			}
			else if(element.getLevel() == 1) {
				cell.setImage(fileImage);
				text.append(" (" + element.getChildren().size() + ") ", StyledString.COUNTER_STYLER);
			}	
			else {
				cell.setImage(methodImage);
			}
			
			cell.setText(text.toString());
			cell.setStyleRanges(text.getStyleRanges());
			super.update(cell);
		}
	}

	public void setFocus() {
		viewer.getControl().setFocus();
	}

	@PreDestroy
	public void dispose() {
		projectImage.dispose();
		fileImage.dispose();
		methodImage.dispose();
	}
	
	public void moveFunction(String pathMethod)
    {
		pathMethod = pathMethod.substring(0,pathMethod.length()-4);
//		System.out.println("PATH IS:" +pathMethod);
		pathMethod = pathMethod.replace('$', '/');
		pathMethod = pathMethod.replace('+', '(');
		pathMethod = pathMethod.replace('=', ')');
		
    	int nSeparator = pathMethod.indexOf('#');    	
    	String strPath = pathMethod.substring(0, nSeparator);
    	String methodKey = pathMethod.substring(pathMethod.indexOf('#') + 1, pathMethod.length());
    	String packageName = "";
    	if(methodKey.indexOf('/') != -1)
    		packageName = methodKey.substring(1, methodKey.indexOf('/'));
    	
    	org.eclipse.core.runtime.Path path = new org.eclipse.core.runtime.Path(strPath);
    	IFile ifile = ResourcesPlugin.getWorkspace().getRoot().getFile((IPath)path);
    	if (ifile == null)
			try {
				throw new FileNotFoundException();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				return;
		}	
		
    	IJavaProject javaProject = JavaCore.create(ifile.getProject());
	    IPackageFragment[] packages;
	    boolean isFound = false;
	    MethodInfo methodInfo = new MethodInfo();
	    try {
			packages = javaProject.getPackageFragments();
			for (IPackageFragment mypackage : packages) {
				if(isFound) break;
				
	    		for (ICompilationUnit unit : mypackage.getCompilationUnits()) {
	    			if(isFound) break;
	    			ASTParser parser = ASTParser.newParser(AST.JLS4);
	    			parser.setKind(ASTParser.K_COMPILATION_UNIT);	            	     
	    			parser.setSource(unit);
	    			parser.setResolveBindings(true);
					
	    			CompilationUnit parse = (CompilationUnit) parser.createAST(null);
	    			MethodVisitor visitor = new MethodVisitor(packageName);
	    			parse.accept(visitor);
	    			
	    			IType[] allTypes = unit.getAllTypes();
	    			for (IType type : allTypes) {
	    				if(isFound) break;
	    				IMethod[] methods = type.getMethods();
	    				for (IMethod method : methods) {
	    					ASTNode node = UtilASTNode.astNode(method);
	    					if(node != null) {
	    						if(methodKey.equals(method.getKey())) {
		    						methodInfo.path = method.getPath();
		    						methodInfo.methodKey = method.getKey();
		    						UtilASTNode.setStartAndEndLine(method, methodInfo);
		    						isFound = true;
		    						break;
	    						}
	    					}
	    				}
	    			}
	    		}  				
	    	} 
		} catch (JavaModelException e1) {
			// TODO Auto-generated catch block
			return;
		}
    	
    	if(isFound) {
		    try {
		    	//wub.clearAllMarkers();
		    	marker = ifile.createMarker("edu.uci.ics.mondego.clonedetection.marker");
		    	marker.setAttribute(IMarker.LINE_NUMBER, methodInfo.lineStart + 1);
		    	Display.getDefault().asyncExec(new Runnable() {
					
					IWorkbench workbench = null;
					IWorkbenchWindow workbenchWindow = null;
					IWorkbenchPage page = null;
					//
					// Check if there is new project
					//

					public void run() {
						workbench = PlatformUI.getWorkbench();
						workbenchWindow = workbench.getActiveWorkbenchWindow();
						page = workbenchWindow.getActivePage();
						try {
							IDE.openEditor(page, marker);
							marker.delete();
							wub.clearAllMarkers();
					    	//wub.updateMethodKeyList();
					    	wub.resetParsingHistory();
						} catch (PartInitException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (CoreException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
		    	});
		    	
		    } catch (CoreException e) {
				return;
		    }
    	}
    }
}
