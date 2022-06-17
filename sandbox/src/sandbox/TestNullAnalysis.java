package sandbox;

import org.openrewrite.Cursor;
import org.openrewrite.java.dataflow2.*;
import org.openrewrite.java.dataflow2.examples.IsNullAnalysis;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.openrewrite.java.dataflow2.ModalBoolean.*;
import static org.openrewrite.java.dataflow2.ModalBoolean.True;
import static sandbox.TestUtils.parse;

public class TestNullAnalysis {
    public static void test()
    {
        testAPI();
    }

    public static void testAPI() {
        // Test the value of 's' at the end of given code fragment.


//        String source =
//                "class C {\n" +
//                        "    void a() {} \n" +
//                        "    void b() {} \n" +
//                        "    void m(String u, String v) { \n" +
//                        "        a(); \n" +
//                        "        String s = null; while(x == 0) { s = \"a\"; } \n" +
//                        "        b(); \n" +
//                        "        if(!x.isValid()) { throw e; }" +
//                        "        // state = { x is valid }" +
//                        "        c();" +
//                        "    }\n" +
//                        "}\n" +
//                        "" ;
//
//        J.CompilationUnit cu = parse(source);
//
//        // body -> x == 0 -> 0 -> x -> { body, s = null } -> ...
//        TestUtils.assertPrevious(cu,"b()", ENTRY, "(x == 0)", "{ s = \"a\"; }");
//        TestUtils.assertPrevious(cu,"(x == 0)", EXIT, "x == 0");
//        TestUtils.assertPrevious(cu,"(x == 0)", ENTRY, "s = null", "{ s = \"a\"; }");
//        TestUtils.assertPrevious(cu,"x == 0", EXIT, "x == 0");
//        TestUtils.assertPrevious(cu,"x == 0", ENTRY, "0");
//        TestUtils.assertPrevious(cu,"0", EXIT, "0");
//        TestUtils.assertPrevious(cu,"0", ENTRY, "x");
//        TestUtils.assertPrevious(cu,"x", EXIT, "x");
//        TestUtils.assertPrevious(cu,"x", ENTRY, "s = null", "{ s = \"a\"; }");
//        TestUtils.assertPrevious(cu,"{ s = \"a\"; }", EXIT, "s = \"a\"");
//        TestUtils.assertPrevious(cu,"{ s = \"a\"; }", ENTRY, "(x == 0)");
//        TestUtils.assertPrevious(cu,"s = \"a\"", EXIT, "s = \"a\"");
//        TestUtils.assertPrevious(cu,"s = \"a\"", ENTRY, "\"a\"");
//        TestUtils.assertPrevious(cu,"\"a\"", EXIT, "\"a\"");
//        TestUtils.assertPrevious(cu,"\"a\"", ENTRY, "(x == 0)");

//        testIsSNull("String s = null; while(x == 0) { s = \"a\"; }", CantTell);

//        testIsSNull("String s = null; while(c) { s = \"a\"; }", CantTell);
//        testIsSNull("String s = null; while(c) { s = null; }", DefinitelyYes);
//        testIsSNull("String s = \"a\"; while(c) { s = null; }", CantTell);
//        testIsSNull("String s = \"a\"; while(c) { s = \"b\"; }", DefinitelyNo);
//        testIsSNull("String s; while((s = null) == null) { s = \"a\"; }", CantTell);
//        testIsSNull("String s; while((s = null) == null) { s = null; }", DefinitelyYes);
//        testIsSNull("String s; while((s = \"a\") == null) { s = null; }", CantTell);
//        testIsSNull("String s; while((s = \"a\") == null) { s = \"b\"; }", DefinitelyNo);

        testIsSNull("String s = f(); if(s == null) { s = \"a\"; }", False);
        // Understanding that s is always null below requires constant propagation
        // and partial evaluation of the condition
        testIsSNull("String s = null; if(s == \"b\") { s = \"a\"; }", Conflict);

        testIsSNull("String s, t; t = (s = null);", True);
        testIsSNull("String s, t; s = (t = null);", True);
        testIsSNull("String s = \"a\", t, u; t = (u = null);", False);

        testIsSNull("String s = null;", True);
        testIsSNull("String s = \"abc\";", False);
        testIsSNull("String s; s = null; s = \"abc\";", False);
        testIsSNull("String s; s = \"abc\"; s = null;", True);
        testIsSNull("String q = null; String s = q;", True);
        testIsSNull("String q = \"abc\"; String s = q;", False);
        testIsSNull("String s = null + null;", False);
        testIsSNull("String s = \"a\" + null;", False);
        testIsSNull("String s = null + \"b\";", False);
        testIsSNull("String s = \"a\" + \"b\";", False);
        testIsSNull("String s = u;", null); // Because u is undefined
        testIsSNull("String u = null; String s = u;", True);
        testIsSNull("String s = \"a\".toUpperCase();", False);
        testIsSNull("String s = \"a\".unknownMethod(s, null);", NoIdea);
        testIsSNull("String s; if(c) { s = null; } else { s = null; }", True);
        testIsSNull("String s; if(c) { s = null; } else { s = \"b\"; }", Conflict);
        testIsSNull("String s; if(c) { s = \"a\"; } else { s = null; }", Conflict);
        testIsSNull("String s; if(c) { s = \"a\"; } else { s = \"b\"; }", False);
        testIsSNull("String s, q; if((s = null) == null) { q = \"a\"; } else { q = null; }",
                True);


    }

    /**
     * Test the value of 's' at the end of given code fragment.
     */
    public static void testIsSNull(String fragment, ModalBoolean expected) {
        String source =
                "class C {\n" +
                        "    void a() {} \n" +
                        "    void b() {} \n" +
                        "    void m(String u, String v) { \n" +
                        "        a(); \n" +
                        "        __FRAGMENT__ \n" +
                        "        b(); \n" +
                        "    }\n" +
                        "}\n" +
                        "" ;

        source = source.replace("__FRAGMENT__", fragment);

        J.CompilationUnit cu = parse(source);

        //new PrintProgramPointsVisitor().visit(cu, null);

        String pp1 = "b()";
        Cursor c1 = Utils.findProgramPoint(cu, pp1);
        assertThat(c1).withFailMessage("program point <" + pp1 + "> not found").isNotNull();

        String pp2 = "s";
        JavaType.Variable v = Utils.findVariable(cu, pp2);
        assertThat(v).isNotNull();

        DataFlowGraph dfg = new DataFlowGraph(cu);
        ProgramState state = new IsNullAnalysis(dfg).inputState(c1, new TraversalControl<>());

//
//        IsNullAnalysis a = new IsNullAnalysis(dfg);
//        a.analyze(c1, new TraversalControl<>());
//        ProgramState state = a.inputState(c1, new TraversalControl<>());

        System.out.println(fragment + "\n    Is 's' null when entering point 'b()' ? " + state.get(v));

        assertThat(state.get(v)).isEqualTo(expected);
    }

}
