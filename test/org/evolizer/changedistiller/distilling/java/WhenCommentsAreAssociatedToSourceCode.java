package org.evolizer.changedistiller.distilling.java;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Enumeration;
import java.util.List;

import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;
import org.evolizer.changedistiller.compilation.java.JavaCompilation;
import org.evolizer.changedistiller.model.classifiers.EntityType;
import org.evolizer.changedistiller.model.classifiers.SourceRange;
import org.evolizer.changedistiller.model.classifiers.java.JavaEntityType;
import org.evolizer.changedistiller.model.entities.SourceCodeEntity;
import org.evolizer.changedistiller.treedifferencing.Node;
import org.evolizer.changedistiller.util.CompilationUtils;
import org.junit.BeforeClass;
import org.junit.Test;

public class WhenCommentsAreAssociatedToSourceCode extends JavaDistillerTestCase {

    private static JavaCompilation sCompilation;
    private static List<Comment> sComments;
    private static Node sRoot;

    @BeforeClass
    public static void prepareCompilationUnit() throws Exception {
        sCompilation = CompilationUtils.compileFile("ClassWithCommentsToAssociate.java");
        List<Comment> comments = CompilationUtils.extractComments(sCompilation);
        CommentCleaner visitor = new CommentCleaner(sCompilation.getSource());
        for (Comment comment : comments) {
            visitor.process(comment);
        }
        sComments = visitor.getComments();
        sRoot = new Node(JavaEntityType.METHOD, "foo");
        sRoot.setEntity(new SourceCodeEntity("foo", JavaEntityType.METHOD, new SourceRange()));
        AbstractMethodDeclaration method = CompilationUtils.findMethod(sCompilation.getCompilationUnit(), "foo");
        JavaMethodBodyConverter bodyT = sInjector.getInstance(JavaMethodBodyConverter.class);
        bodyT.initialize(sRoot, method, sComments, sCompilation.getScanner());
        method.traverse(bodyT, (ClassScope) null);
    }

    @Test
    public void proximityRatingShouldAssociateCommentToClosestEntity() throws Exception {
        Node node = findNode("boolean check = (number > 0);");
        assertCorrectAssociation(node, "// check if number is greater than -1", JavaEntityType.LINE_COMMENT);
    }

    @Test
    public void undecidedProximityRatingShouldAssociateCommentToNextEntity() throws Exception {
        Node node = findNode("check");
        assertCorrectAssociation(
                node,
                "// check the interesting number\n        // and some new else",
                JavaEntityType.LINE_COMMENT);
    }

    @Test
    public void commentInsideBlockShouldBeAssociatedInside() throws Exception {
        Node node = findNode("a = (23 + Integer.parseInt(\"42\"));");
        assertCorrectAssociation(
                node,
                "/* A block comment\n             * with stars\n             */",
                JavaEntityType.BLOCK_COMMENT);
        node = findNode("b = Math.abs(number);");
        assertCorrectAssociation(node, "/* inside else */", JavaEntityType.BLOCK_COMMENT);
    }

    @Test
    public void commentInsideSimpleStatementShouldBeAssociatedToThatStatement() throws Exception {
        Node node = findNode("b = Math.round(Math.random());");
        assertCorrectAssociation(node, "/* inner comment */", JavaEntityType.BLOCK_COMMENT);
    }

    private void assertCorrectAssociation(Node node, String expectedComment, EntityType expectedCommentType) {
        List<Node> associatedNodes = node.getAssociatedNodes();
        assertThat(associatedNodes.size(), is(1));
        assertThat(associatedNodes.get(0).getValue(), is(expectedComment));
        assertThat(associatedNodes.get(0).getLabel(), is(expectedCommentType));
        assertThat(associatedNodes.get(0).getAssociatedNodes().get(0), is(node));
    }

    @SuppressWarnings("unchecked")
    private Node findNode(String value) {
        for (Enumeration<Node> e = sRoot.breadthFirstEnumeration(); e.hasMoreElements();) {
            Node node = e.nextElement();
            if (node.getValue().equals(value)) {
                return node;
            }
        }
        return null;
    }

}
