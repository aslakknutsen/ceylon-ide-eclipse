package com.redhat.ceylon.eclipse.code.quickfix;

import static com.redhat.ceylon.eclipse.code.outline.CeylonLabelProvider.CORRECTION;

import java.util.Collection;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.text.edits.InsertEdit;

import com.redhat.ceylon.compiler.typechecker.tree.Node;
import com.redhat.ceylon.compiler.typechecker.tree.Tree;
import com.redhat.ceylon.compiler.typechecker.tree.Tree.Annotation;
import com.redhat.ceylon.compiler.typechecker.tree.Tree.AnnotationList;
import com.redhat.ceylon.compiler.typechecker.tree.Tree.AnyAttribute;
import com.redhat.ceylon.compiler.typechecker.tree.Tree.AnyMethod;
import com.redhat.ceylon.compiler.typechecker.tree.Tree.BaseMemberExpression;
import com.redhat.ceylon.compiler.typechecker.tree.Tree.ClassOrInterface;
import com.redhat.ceylon.compiler.typechecker.tree.Tree.CompilationUnit;
import com.redhat.ceylon.compiler.typechecker.tree.Tree.ImportPath;
import com.redhat.ceylon.compiler.typechecker.tree.Tree.MemberOrTypeExpression;
import com.redhat.ceylon.compiler.typechecker.tree.Tree.ModuleDescriptor;
import com.redhat.ceylon.compiler.typechecker.tree.Tree.ObjectDefinition;
import com.redhat.ceylon.compiler.typechecker.tree.Tree.PackageDescriptor;
import com.redhat.ceylon.compiler.typechecker.tree.Tree.Parameter;
import com.redhat.ceylon.compiler.typechecker.tree.Tree.QuotedLiteral;
import com.redhat.ceylon.compiler.typechecker.tree.Tree.Type;
import com.redhat.ceylon.eclipse.code.editor.Util;
import com.redhat.ceylon.eclipse.util.FindContainerVisitor;

public class AddDocAnnotationProposal extends ChangeCorrectionProposal {
    
    public static void addDocAnnotationProposal(Collection<ICompletionProposal> proposals, Node node, CompilationUnit cu, IFile file, IDocument doc) {
        node = determineNode(node, cu);
        if (node == null) {
            return;
        }
        
        if (isAlreadyPresent(node)) {
            return;
        }
        
        StringBuilder docBuilder = new StringBuilder("doc \"\"");
        if (node instanceof Parameter) {
            docBuilder.append(" ");
        } else {
            docBuilder.append(System.getProperty("line.separator"));
            docBuilder.append(CeylonQuickFixAssistant.getIndent(node, doc));
        }

        TextFileChange change = new TextFileChange("Add doc annotation", file);
        change.setEdit(new InsertEdit(node.getStartIndex(), docBuilder.toString()));

        AddDocAnnotationProposal proposal = new AddDocAnnotationProposal(change, file, node.getStartIndex() + 5);
        if (!proposals.contains(proposal)) {
            proposals.add(proposal);
        }
    }

    private static Node determineNode(Node node, CompilationUnit cu) {
        if( node instanceof Type || 
                node instanceof MemberOrTypeExpression || 
                node instanceof ImportPath ||
                node instanceof QuotedLiteral) {
            FindContainerVisitor fcv = new FindContainerVisitor(node);
            fcv.visit(cu);
            node = fcv.getStatementOrArgument();
        }
        if( node instanceof ClassOrInterface ||
                node instanceof AnyAttribute ||
                node instanceof AnyMethod ||
                node instanceof ObjectDefinition ||
                node instanceof Parameter ||
                node instanceof ModuleDescriptor ||
                node instanceof PackageDescriptor ) {
            return node;
        }
        return null;
    }
    
    private static boolean isAlreadyPresent(Node node) {
        AnnotationList annotationList = null;

        if (node instanceof Tree.Declaration) {
            annotationList = ((Tree.Declaration) node).getAnnotationList();
        } else if (node instanceof ModuleDescriptor) {
            annotationList = ((ModuleDescriptor) node).getAnnotationList();
        } else if (node instanceof PackageDescriptor) {
            annotationList = ((PackageDescriptor) node).getAnnotationList();
        }
        
        if (annotationList != null) {
            for (Annotation annotation : annotationList.getAnnotations()) {
                if (annotation.getPrimary() instanceof BaseMemberExpression) {
                    String annotationName = ((BaseMemberExpression) annotation.getPrimary()).getIdentifier().getText();
                    if ("doc".equals(annotationName)) {
                        return true;
                    }
                }
            }
        }
        
        return false;
    }
    

    private IFile file;
    private int offset;

    private AddDocAnnotationProposal(Change change, IFile file, int offset) {
        super("Add 'doc' annotation", change, 10, CORRECTION);
        this.file = file;
        this.offset = offset;
    }
    
    @Override
    public void apply(IDocument document) {
        super.apply(document);
        Util.gotoLocation(file, offset);
    }    

}