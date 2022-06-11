package sandbox;

import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.Java11Parser;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.dataflow2.*;
import org.openrewrite.java.dataflow2.examples.IsNullAnalysis;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Statement;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.openrewrite.java.dataflow2.ProgramPoint.ENTRY;
import static org.openrewrite.java.dataflow2.ProgramPoint.EXIT;
import static org.openrewrite.java.dataflow2.ModalBoolean.*;

public class Main {

    public static boolean debug = false;

    private static J.CompilationUnit parse(String src) {
        Java11Parser parser = new Java11Parser.Builder().build();
        ExecutionContext ctx = new InMemoryExecutionContext();
        List<J.CompilationUnit> cus = parser.parse(ctx, src);
        J.CompilationUnit cu = cus.get(0);
        return cu;
    }

    public static void main(String[] args)
    {
        //testAPI();
        testNonLocalExits();
        //testForLoop();
        //testVariableDeclarations();
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
        testIsSNull("String s = null; if(s == \"b\") { s = \"a\"; }", True);

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
        testIsSNull("String s = u;", null); // CantTell
        testIsSNull("String s = \"a\".toUpperCase();", False);
        testIsSNull("String s = \"a\".unknownMethod(s, null);", Conflict);
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

    public static void testPrevious(String fragment, String pp, ProgramPoint entryOrExit, String... previous) {
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

        TestUtils.assertPrevious(cu,pp, entryOrExit, previous);
    }
    public static void testVariableDeclarations()
    {
        String source =
                    "class C {\n" +
                    "    void a() {} \n" +
                    "    void b() {} \n" +
                    "    void m() {\n" +
                    "        a();\n" +
                    "        int i = u + v, j = w;\n" +
                    "        b();\n" +
                    "    }\n" +
                    "}\n" +
                    "";

        J.CompilationUnit cu = parse(source);
        DataFlowGraph dfg = new DataFlowGraph(cu);
        new PrintProgramPointsVisitor(dfg).visit(cu, null);

        TestUtils.assertPrevious(cu,"b()", ENTRY, "j = w");
        TestUtils.assertPrevious(cu,"j = w", EXIT, "j = w");
        TestUtils.assertPrevious(cu,"j = w", ENTRY, "w");
        TestUtils.assertPrevious(cu,"w", EXIT,"w");
        TestUtils.assertPrevious(cu,"w", ENTRY,"i = u + v");
        TestUtils.assertPrevious(cu,"i = u + v", EXIT, "i = u + v");
        TestUtils.assertPrevious(cu,"i = u + v", ENTRY, "u + v");
        TestUtils.assertPrevious(cu,"u + v", EXIT, "u + v");
        TestUtils.assertPrevious(cu,"u + v", ENTRY, "v");
        TestUtils.assertPrevious(cu,"v", EXIT, "v");
        TestUtils.assertPrevious(cu,"v", ENTRY, "u");
        TestUtils.assertPrevious(cu,"u", EXIT, "u");
        TestUtils.assertPrevious(cu,"u", ENTRY, "a");

    }

    public static void testNonLocalExits()
    {
        String source =
                "class C {\n" +
                        "    void m() { \n" +
                        "        a(); \n" +
                        "        if(x) return; \n" +
                        "        b(); \n" +
//                        "        if(y) throw e; \n" +
//                        "        try { \n" +
//                        "            throw new Exception(1); \n" +
//                        "        } catch(Exception e) {" +
//                        "        } \n" +
//                        "        try { \n" +
//                        "            throw new Exception(2); \n" +
//                        "        } catch(IllegalStateException e) {" +
//                        "        } \n" +
                        // also add loops with break and continue
                        "         \n" +
                        "         \n" +
                        "         \n" +
                        "         \n" +
                        "         \n" +
                        "         \n" +
                        "        b(); \n" +
                        // implicit return
                        "    } \n" +
                        "} \n" +
                        "";

        J.CompilationUnit cu = parse(source);
        DataFlowGraph dfg = new DataFlowGraph(cu);
        new PrintProgramPointsVisitor(dfg).visit(cu, null);

        J.MethodDeclaration m = (J.MethodDeclaration) cu.getClasses().get(0).getBody().getStatements().get(0);
        System.out.println(Utils.print(m));

        TestUtils.assertPrevious(cu,Utils.print(m), EXIT);

    }

    public static void testForLoop()
    {
        String source =
            "class C { \n" +
            "    void a() {} \n" +
            "    void b() {} \n" +
            "    void f() {} \n" +
            "    void g() {} \n" +
            "    void h() {} \n" +
            "    void method() { \n" +
            "       a(); \n" +
            "       for(int i=0, j=1; 2<3; m++, n++) { \n" +
            "           f(); g(); h(); \n" +
            "       } \n" +
            "       b(); \n" +
            "    } \n" +
            "} \n" +
            "" ;

        // init;
        // if(cond) {
        //     body;
        //     update;
        // }

        J.CompilationUnit cu = parse(source);
        DataFlowGraph dfg = new DataFlowGraph(cu);
        new PrintProgramPointsVisitor(dfg).visit(cu, null);

        TestUtils.assertPrevious(cu,"b()", ENTRY, "2<3", "n++");

        TestUtils.assertPrevious(cu,"n++", ENTRY,"m++");
        TestUtils.assertPrevious(cu,"n++", EXIT,"n");
        TestUtils.assertPrevious(cu,"n", ENTRY, "m++");
        TestUtils.assertPrevious(cu,"n", EXIT, "n");
        TestUtils.assertPrevious(cu,"m++", ENTRY,"h");
        TestUtils.assertPrevious(cu,"m++", EXIT,"m");
        TestUtils.assertPrevious(cu,"m", ENTRY,"h");
        TestUtils.assertPrevious(cu,"m", EXIT,"m");

        TestUtils.assertPrevious(cu,"h()", ENTRY,"g");
        TestUtils.assertPrevious(cu,"h()", EXIT,"h");
        TestUtils.assertPrevious(cu,"g()", ENTRY,"f");
        TestUtils.assertPrevious(cu,"g()", EXIT,"g");
        TestUtils.assertPrevious(cu,"f()", ENTRY,"2<3");
        TestUtils.assertPrevious(cu,"f()", EXIT,"f");
        TestUtils.assertPrevious(cu,"{ f(); g(); h(); }", ENTRY,"2<3");
        TestUtils.assertPrevious(cu,"{ f(); g(); h(); }", EXIT,"h");

        TestUtils.assertPrevious(cu,"2<3", ENTRY,"3");
        TestUtils.assertPrevious(cu,"2<3", EXIT,"2<3");
        TestUtils.assertPrevious(cu,"3", ENTRY, "2");
        TestUtils.assertPrevious(cu,"3", EXIT,"3");
        TestUtils.assertPrevious(cu, "2", ENTRY, "j=1", "n++");
        TestUtils.assertPrevious(cu, "2", EXIT, "2");

        TestUtils.assertPrevious(cu,"int i=0, j=1", ENTRY, "a");
        TestUtils.assertPrevious(cu,"int i=0, j=1", EXIT, "j=1");
        TestUtils.assertPrevious(cu,"i=0", ENTRY,"0");
        TestUtils.assertPrevious(cu,"i=0", EXIT,"i=0");
        TestUtils.assertPrevious(cu,"0", ENTRY,"a");
        TestUtils.assertPrevious(cu,"0", EXIT,"0");
        TestUtils.assertPrevious(cu,"j=1", ENTRY,"1");
        TestUtils.assertPrevious(cu,"j=1", EXIT,"j=1");
        TestUtils.assertPrevious(cu,"1", ENTRY, "i=0");
        TestUtils.assertPrevious(cu,"1", EXIT,"1");
    }

    public static void main2()
    {
        Java11Parser parser = new Java11Parser.Builder().build();
        String source =
            "class C {\n" +
            "   void m() {\n" +
            "       a(); int x = 1 + 2; b();" +
//            "       int x = 0, y = 1;\n" +
//            "       for(int i=0, j=2*i+1; i<x; i++, j++) {\n" +
//            "           f(x); g(x); h(x);\n" +
//            "       }\n" +
//            "       a();" +
//            "       if(x == y+1) { u(x); } else { v(x); }\n" +
//            "       while(x == 0) { w(x); }\n" +
//            "       o.m(p,q,r);\n" +
//            "       b();\n" +
//            "       o.m(1+x++, 2+(y++ *3));\n" +
//            "       x = 1+(2*y);\n" +
            "   }\n" +
            "}\n"
            ;

        J.CompilationUnit cu = parse(source);

        // new MyVisitor().visit(cu, null);

        /*
        J.Block classBody = cu.getClasses().get(0).getBody();
        J.MethodDeclaration methodDecl = (J.MethodDeclaration) classBody.getStatements().get(0);
        List<Statement> methodStatements = methodDecl.getBody().getStatements();
        Statement stmt1 = methodStatements.get(1);
        Statement stmt2 = methodStatements.get(2);

        J.VariableDeclarations.NamedVariable xVar = ((J.VariableDeclarations)methodStatements.get(0)).getVariables().get(0);
        J.VariableDeclarations.NamedVariable yVar = ((J.VariableDeclarations)methodStatements.get(0)).getVariables().get(1);

        JavaType.Variable x = xVar.getVariableType();
        JavaType.Variable y = yVar.getVariableType();

        System.out.println("stmt1 = " + stmt1);
        System.out.println("stmt1 reads x = " + stmt1.reads(x));
        System.out.println("stmt1 writes x = " + stmt1.writes(x));
        System.out.println("stmt1 reads y = " + stmt1.reads(y));
        System.out.println("stmt1 writes y = " + stmt1.writes(y));

        System.out.println("stmt2 = " + stmt2);
        System.out.println("stmt2 reads x = " + stmt2.reads(x));
        System.out.println("stmt2 writes x = " + stmt2.writes(x));
        System.out.println("stmt2 reads y = " + stmt2.reads(y));
        System.out.println("stmt2 writes y = " + stmt2.writes(y));

        cu.print();
         */
    }

}

class PrintProgramPointsVisitor extends JavaIsoVisitor {

    private final DataFlowGraph dfg;

    public PrintProgramPointsVisitor(DataFlowGraph dfg) {
        this.dfg = dfg;
    }
    
    public static String print(Cursor c)
    {
        if(c.getValue() instanceof ProgramPoint) {
            ProgramPoint p = (ProgramPoint) c.getValue();
            return p.printPP(c).replace("\n", " ").replaceAll("[ ]+", " ").trim();
        } else if(c.getValue() instanceof Collection) {
            return (String) ((Collection)c.getValue()).stream().map(e -> print(new Cursor(c, e))).collect(Collectors.joining("; "));
        } else {
            throw new IllegalStateException();
        }
    }

    @Override
    public Statement visitStatement(Statement statement, Object o) {
        System.out.println("statement = <" + print(getCursor()) + "> " + statement.getClass().getSimpleName());
        if (print(getCursor()).equals("0")) {
            System.out.println();
        }
        Collection<Cursor> pp = dfg.previous(getCursor());
        if (pp == null) {
            System.out.println("   (prevs = null)");
            dfg.previous(getCursor());
        } else {
            for (Cursor p : pp) {
                System.out.println("   prev = " + print(p));
            }
        }
        return super.visitStatement(statement, o);
    }

    @Override
    public Expression visitExpression(Expression expr, Object o) {
        if (expr instanceof J.Primitive) {
            System.out.println("skipping J.Primitive");
        } else {
            System.out.println("expression = <" + print(getCursor()) + "> " + expr.getClass().getSimpleName());
            if (expr instanceof J.Binary) {
                J.Binary b = (J.Binary) expr;
                System.out.println("          " +
                        print(new Cursor(getCursor(), b.getLeft())) +
                        "  " + b.getOperator() + "  " +
                        print(new Cursor(getCursor(), b.getRight()))
                );
            }
            if (print(getCursor()).equals("xxx")) {
                Main.debug = true;
            }
            Collection<Cursor> pp = dfg.previous(getCursor());
            if (pp == null) {
                System.out.println("   (prevs = null)");
                dfg.previous(getCursor());
            } else {
                for (Cursor p : pp) {
                    System.out.println("   prev = " + print(p));
                }
            }
        }
        return super.visitExpression(expr, o);
    }

    @Override
    public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, Object o) {
        System.out.println("variable = <" + print(getCursor()) + "> " + variable.getClass().getSimpleName());
        if (print(getCursor()).equals("0")) {
            System.out.println();
        }
        Collection<Cursor> pp = dfg.previous(getCursor());
        if (pp == null) {
            System.out.println("   (null)");
        } else {
            for (Cursor p : pp) {
                System.out.println("   prev = " + print(p));
            }
        }
        return super.visitVariable(variable, o);
        //return variable.withInitializer(visitExpression(variable.getInitializer(), null));
    }
}


