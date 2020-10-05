import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitor;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.utils.ParserCollectionStrategy;
import com.github.javaparser.utils.ProjectRoot;
import com.github.javaparser.utils.SourceRoot;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class LineCalculator {

    public static String answer = "";

    //General version of LOC
    public static int LOC(String[] lines) {
        int count = 0;
        for (String line : lines) {
            if(!line.isEmpty()) {
                ++count;
            }
        }

        return count;
    }

    //General version of CLOC
    public static int CLOC(String[] lines) {
        int count = 0;
        for (String line : lines) {
            line = line.replaceAll("\\s", "");
            line = line.replaceAll("\\t", "");
            if(isComment(line)) {
                ++count;
            }
        }
        // System.out.println("CLOC: " + count);
        return count;
    }

    //Check if line is a comment
    private static boolean isComment(String line) {
        return  line.matches("^\\/\\/.*") ||  //Match single line comment
                line.matches("^\\/\\*.*") ||  //Match block comment
                line.matches("^\\*.*")    ||  //Match block comment
                line.matches("^\\/\\*\\*.*"); //Match block comment
    }

    public static void write(String fileName, String output) {
        try {
            FileWriter fw = new FileWriter(fileName);
            BufferedWriter bw = new BufferedWriter(fw);

            bw.append(output);
            bw.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {

        Path projectPath = Paths.get("/home/unknown/Documents/projects/Flappy Ghost");
        ProjectRoot projectRoot = new ParserCollectionStrategy().collect(projectPath);
        List<SourceRoot>  srcRoots = projectRoot.getSourceRoots();
        for (SourceRoot sourceRoot: srcRoots) {
            sourceRoot.tryToParse();
            List<CompilationUnit> compilations = sourceRoot.getCompilationUnits();
            for (CompilationUnit n: compilations) {
                //Create visitor for methods
                VoidVisitor<?> methodLinesVisitor = new MethodsLineCounter(n.getStorage().get().getPath().toString());
                //Create visitor for classes
                VoidVisitor<?> classLinesVisitor = new ClassesLineCounter(n.getStorage().get().getPath().toString());
                //Call visit method for classes
                answer +=  "chemin, class, classe_LOC, classe_CLOC, classe_DC, WMC, classe_BC\n";
                classLinesVisitor.visit(n, null);
                //Call visit method for methods
                answer += "chemin, class, methode, methode_LOC, methode_CLOC, methode_DC, CC, methode_BC\n";
                methodLinesVisitor.visit(n, null);
                answer += "\n";
            }
        }
        write("output.csv", answer);
    }

    //Implementation of the line visitor for methods
    private static class MethodsLineCounter extends VoidVisitorAdapter<Void> {

        private String path = "";

        public MethodsLineCounter(String path) {
            this.path = path;
        }

        public int method_LOC(String[] lines) {
            return LineCalculator.LOC(lines);
        }

        public int method_CLOC(String[] lines) {
            return LineCalculator.CLOC(lines);
        }

        public float method_DC(String[] lines) {
            return (float)method_CLOC(lines) / method_LOC(lines);
        }

        public String printInfo(String[] lines,String methodName) {
            return this.path + ", class, " + methodName + ", " + method_LOC(lines) + ", " + method_CLOC(lines) + ", " + method_DC(lines);
        }

        //Visit the methods declarations
        @Override
        public void visit(MethodDeclaration md, Void arg) {
            super.visit(md, arg);
            String[] lines = md.toString().split("\n");
            if(md.getBody().isPresent()) {
                int statementCount = md.getBody().get().getStatements().size();
                answer += printInfo(lines, md.getName().asString()) + ", " + statementCount + ", " + method_DC(lines)/statementCount + "\n";
            }
        }
    }

    //Implementation of the line visitor for classes
    private static class ClassesLineCounter extends VoidVisitorAdapter<Void> {

        private String path;

        public ClassesLineCounter(String path) {
            this.path = path;
        }

        public int class_LOC(String[] lines) {
            return LineCalculator.LOC(lines);
        }

        public int class_CLOC(String[] lines) {
            return LineCalculator.CLOC(lines);
        }

        public float class_DC(String[] lines) {
            return (float)class_CLOC(lines) / class_LOC(lines);
        }

        public int WMC(ClassOrInterfaceDeclaration cd) {
            int wmc = 0;
            List<MethodDeclaration> methods = cd.getMethods();
            List<ConstructorDeclaration> constructors = cd.getConstructors();

            for (MethodDeclaration md: methods) {
                if(md.getBody().isPresent()) {
                    wmc += md.getBody().get().getStatements().size();
                }
            }

            for (ConstructorDeclaration consDec: constructors) {
                wmc += consDec.getBody().getStatements().size();
            }
            return wmc;
        }

        public String printInfo(String[] lines,String className ) {
            return this.path + ", " + className + ", " + class_LOC(lines) + ", " + class_CLOC(lines) + ", " + class_DC(lines);
        }

        //Visit the classes or interfaces declaration
        @Override
        public void visit(ClassOrInterfaceDeclaration cd, Void arg) {
            super.visit(cd, arg);
            String[] lines = cd.toString().split("\n");
            int wmc = WMC(cd);
            answer += printInfo(lines, cd.getNameAsString()) + ", " + wmc + ", " + class_DC(lines)/wmc + "\n";
        }

        //Visit the enumerators declarations
        @Override
        public void visit(EnumDeclaration ed, Void arg) {
            super.visit(ed, arg);
            String[] lines = ed.toString().split("\n");
            answer += printInfo(lines, ed.getNameAsString()) + ", 1" + ", " + class_DC(lines) + "\n";
        }
    }
}