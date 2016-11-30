import java.io.File;
import java.io.IOException;
import java.util.*;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;

public class Tester {

    IndexSearcher postSearcher;
    QueryParser bodyQueryParser;
    public static String indexPrefix = "indexes";
    public static final int beta = 100;


    public Tester(String indexDirectoryPath) throws IOException {
        indexPrefix = indexDirectoryPath;
        postSearcher = new IndexSearcher(DirectoryReader.open(FSDirectory.open(new File(indexPrefix + "\\Posts").toPath())));
    }


    public Set<String> expertFinding(String answerQuery) throws ParseException, IOException {

        Map<String, List<Integer>> authorDocs = new HashMap<String, List<Integer>>(); // To store document ids of each author (luceneId, not PostId)
        Map<String, Integer> termDocFreq = new HashMap<String, Integer>(); // To store document frequency for each term of query. (In how many document it occurs)
        Map<String, Integer> queryTermFrequency = new HashMap<String, Integer>(); // To store number of occurrence of each term of query in query --> "android java android"
        Map<String, Map<String, Integer>> docTermFrequency = new HashMap<String, Map<String, Integer>>(); //To store number of occurrence of each term in each retrieved document
        Map<String, Double> authorScores = new HashMap<String, Double>(); //Most important variable. Store final score of each author
        Map<String, Double> sortedDescending = new LinkedHashMap<String, Double>(); //Sort authorScores by its' values. Sort authors by their scores descending
        List<String> answerIds = new ArrayList<String>(); // To store id of each post from Posts.xml file
        List<Integer> luceneIds = new ArrayList<Integer>();// To store id of each document in index. The id which lucene set to documents
        int answerCounts = getAnswerCounts(postSearcher);//Total number of answers

        Helper balog = new Helper(indexPrefix + "\\Posts"); // New class to compute tf, idf ...

        String[] answerQueryTerms = answerQuery.split(" "); // Get terms of query

        //Fill termDocFreq and queryTermFrequency
        for (String answerTerm : answerQueryTerms) {
            System.out.println("answerTerm: " + answerTerm);
            if (!queryTermFrequency.containsKey(answerTerm))
                termDocFreq.put(answerTerm, balog.getDocFrequency(answerTerm));

            Integer f = queryTermFrequency.get(answerTerm);
            if (f == null)
                f = 0;
            queryTermFrequency.put(answerTerm, f + 1);
        }
        //Get answers based on query (all terms of query) and fill answerIds and luceneIds
        TopDocs answers = searchAnswersBody(postSearcher, answerQuery);
        for (ScoreDoc doc : answers.scoreDocs) {
            String authorId = postSearcher.doc(doc.doc).get(LuceneConstants.AUTHOR);
//			System.out.println("authorId : "+authorId);
            if (authorId != null) {
                if (authorDocs.containsKey(authorId)) {
                    authorDocs.get(authorId).add(doc.doc);
                } else {
                    List<Integer> tmp = new ArrayList<Integer>();
                    tmp.add(doc.doc);
                    authorDocs.put(authorId, tmp);
                }
                answerIds.add(postSearcher.doc(doc.doc).get(LuceneConstants.ID));
                luceneIds.add(doc.doc);//all docs for this query
            }
        }
        //Fill docTermFrequency
        docTermFrequency = balog.getTermFrequencyForEachDoc(Arrays.asList(answerQueryTerms), luceneIds);

        //Main Fromula for each author = SigmaDoc(ProductTerm(((1-L).p(t|d)+L.p(t))^n(t,q)) . p(d|ca))
        for (Map.Entry<String, List<Integer>> entry : authorDocs.entrySet()) {//for each author
            String authorId = entry.getKey();
            List<Integer> docIds = entry.getValue();
            double authorScore = 0;
            for (Integer docId : docIds) {//for each author's document--->sigma
                double documentScore = 1;
                for (String answerTerm : answerQueryTerms) {//for each query term---->product
                    int docFrequency = termDocFreq.get(answerTerm);
                    float probOfTermInCollection = (float) docFrequency / (float) answerCounts;//--->p(t)

                    int termFrequency;
                    //p mle soorat
                    if (docTermFrequency.get(Integer.toString(docId)).containsKey(answerTerm)) {
                        termFrequency = docTermFrequency.get(Integer.toString(docId)).get(answerTerm);
                    } else {
                        termFrequency = 0;
                    }
                    int docLength = postSearcher.doc(docId).get(LuceneConstants.BODY).trim().split("\\s+").length;
                    float probOfTermInDoc = (float) termFrequency / (float) docLength;//p(t|d)

//					float lambda = (float)beta/(float)(docLength);//beta
                    float lambda = 0.9f;
                    //p(w|d)=1-lambda*p(mle(t|d))+lambda*p(collection(t))
                    documentScore = documentScore * Math.pow(((1 - lambda) * (probOfTermInDoc) + lambda * (probOfTermInCollection)), queryTermFrequency.get(answerTerm));
                }
                authorScore += documentScore;
            }
            authorScores.put(authorId, authorScore);
        }
        //Sort author
        List<Map.Entry<String, Double>> tmpList =
                new LinkedList<Map.Entry<String, Double>>(authorScores.entrySet());

        Collections.sort(tmpList, new Comparator<Map.Entry<String, Double>>() {

            public int compare(Map.Entry<String, Double> o1, Map.Entry<String, Double> o2) {
                return (o2.getValue()).compareTo(o1.getValue());
            }
        });
        for (Map.Entry<String, Double> entry : tmpList) {
            sortedDescending.put(entry.getKey(), entry.getValue());
        }
        System.out.println("result: " + sortedDescending.keySet());
        return sortedDescending.keySet();
    }

    public int getAnswerCounts(IndexSearcher searcher) throws IOException {
        //Get total number of answers in index, use in calculation of p(t) = number of documents contain query divide by number of all documents
        Query answerQuery = new TermQuery(new Term(LuceneConstants.TYPE_ID, "2"));
        System.out.println("getAnswerCounts..........");
        TopDocs answers = searcher.search(answerQuery, Integer.MAX_VALUE);
        return answers.totalHits;
    }

    public TopDocs searchAnswersBody(IndexSearcher searcher, String query) throws ParseException, IOException {
        //Get answers with query term in body.
        //If you want to all terms of query occur in retrieved documents uncomment line //bodyQueryParser.setDefaultOperation...

        bodyQueryParser = new QueryParser(LuceneConstants.BODY, new StandardAnalyzer());
        bodyQueryParser.setDefaultOperator(QueryParser.Operator.AND);

        BooleanQuery.Builder finalQuery = new BooleanQuery.Builder();

        Query answerQuery = new TermQuery(new Term(LuceneConstants.TYPE_ID, "2")); //To extract just answers
        Query bodyQuery = bodyQueryParser.parse(query);

        if (bodyQuery != null)
            finalQuery.add(new BooleanClause(bodyQuery, BooleanClause.Occur.MUST));
        if (answerQuery != null)
            finalQuery.add(new BooleanClause(answerQuery, BooleanClause.Occur.MUST));

        TopDocs answerHits = searcher.search(finalQuery.build(), 10000);//Number of retrieved docs
        return answerHits;
    }


}
