import org.apache.lucene.queryparser.classic.ParseException;
import java.io.*;
import java.util.*;

public class Evaluation {
    public  static final String outputPathForResult="result\\N10000_Lambda0_9_2.txt";
    public static final String indexDirectoryPath = "indexes";
    public static final String queryGroundTruthDir = "GroundTruth\\DataSetFor";
    static String[] elments;

    public static ArrayList<String> getGroundArray(String query) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(queryGroundTruthDir + query + ".txt"));
        String oneLineOfQueryGTFile = br.readLine();
        ArrayList<String> list = new ArrayList<>();
        int count = 0;
        while ((oneLineOfQueryGTFile = br.readLine()) != null) {
            count++;
            elments = oneLineOfQueryGTFile.split(",");
            String userId = elments[0];
            list.add(userId);
        }
        br.close();
        return list;
    }

    public static void getMeasures(String query) throws IOException, ParseException {
        FileOutputStream fileOutputStream = new FileOutputStream(outputPathForResult, true);
        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream);
//        outputStreamWriter.write("query,P@1,P@5,P@10,AP"+"\n");
        String q = query.replace("-", " ");
        q = q.replace("+", "");
        q = q.replace("#", "");

        //it will return groundTruth related to query
        ArrayList<String> groundList = getGroundArray(query);
        float P_at_1 = 0.0f;
        float P_at_5 = 0.f;
        float P_at_10 = 0.0f;
        float AP = 0.0f;//Average of Precisions
        float sumOfRels = 0.0f;
        float Rel = 0.0f;
        float perRel;

        Tester ts = new Tester(indexDirectoryPath);
        Set<String> result = ts.expertFinding(q);
        String[] array = new String[result.size()];
        Iterator<String> it = result.iterator();
        for (int i = 0; it.hasNext(); i++) {
            array[i] = it.next();
        }
        for (int j = 0; j < array.length; j++) {
            if (groundList.contains(array[j])) {
                Rel++;
                perRel = Rel / (j + 1);
                sumOfRels = sumOfRels + perRel;
            }
            switch (j) {
                case 0: {
                    P_at_1 = sumOfRels;

                    break;
                }
                case 4: {
                    P_at_5 = sumOfRels;
                    break;
                }
                case 9: {
                    P_at_10 = sumOfRels;
                    break;
                }
            }
        }
        if (Rel != 0) {
            AP = sumOfRels / Rel;
        }
        outputStreamWriter.append(query + "," + P_at_1 + "," + P_at_5 + "," + P_at_10 + "," + AP + "\n");
        outputStreamWriter.flush();
    }
    public static void main(String[] args) throws IOException, ParseException {
        String[] query1 = new String[]{"algorithm", "android", "annotations", "ant", "apache",
                "applet", "arraylist", "arrays", "awt","c#", "c++", "class", "collections", "concurrency",
                "database", "date", "design-patterns", "eclipse", "encryption", "exception", "file", "file-io", "generics",
                "google-app-engine","gwt", "hadoop", "hashmap", "hibernate", "html", "http", "image", "inheritance", "intellij-idea",
                "io", "jar", "java","java-ee", "javafx", "javascript", "jaxb", "jboss", "jdbc", "jersey", "jframe", "jni",
                "jpa", "jpanel", "jquery", "jsf", "json", "jsp", "jtable", "junit", "jvm", "libgdx",
                "linux", "list", "log4j", "logging", "loops", "maven", "methods", "multithreading", "mysql",
                "netbeans", "nullpointerexception", "object", "oop", "oracle", "osx", "parsing",
                "performance", "php", "reflection", "regex", "rest", "scala", "security", "selenium",
                "serialization", "servlets", "soap", "sockets", "sorting", "spring","spring-mvc"  , "spring-security", "sql",
                "sqlite", "string", "struts", "swing", "swt", "tomcat", "unit-testing", "user-interface", "web-services", "windows", "xml"
              };
        for (int i = 0; i < query1.length; i++) {
            getMeasures(query1[i]);
        }
    }
}