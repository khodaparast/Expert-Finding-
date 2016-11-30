import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.util.BytesRef;


public class Helper {
    String indexDirectory = "";

    public Helper(String indexDirectory) {
        this.indexDirectory = indexDirectory;
    }

    public int getDocFrequency(String term) {
        try {
            IndexReader idxReader = DirectoryReader.open(FSDirectory.open(Paths.get(indexDirectory)));
            return idxReader.docFreq(new Term(LuceneConstants.BODY, term));

        } catch (IOException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public Map<String, Map<String, Integer>> getTermFrequencyForEachDoc(List<String> answerTerms, List<Integer> docIds) {
        Map<String, Map<String, Integer>> docTermFrequency = new HashMap<String, Map<String, Integer>>();
        try {
            IndexReader idxReader = DirectoryReader.open(FSDirectory.open(Paths.get(indexDirectory)));
            for (Integer docId : docIds) {
                Terms terms = idxReader.getTermVector(docId, LuceneConstants.BODY);
                TermsEnum termsEnum = terms.iterator();
                BytesRef bytesRef = termsEnum.next();

                while (bytesRef != null) {
                    String termValue = bytesRef.utf8ToString();
                    if (!answerTerms.contains(termValue)) {
                        bytesRef = termsEnum.next();
                        continue;
                    }
                    PostingsEnum pe = MultiFields.getTermDocsEnum(idxReader, LuceneConstants.BODY, bytesRef);
                    int doc;
                    while ((doc = pe.nextDoc()) != PostingsEnum.NO_MORE_DOCS) {
                        if (doc == docId) {
                            Map<String, Integer> tmp;
                            if (!docTermFrequency.containsKey(Integer.toString(docId))) {
                                tmp = new HashMap<String, Integer>();
                            } else {
                                tmp = docTermFrequency.get(Integer.toString(docId));
                            }
                            tmp.put(termValue, pe.freq());
                            docTermFrequency.put(Integer.toString(docId), tmp);
                            break;
                        }
                    }
                    bytesRef = termsEnum.next();
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return docTermFrequency;
    }
}
