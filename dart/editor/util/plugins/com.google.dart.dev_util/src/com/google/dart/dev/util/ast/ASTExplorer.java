/*
 * Copyright (c) 2012, the Dart project authors.
 * 
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.dev.util.ast;

import com.google.dart.dev.util.DartDevPlugin;
import com.google.dart.engine.ast.ASTNode;
import com.google.dart.engine.ast.ClassDeclaration;
import com.google.dart.engine.ast.ClassTypeAlias;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.ConstructorDeclaration;
import com.google.dart.engine.ast.FieldDeclaration;
import com.google.dart.engine.ast.FunctionDeclaration;
import com.google.dart.engine.ast.FunctionTypeAlias;
import com.google.dart.engine.ast.Identifier;
import com.google.dart.engine.ast.MethodDeclaration;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.TopLevelVariableDeclaration;
import com.google.dart.engine.ast.TypeParameter;
import com.google.dart.engine.ast.VariableDeclaration;
import com.google.dart.engine.ast.VariableDeclarationList;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.error.AnalysisErrorListener;
import com.google.dart.engine.parser.Parser;
import com.google.dart.engine.scanner.StringScanner;
import com.google.dart.engine.scanner.Token;
import com.google.dart.tools.core.utilities.resource.IFileUtilities;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;
import com.google.dart.tools.ui.internal.text.editor.EditorUtility;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.ViewPart;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A basic AST explorer view.
 */
@SuppressWarnings("restriction")
public class ASTExplorer extends ViewPart implements AnalysisErrorListener {

  private class EventHandler implements IPartListener, ISelectionListener,
      ISelectionChangedListener {

    //triggers refresh on save and updates label to indicate dirtiness
    private IPropertyListener propertyListener = new IPropertyListener() {
      @Override
      public void propertyChanged(Object source, int propId) {
        if (source instanceof DartEditor) {
          if (propId == IEditorPart.PROP_DIRTY) {
            if (((DartEditor) source).isDirty()) {
              setPartName('*' + getPartName());
            } else {
              refresh();
              String name = getPartName();
              if (name.charAt(0) == '*') {
                name = name.substring(1);
              }
              setPartName(name);
            }
          }
        }
      }
    };

    @Override
    public void partActivated(IWorkbenchPart part) {
      if (part instanceof DartEditor) {
        part.addPropertyListener(propertyListener);
      }
    }

    @Override
    public void partBroughtToTop(IWorkbenchPart part) {
      //ignore
    }

    @Override
    public void partClosed(IWorkbenchPart part) {
      if (part instanceof DartEditor) {
        part.removePropertyListener(propertyListener);
      }
    }

    @Override
    public void partDeactivated(IWorkbenchPart part) {
      if (part instanceof DartEditor) {
        part.removePropertyListener(propertyListener);
      }
    }

    @Override
    public void partOpened(IWorkbenchPart part) {
      //ignore
    }

    @Override
    public void selectionChanged(IWorkbenchPart part, ISelection selection) {
      //TODO(pquitslund): implement text selection tracking
      if (selection instanceof ITextSelection) {
        int offset = ((ITextSelection) selection).getOffset();
        int length = ((ITextSelection) selection).getLength();
        selectNodeAtOffset(offset, length);
      }
    }

    @Override
    public void selectionChanged(SelectionChangedEvent event) {

      IEditorPart editor = getActiveEditor();

      if (editor instanceof DartEditor) {
        ISelection selection = event.getSelection();
        if (selection instanceof TreeSelection) {
          Object element = ((TreeSelection) selection).getFirstElement();
          if (element instanceof ASTNode) {
            ASTNode node = (ASTNode) element;
            EditorUtility.revealInEditor(editor, node.getOffset(), node.getLength());
          }
          if (element instanceof AnalysisError) {
            AnalysisError error = (AnalysisError) element;
            EditorUtility.revealInEditor(editor, error.getOffset(), error.getLength());
          }
        }
      }

    }

  }

  private class ExplorerContentProvider implements IStructuredContentProvider, ITreeContentProvider {

    private final ASTNode[] NO_NODES = new ASTNode[0];

    @Override
    public void dispose() {
    }

    @Override
    public Object[] getChildren(Object parent) {
      if (parent instanceof ASTNode) {
        ASTNode node = (ASTNode) parent;
        CollectingVisitor nodeCollector = new CollectingVisitor();
        node.visitChildren(nodeCollector);
        return nodes(nodeCollector, parent);
      }

      return NO_NODES;
    }

    @Override
    public Object[] getElements(Object parent) {
      return getChildren(parent);
    }

    @Override
    public Object getParent(Object child) {
      if (child instanceof ASTNode) {
        return ((ASTNode) child).getParent();
      }
      return null;
    }

    @Override
    public boolean hasChildren(Object parent) {
      return getChildren(parent).length > 0;
    }

    @Override
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
      viewer.getControl().setRedraw(false);
      ((TreeViewer) viewer).refresh(true);
      viewer.getControl().setRedraw(true);
    }

    private Object[] nodes(CollectingVisitor nodeCollector, Object parent) {

      List<? extends Object> nodes = nodeCollector.getNodes();

      if (parent instanceof CompilationUnit) {
        ArrayList<Object> nodesAndErrors = new ArrayList<Object>(nodes);
        nodesAndErrors.addAll(errors);
        return nodesAndErrors.toArray();
      }

      return nodes.toArray();
    }

  }

  private static class ExplorerLabelProvider extends LabelProvider {

    @Override
    public Image getImage(Object obj) {
      String img = obj instanceof AnalysisError ? "error_obj.gif" : "brkpi_obj.gif";
      return DartDevPlugin.getImage(img);
    }

    @Override
    public String getText(Object obj) {
      if (obj instanceof AnalysisError) {
        return ((AnalysisError) obj).getMessage();
      }
      StringBuilder builder = new StringBuilder();
      builder.append(obj.getClass().getSimpleName());
      if (obj instanceof ASTNode) {
        ASTNode node = (ASTNode) obj;
        builder.append(" [");
        builder.append(node.getOffset());
        builder.append("..");
        builder.append(node.getOffset() + node.getLength() - 1);
        builder.append("]");
        String name = getName(node);
        if (name != null) {
          builder.append(" - ");
          builder.append(name);
        }
      }
      return builder.toString();
    }

    /**
     * Return the name of the given node, or {@code null} if the given node is not a declaration.
     * 
     * @param node the node whose name is to be returned
     * @return the name of the given node
     */
    private String getName(ASTNode node) {
      // TODO(brianwilkerson) Rewrite this to use a visitor.
      if (node instanceof ConstructorDeclaration) {
        ConstructorDeclaration cd = (ConstructorDeclaration) node;
        if (cd.getName() == null) {
          return cd.getReturnType().getName();
        } else {
          return cd.getReturnType().getName() + '.' + cd.getName().getName();
        }
      } else if (node instanceof FieldDeclaration) {
        return getNames(((FieldDeclaration) node).getFields());
      } else if (node instanceof MethodDeclaration) {
        return ((MethodDeclaration) node).getName().getName();
      } else if (node instanceof ClassDeclaration) {
        return ((ClassDeclaration) node).getName().getName();
      } else if (node instanceof ClassTypeAlias) {
        return ((ClassTypeAlias) node).getName().getName();
      } else if (node instanceof FunctionDeclaration) {
        SimpleIdentifier nameNode = ((FunctionDeclaration) node).getName();
        if (nameNode != null) {
          return nameNode.getName();
        }
      } else if (node instanceof FunctionTypeAlias) {
        return ((FunctionTypeAlias) node).getName().getName();
      } else if (node instanceof Identifier) {
        return ((Identifier) node).getName();
      } else if (node instanceof TopLevelVariableDeclaration) {
        return getNames(((TopLevelVariableDeclaration) node).getVariables());
      } else if (node instanceof TypeParameter) {
        return ((TypeParameter) node).getName().getName();
      } else if (node instanceof VariableDeclaration) {
        return ((VariableDeclaration) node).getName().getName();
      }
      return null;
    }

    /**
     * Return a string containing a comma-separated list of the names of all of the variables in the
     * given list.
     * 
     * @param variables the list containing the variables
     * @return a comma-separated list of the names of the given variables
     */
    private String getNames(VariableDeclarationList variables) {
      boolean first = true;
      StringBuilder builder = new StringBuilder();
      for (VariableDeclaration variable : variables.getVariables()) {
        if (first) {
          first = false;
        } else {
          builder.append(", ");
        }
        builder.append(variable.getName().getName());
      }
      return builder.toString();
    }
  }

  /**
   * View extension id.
   */
  public static final String ID = "com.google.dart.dev.util.ast.ASTExplorer";

  private TreeViewer viewer;

  private Action expandAllAction;
  private Action collapseAllAction;

  private EventHandler eventHandler = new EventHandler();

  private ArrayList<AnalysisError> errors = new ArrayList<AnalysisError>();

  @Override
  public void createPartControl(Composite parent) {

    viewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
    viewer.setContentProvider(new ExplorerContentProvider());
    viewer.setLabelProvider(new ExplorerLabelProvider());

    makeActions();
    contributeToActionBars();
    hookupListeners();

    refresh();
  }

  @Override
  public void dispose() {
    getSelectionService().removeSelectionListener(eventHandler);
    getPage().removePartListener(eventHandler);
  }

  @Override
  public void onError(AnalysisError error) {
    errors.add(error);
  }

  @Override
  public void setFocus() {
    refresh();
    viewer.getControl().setFocus();
  }

  protected IEditorPart getActiveEditor() {
    return getPage().getActiveEditor();
  }

  protected IWorkbenchPage getPage() {
    return getSite().getPage();
  }

  protected ISelectionService getSelectionService() {
    return (ISelectionService) getSite().getService(ISelectionService.class);
  }

  private void contributeToActionBars() {
    IActionBars bars = getViewSite().getActionBars();
    contributeViewToolItems(bars.getToolBarManager());
  }

  private void contributeViewToolItems(IToolBarManager manager) {
    manager.add(expandAllAction);
    manager.add(collapseAllAction);
  }

  private CompilationUnit getCompilationUnit() {
    IEditorPart editor = getActiveEditor();
    if (editor != null) {

      IEditorInput input = editor.getEditorInput();
      if (input instanceof IFileEditorInput) {
        IFile file = ((IFileEditorInput) input).getFile();
        try {

          String contents = IFileUtilities.getContents(file);

          errors.clear();

          StringScanner scanner = new StringScanner(null, contents, this);
          Parser parser = new Parser(null, this);

          Token token = scanner.tokenize();
          return parser.parseCompilationUnit(token);
        } catch (CoreException e) {
          DartDevPlugin.logError(e);
        } catch (IOException e) {
          DartDevPlugin.logError(e);
        }
      }
    }
    return null;
  }

  private void hookupListeners() {
    viewer.addSelectionChangedListener(eventHandler);
    getSelectionService().addSelectionListener(eventHandler);
    getPage().addPartListener(eventHandler);
  }

  private void makeActions() {

    expandAllAction = new Action() {
      @Override
      public void run() {
        if (!viewer.getControl().isDisposed()) {
          viewer.getControl().setRedraw(false);
          viewer.expandToLevel(viewer.getInput(), AbstractTreeViewer.ALL_LEVELS);
          viewer.getControl().setRedraw(true);
        }
      }
    };
    expandAllAction.setText("Expand All");
    expandAllAction.setToolTipText("Expand All");
    expandAllAction.setImageDescriptor(DartDevPlugin.getImageDescriptor("expandall.gif"));

    collapseAllAction = new Action() {
      @Override
      public void run() {
        if (!viewer.getControl().isDisposed()) {
          viewer.getControl().setRedraw(false);
          viewer.collapseToLevel(viewer.getInput(), AbstractTreeViewer.ALL_LEVELS);
          viewer.getControl().setRedraw(true);
        }
      }
    };
    collapseAllAction.setText("Collapse All");
    collapseAllAction.setToolTipText("Collapse All");
    collapseAllAction.setImageDescriptor(DartDevPlugin.getImageDescriptor("collapseall.gif"));

  }

  private void refresh() {
    viewer.setInput(getCompilationUnit());
  }

  private void selectNodeAtOffset(int offset, int length) {
    //TODO(pquitslund): implement
    if (length <= 0) {
      return;
    }
//    NodeLocator locator = new NodeLocator(offset, offset + length - 1);
//    ASTNode node = locator.searchWithin(getCompilationUnit());
//    viewer.setSelection(new StructuredSelection(node), true);
  }

}
