import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.xpath.XPathExpressionException;

import org.xml.sax.SAXException;

public class SOFExpertFinding {
    public static final String indexPathPrefix = "indexes";//Place to put indexes
    public static final String inputPathPrefix = "D:\\projects\\lucene\\resources";//Place to Read Source file that want to be indexed

    public static void main(String[] args) throws XPathExpressionException, SAXException, ParserConfigurationException, XMLStreamException {

        try {
            //Create Folders to hold indexes
            Indexer postIndexer = new Indexer(indexPathPrefix + "\\Posts");
            Indexer questionIndexer = new Indexer(indexPathPrefix + "\\Questions");
            Indexer answerIndexer = new Indexer(indexPathPrefix + "\\Answers");
            Indexer questionAnswerIndexer = new Indexer(indexPathPrefix + "\\QuestionAnswers");
            Indexer questionTagsIndexer = new Indexer(indexPathPrefix + "\\QuestionTags");
            //Source files for indexing
            questionAnswerIndexer.createIndex(inputPathPrefix + "\\Q_A.txt", LuceneConstants.QUESTION_ANSWER_INDEXER);
            questionIndexer.createIndex(inputPathPrefix + "\\Q.txt", LuceneConstants.QUESTION_INDEXER);
            answerIndexer.createIndex(inputPathPrefix + "\\A.txt", LuceneConstants.ANSWER_INDEXER);
            postIndexer.createIndex(inputPathPrefix + "\\Posts.xml", LuceneConstants.POST_INDEXER);
            questionTagsIndexer.createIndex(inputPathPrefix + "\\Q_TAG.txt", LuceneConstants.QUESTION_TAG_INDEXER);

            postIndexer.close();
            questionIndexer.close();
            answerIndexer.close();
            questionAnswerIndexer.close();
            questionTagsIndexer.close();

        } catch (IOException e) {
            e.printStackTrace();

        }
    }
}
