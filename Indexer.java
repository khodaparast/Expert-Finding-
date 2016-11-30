import java.io.File;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.xpath.XPathExpressionException;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.xml.sax.SAXException;


public class Indexer {

	private IndexWriter writer;


	public Indexer(String indexDirectoryPath) throws IOException{
		Directory indexDirectory = FSDirectory.open(new File(indexDirectoryPath).toPath());
		System.out.println("directory: " + new File(indexDirectoryPath).getCanonicalPath());
		IndexWriterConfig indexWriterConfig = new IndexWriterConfig(new StandardAnalyzer());
		writer = new IndexWriter(indexDirectory, indexWriterConfig);
	}

	public void close() throws CorruptIndexException, IOException{
		writer.close();
	}

	public void indexFile(Document doc) throws IOException{
		writer.addDocument(doc);
	}

	public int createIndex(String inputFile, String type) throws IOException, XPathExpressionException, SAXException, ParserConfigurationException, XMLStreamException{
		Parser parser = new Parser(this);
		parser.parse(inputFile, type);
		return writer.numDocs();
	}

}
