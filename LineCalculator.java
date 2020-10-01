import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;

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
    }
}