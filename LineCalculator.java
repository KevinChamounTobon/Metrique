import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.*;
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

/**
 *Authors: Kevin Chamoun-Tobon & Christian El-Hamaoui
 *
 * Static class for metric calculation
 */
public class LineCalculator {

    public static String classCSV = "";  //Global variable for classes information
    public static String methodCSV = ""; //Global variable for methods information

    public static void main(String[] args) throws IOException {

        Path projectPath = Paths.get(args[0]);
        ProjectRoot projectRoot = new ParserCollectionStrategy().collect(projectPath);
        List<SourceRoot>  srcRoots = projectRoot.getSourceRoots();

        LineCalculator.parseJavaProject(srcRoots);

        write("classes.csv", classCSV);
        write("methodes.csv", methodCSV);

        System.out.println("CSV files built");
    }

    /**
     * Checks ammount of non empty lines of code
     * @param lines array of strings containing lines of java code
     * @return count of non empty lines of code
     */
    public static int LOC(String[] lines) {
        int count = 0;
        for (String line : lines) {
            if(!line.isEmpty()) {
                ++count;
            }
        }
        return count;
    }

    /**
     * Checks amount of comments
     * @param lines array of strings containing lines of java code
     * @return count of comments
     */
    public static int CLOC(String[] lines) {
        int count = 0;
        for (String line : lines) {
            line = line.replaceAll("\\s", "");
            line = line.replaceAll("\\t", "");
            if(isComment(line)) {
                ++count;
            }
        }
        return count;
    }

    /**
     * Checks if line is a comment
     * @param line String representing java code
     * @return if a line is a comment
     */
    private static boolean isComment(String line) {
        return  line.matches("^//.*") ||  //Match single line comment
                line.matches("^/\\*.*") ||  //Match block comment
                line.matches("^\\*.*")    ||  //Match block comment
                line.matches("^/\\*\\*.*"); //Match block comment
    }

    /**
     * All purpose writer
     * @param fileName The name of the file to create
     * @param output Then content of the file to create
     */
    private static void write(String fileName, String output) {
        try {
            FileWriter fw = new FileWriter(fileName);
            BufferedWriter bw = new BufferedWriter(fw);

            bw.append(output);
            bw.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Parse the java file
     * @param srcRoots List of all src directory in project
     * @throws IOException If file not found
     */
    public static void parseJavaProject(List<SourceRoot> srcRoots) throws IOException {

        classCSV +=  "chemin, class, classe_LOC, classe_CLOC, classe_DC, WMC, classe_BC\n";
        methodCSV += "chemin, class, methode, methode_LOC, methode_CLOC, methode_DC, CC, methode_BC\n";

        for (SourceRoot sourceRoot: srcRoots) {
            sourceRoot.tryToParse();
            List<CompilationUnit> compilations = sourceRoot.getCompilationUnits();
            for (CompilationUnit n: compilations) {
                //Create visitor for methods
                VoidVisitor<?> methodLinesVisitor = new MethodsLineCounter(n.getStorage().get().getPath().toString());
                //Create visitor for classes
                VoidVisitor<?> classLinesVisitor = new ClassesLineCounter(n.getStorage().get().getPath().toString());
                //Call visit method for classes
                classLinesVisitor.visit(n, null);
                //Call visit method for methods
                methodLinesVisitor.visit(n, null);
            }
        }
    }

    /**
     * Nested class made to visit methods declaration
     */
    private static class MethodsLineCounter extends VoidVisitorAdapter<Void> {

        private String path;
        private String className;

        /**
         * Constructor
         * @param path Path of the project
         */
        public MethodsLineCounter(String path) {
            String[] splittedPath = path.split("/");
            this.path = path;
            this.className = splittedPath[splittedPath.length-1].replace(".java", "");
        }

        /**
         * Calculates the DC for a methode
         * @param lines array of strings containing lines of java code
         * @return returns the DC for a methode
         */
        public float method_DC(String[] lines) {
            return (float)LineCalculator.CLOC(lines) / LineCalculator.LOC(lines);
        }

        /**
         * Gets the general info for the csv file of methodes
         * @param lines array of strings containing lines of java code
         * @param methodName The name of the methode
         * @return Returns the info for the csv
         */
        public String printInfo(String[] lines,String methodName) {
            return this.path + "," +  this.className +", " + methodName + ", " + LineCalculator.LOC(lines)  + ", "
                    + LineCalculator.CLOC(lines) + ", "
                    + method_DC(lines);
        }

        /**
         * Override the API visitor to visit methode declaration
         * @param md A methode declaration
         * @param arg Null argument
         */
        @Override
        public void visit(MethodDeclaration md, Void arg) {
            super.visit(md, arg);
            String[] lines = md.toString().split("\n");
            String methodeName = md.getName().asString();
            StringBuilder methodeParam = new StringBuilder();

            for (Parameter type : md.getParameters()) {
                methodeParam.append("_").append(type.getTypeAsString());
            }

            if(md.getBody().isPresent()) {
                int statementCount = md.getBody().get().getStatements().size();
                float methodeBC = method_DC(lines)/statementCount;
                methodCSV += printInfo(lines, methodeName + methodeParam) +  ", " + statementCount +
                        ", " + methodeBC + "\n";
            }
        }

        /**
         * Override the API visitor to visit constructor declaration
         * @param cd A constructor declaration
         * @param arg Null argument
         */
        @Override
        public void visit(ConstructorDeclaration cd, Void arg) {
            super.visit(cd, arg);
            String[] lines = cd.toString().split("\n");
            int statementCount = cd.getBody().getStatements().size();
            float methodeBC = method_DC(lines)/statementCount;
            methodCSV += printInfo(lines, cd.getName().asString()) + ", " + statementCount +
                    ", " + methodeBC + "\n";
        }
    }

    /**
     * Nested class made to visit classes declaration
     */
    private static class ClassesLineCounter extends VoidVisitorAdapter<Void> {

        private String path;

        /**
         * constructor
         * @param path Path of the project
         */
        public ClassesLineCounter(String path) {
            this.path = path;
        }

        /**
         * Calculated the DC for a class declaration
         * @param lines array of strings containing lines of java code
         * @return Return the DC of a class declaration
         */
        public float class_DC(String[] lines) {
            return (float)LineCalculator.CLOC(lines) / LineCalculator.LOC(lines);
        }

        /**
         * Calculate the WMC of a class declaration
         * @param cd A class declaration
         * @return Returns the WMC of a class declaration
         */
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

        /**
         * Gets the general info for the csv file of classes
         * @param lines array of strings containing lines of java code
         * @param className The name of the class
         * @return Returns the info for the csv
         */
        public String printInfo(String[] lines, String className) {
            return this.path + ", " + className + ", " + LineCalculator.LOC(lines) +
                    ", " + LineCalculator.CLOC(lines) +
                    ", " + class_DC(lines);
        }

        /**
         * Override the API visitor to visit class or interface declaration
         * @param cd a class or interface declaration
         * @param arg Null argument
         */
        @Override
        public void visit(ClassOrInterfaceDeclaration cd, Void arg) {
            super.visit(cd, arg);
            String[] lines = cd.toString().split("\n");
            int wmc = WMC(cd);
            float classeBC = class_DC(lines)/wmc;
            classCSV += printInfo(lines, cd.getNameAsString()) + ", " + wmc + ", " + classeBC + "\n";
        }

        /**
         * Override the API visitor to visit enumeration declaration
         * @param ed A enumerator declaration
         * @param arg Null argument
         */
        @Override
        public void visit(EnumDeclaration ed, Void arg) {
            super.visit(ed, arg);
            String[] lines = ed.toString().split("\n");
            classCSV += printInfo(lines, ed.getNameAsString()) + ", 1" + ", " + class_DC(lines) + "\n";
        }
    }
}
