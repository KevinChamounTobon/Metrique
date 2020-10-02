import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitor;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class LineCalculator {

    private CompilationUnit cu;

    //Constructor
    public LineCalculator(String path) throws FileNotFoundException {
        this.cu =  StaticJavaParser.parse(new File(path));
    }

    //Getter
    public CompilationUnit getCu() {
        return this.cu;
    }

    //General version of LOC
    public static int LOC(String[] lines) {
        int count = 0;
        for (String line : lines) {
            if(!line.isEmpty()) {
                ++count;
            }
        }
        System.out.println("LOC: " + count);
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
        System.out.println("CLOC: " + count);
        return count;
    }

    //Check if line is a comment
    private static boolean isComment(String line) {
        return  line.matches("^\\/\\/.*") ||  //Match single line comment
                line.matches("^\\/\\*.*") ||  //Match block comment
                line.matches("^\\*.*")    ||  //Match block comment
                line.matches("^\\/\\*\\*.*"); //Match block comment
    }

    public static void main(String[] args) throws IOException {
        int count = 0;
        //Create calculator object
        LineCalculator calc = new LineCalculator("/home/unknown/Documents/projects/Flappy Ghost/src/Coin.java");
        //Create visitor for classes
        VoidVisitor<?> classLinesVisitor = new ClassesLineCounter();

        //Call visit method for classes
        classLinesVisitor.visit(calc.getCu(), null);
    }

    //Implementation of the line visitor for classes
    private static class ClassesLineCounter extends VoidVisitorAdapter<Void> {

        public int class_LOC(String[] lines) {
            return LineCalculator.LOC(lines);
        }

        public int class_CLOC(String[] lines) {
            return LineCalculator.CLOC(lines);
        }

        public void class_DC(String[] lines) {
            System.out.println("DC: " + (float)class_CLOC(lines) / class_LOC(lines));
        }

        //Visit the classes or interfaces declaration
        @Override
        public void visit(ClassOrInterfaceDeclaration cd, Void arg) {
            super.visit(cd, arg);
            String[] lines = cd.toString().split("\n");
            System.out.println("-----" + cd.getName() + "-----");
            class_DC(lines);
        }

        //Visit the enumerators declarations
        @Override
        public void visit(EnumDeclaration ed, Void arg) {
            super.visit(ed, arg);
            String[] lines = ed.toString().split("\n");
            System.out.println("-----" + ed.getName() + "-----");
            class_DC(lines);
        }
    }
}