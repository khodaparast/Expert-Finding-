import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.xpath.XPathExpressionException;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.util.BytesRef;
import org.jsoup.Jsoup;
import org.xml.sax.SAXException;


//PostTypeId: 1=question 2=answer

public class Parser {

    Indexer indexer;
    public static Set<String> questionIds = new HashSet<String>();
    public static Set<String> answerIds = new HashSet<String>();
    public static int questionCounter = 0;
    public static int answerCounter = 0;
    public static Set<String> questionIds2 = new HashSet<String>();
    public static Set<String> answerIds2 = new HashSet<String>();
    public static List<Document> documents = new ArrayList<Document>();

    public Parser(Indexer indexer) {
        this.indexer = indexer;
    }

    public void parse(String inputFile, String indexType) throws IOException, XPathExpressionException, SAXException, ParserConfigurationException, XMLStreamException {
        System.out.println("parser in Parser....................");
        switch (indexType) {
            case LuceneConstants.POST_INDEXER:
                System.out.println("POST_INDEXER in parser................. ");
                parsePosts(inputFile);
                break;
            case LuceneConstants.QUESTION_INDEXER:
                System.out.println("QUESTION_INDEXER in parser................. ");

                parseQuestions(inputFile);
                break;
            case LuceneConstants.ANSWER_INDEXER:
                System.out.println("ANSWER_INDEXER in parser................. ");

                parseAnswers(inputFile);
                break;
            case LuceneConstants.QUESTION_ANSWER_INDEXER:
                System.out.println("QUESTION_ANSWER_INDEXER in parser................. ");

                parseQuestionAnswer(inputFile);
                break;
            case LuceneConstants.QUESTION_TAG_INDEXER:
                System.out.println("QUESTION_TAG_INDEXER in parser................. ");

                parseQuestionTags(inputFile);
                break;
        }
    }

    public void parsePosts(String inputFile) throws XMLStreamException, IOException {
        System.out.println("parserPosts.............................");
        System.out.println("questionId2: " + questionIds2);
        System.out.println("answerId2: " + answerIds2);
        XMLInputFactory inputFactory = XMLInputFactory.newInstance();
        InputStream in = new FileInputStream(inputFile);
        XMLStreamReader parser = inputFactory.createXMLStreamReader(in, "UTF-8");
        parser.nextTag();

        int counter = 0;
        while (parser.hasNext()) {
            int event = parser.next();

            if (event == XMLStreamConstants.START_ELEMENT) {
                if (parser.getLocalName().equals("row")) {
                    Document doc = new Document();
                    String id = parser.getAttributeValue(null, "Id");
                    System.out.println("id: " + id);
                    String body = jsoupBody(parser.getAttributeValue(null, "Body"));
                    String postTypeId = parser.getAttributeValue(null, "PostTypeId");
                    String author = parser.getAttributeValue(null, "OwnerUserId");
                    if (!answerIds.contains(id)) continue;

                    if (id != null)
                        doc.add(new StringField(LuceneConstants.ID, id, Field.Store.YES));

                    if (body != null) {
                        FieldType ft = new FieldType(TextField.TYPE_NOT_STORED);
                        ft.setStored(true);
                        ft.setTokenized(true);
                        ft.setStoreTermVectors(true);
                        ft.setStoreTermVectorPositions(true);
                        ft.setIndexOptions(IndexOptions.DOCS_AND_FREQS);
                        doc.add(new Field(LuceneConstants.BODY, body, ft));
                    }
                    if (postTypeId != null)
                        doc.add(new StringField(LuceneConstants.TYPE_ID, postTypeId, Field.Store.YES));
                    else
                        doc.add(new StringField(LuceneConstants.TYPE_ID, "0", Field.Store.YES));

                    if (author != null)
                        doc.add(new StringField(LuceneConstants.AUTHOR, author, Field.Store.YES));


                    counter++;
                    System.out.println("counter : " + counter);
                    indexer.indexFile(doc);

                }
            }

        }

    }

    public void parseQuestions(String inputFile) {
        try (Stream<String> stream = Files.lines(new File(inputFile).toPath(), Charset.forName("UTF8"))) {
            stream.skip(1).forEachOrdered(line -> addQuestion(line));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void addQuestion(String line) {
        String[] tokens = line.split(",");
        if (tokens.length > 3) {
            if (!questionIds.contains(tokens[0])) return;
            questionCounter++;
            Document doc = new Document();
            String vote = tokens[3];
            if (vote != null) {
                doc.add(new StringField(LuceneConstants.VOTE, vote, Field.Store.YES));
                doc.add(new SortedDocValuesField(LuceneConstants.VOTE, new BytesRef(vote)));
            } else {
                doc.add(new StringField(LuceneConstants.VOTE, "0", Field.Store.YES));
                doc.add(new SortedDocValuesField(LuceneConstants.VOTE, new BytesRef("0")));
            }
            doc.add(new StringField(LuceneConstants.QUESTION_ID, tokens[0], Field.Store.YES));
            doc.add(new StringField(LuceneConstants.DATE, parseDate(tokens[1]), Field.Store.YES));
            try {
                indexer.indexFile(doc);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    public void parseAnswers(String inputFile) {
        try (Stream<String> stream = Files.lines(new File(inputFile).toPath(), Charset.forName("UTF16"))) {
            stream.skip(2).forEachOrdered(line -> addAnswer(line));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addAnswer(String line) {
        String[] tokens = line.split(",");
        if (tokens.length > 3) {
            if (!answerIds.contains(tokens[0])) return;
            /*if(answerCounter > 100) return;*/
            answerCounter++;
            Document doc = new Document();
            //answerIds2.add(tokens[0]);
            if (tokens[0] != null)
                doc.add(new StringField(LuceneConstants.ANSWER_ID, tokens[0], Field.Store.YES));
            String vote = tokens[3];
            if (vote != null) {
                doc.add(new StringField(LuceneConstants.VOTE, vote, Field.Store.YES));
                doc.add(new SortedDocValuesField(LuceneConstants.VOTE, new BytesRef(vote)));
            } else {
                doc.add(new StringField(LuceneConstants.VOTE, "0", Field.Store.YES));
                doc.add(new SortedDocValuesField(LuceneConstants.VOTE, new BytesRef("0")));
            }
            try {
                indexer.indexFile(doc);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void parseQuestionAnswer(String inputFile) {
        try (Stream<String> stream = Files.lines(new File(inputFile).toPath(), Charset.forName("UTF8"))) {
            stream.skip(1).forEachOrdered(line -> addQuestionAnswer(line));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addQuestionAnswer(String line) {
        String[] tokens = line.split(",");
        if (tokens.length > 2) {
            Document doc = new Document();
            doc.add(new StringField(LuceneConstants.QUESTION_ID, tokens[0], Field.Store.YES));
            questionIds.add(tokens[0]);
            doc.add(new StringField(LuceneConstants.ANSWER_ID, tokens[1], Field.Store.YES));
            answerIds.add(tokens[1]);
            doc.add(new StringField(LuceneConstants.ACCEPTED, tokens[2], Field.Store.YES));
            try {
                indexer.indexFile(doc);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void parseQuestionTags(String inputFile) throws IOException {
        try (Stream<String> stream = Files.lines(new File(inputFile).toPath())) {
            stream.skip(1).forEachOrdered(line -> addQuestionTag(line));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addQuestionTag(String line) {
        String[] tokens = line.split(",");
        if (tokens.length > 1) {
            if (!questionIds2.contains(tokens[0])) return;
            Document doc = new Document();
            doc.add(new StringField(LuceneConstants.QUESTION_ID, tokens[0], Field.Store.YES));
            doc.add(new StringField(LuceneConstants.QUESTION_TAG, tokens[1], Field.Store.YES));
            try {
                indexer.indexFile(doc);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static String parseDate(String creationDate) {
        //2008-07-31 21:42:52.667
        creationDate = creationDate.replaceAll("\\D", "");
        Pattern pattern = Pattern.compile("(\\d{4})(\\d{2})(\\d{2})(.*)");
        Matcher matcher = pattern.matcher(creationDate);

        if (matcher.find() && matcher.groupCount() >= 3) {
            return matcher.group(1) + "-" + matcher.group(2) + "-" + matcher.group(3);
        }
        return null;
    }

    public static List<String> parseTags(String tagsString) {
        String[] tmp = tagsString.replace("<", "").replace(">", ",").split(",");
        List<String> tags = Arrays.asList(tmp);
        return tags;
    }

    public String jsoupBody(String body) {
        return Jsoup.parse(body).text();
    }
}
