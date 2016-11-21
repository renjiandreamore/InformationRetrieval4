package PseudoRFSearch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import Classes.Document;
import Classes.Query;
import IndexingLucene.MyIndexReader;


public class PseudoRFRetrievalModel {

	MyIndexReader ixreader;
	public double miu = 2000;
	public long collectionLength = 142065539;
	List<Document> topKDoc = new ArrayList<>();
	HashMap<String, HashMap<Integer, Integer>> postings = new HashMap<>();
	
	
	
	public PseudoRFRetrievalModel(MyIndexReader ixreader)
	{
		this.ixreader=ixreader;
	}
	
	/**
	 * Search for the topic with pseudo relevance feedback. 
	 * The returned results (retrieved documents) should be ranked by the score (from the most relevant to the least).
	 * 
	 * @param aQuery The query to be searched for.
	 * @param TopN The maximum number of returned document
	 * @param TopK The count of feedback documents
	 * @param alpha parameter of relevance feedback model
	 * @return TopN most relevant document, in List structure
	 */
	public List<Document> RetrieveQuery( Query aQuery, int TopN, int TopK, double alpha) throws Exception {	
		// this method will return the retrieval result of the given Query, and this result is enhanced with pseudo relevance feedback
		// (1) you should first use the original retrieval model to get TopK documents, which will be regarded as feedback documents
		// (2) implement GetTokenRFScore to get each query token's P(token|feedback model) in feedback documents
		// (3) implement the relevance feedback model for each token: combine the each query token's original retrieval score P(token|document) with its score in feedback documents P(token|feedback model)
		// (4) for each document, use the query likelihood language model to get the whole query's new score, P(Q|document)=P(token_1|document')*P(token_2|document')*...*P(token_n|document')
		
		//1.get topK doc as feedback document.
		List<String> oneQuery = aQuery.GetQueryContent();
		//HashMap<String, HashMap<Integer, Integer>> postings = new HashMap<>();
		HashSet<Integer> docSet = new HashSet<>();
		
		for(int i = 0; i < oneQuery.size(); i++) {
			String token = oneQuery.get(i);
			HashMap<Integer, Integer> map = new HashMap<>();
			int[][] posting = ixreader.getPostingList(token);
			if(posting == null || posting.length == 0){
				oneQuery.remove(token);
				continue;
			}
			
			for(int j = 0; j < posting.length; j++){
				int id = posting[j][0];
				int freq = posting[j][1];
				map.put(id, freq);
				docSet.add(id);
				postings.put(token, map);
			}
		}
		
		//< hong-<0001-3, 0012-2, 0001-6,...>, econ-<0004-2, 0023-19, 0002-5, ...>, kong-<...>, singapo-<...> >
		//docSet<0001, 0002, 0003, 0005, 0023, 0094...>
		
		List<Document> docResult = new ArrayList<>();
		Iterator<Integer> it = docSet.iterator();
		
		while(it.hasNext()){ //started from doc001
			int id = it.next();
			double score = 1;
			
			for(String token : oneQuery){
				long cf = ixreader.CollectionFreq(token);
				HashMap<Integer, Integer> map = postings.get(token);
				int docFreq = 0;
				if(map.containsKey(id)){
					docFreq = map.get(id);
				}
				
				double PW = (docFreq + miu * cf / collectionLength) / (miu + ixreader.docLength(id));
				score *= PW;
			}
			
			if(score!=0){
				String docNo = ixreader.getDocno(id);
				String docId = String.valueOf(id);
				Document doc = new Document(docId,docNo,score);
				docResult.add(doc);
			}
		}
		
		Collections.sort(docResult, new DocComparator());
		
//		for(int i = 0; i < TopK; i++){
//			topKDoc.add(docResult.get(i));
//		}
		
		topKDoc = docResult.subList(0, TopK);
		//System.out.println(topKDoc.subList(0, 5));
		//docResult.clear();
		
		//return topKDoc;
		
		
		//get P(token|feedback documents)
		HashMap<String,Double> TokenRFScore=GetTokenRFScore(aQuery,TopK);
		List<Document> results = new ArrayList<Document>();
		Iterator<Integer> it1 = docSet.iterator();
		
		while(it1.hasNext()){
			int id = it1.next();
			double score = 1;
			
			for(String token : oneQuery){  //hong
				long cf = ixreader.CollectionFreq(token);
				HashMap<Integer, Integer> map = postings.get(token);
				int docFreq = 0;
				if(map.containsKey(id)){
					docFreq = map.get(id);
				}
				
				double Part1 = alpha * (docFreq + miu * cf / ixreader.CollectionFreq(token)) / (miu + ixreader.docLength(id));
				double Part2 = (1 - alpha) * TokenRFScore.get(token);
				
				score *= Part1 + Part2;
			}
			
			if(score!=0){
				String docNo = ixreader.getDocno(id);
				String docId = String.valueOf(id);
				Document doc = new Document(docId, docNo, score);
				results.add(doc);
			}
		}
		
		Collections.sort(results, new DocComparator());
		//System.out.println(results);
		
		
		// sort all retrieved documents from most relevant to least, and return TopN
				
		return results.subList(0, TopN);
//		List<Document> f = new ArrayList<>();
//		for(int i = 0; i < TopN; i++){
//			f.add(results.get(i));
//		}
//		return f;
	}
	
	public HashMap<String,Double> GetTokenRFScore(Query aQuery,  int TopK) throws Exception
	{
		// for each token in the query, you should calculate token's score in feedback documents: P(token|feedback documents)
		// use Dirichlet smoothing
		// save <token, score> in HashMap TokenRFScore, and return it
		HashMap<String,Double> TokenRFScore=new HashMap<String,Double>();
		
		List<String> oneQuery = aQuery.GetQueryContent();
		
		for(String token : oneQuery){ //"hong"
			double score = 0;
			long cf = ixreader.CollectionFreq(token);
			HashMap<Integer, Integer> map = postings.get(token);
			int docFreq = 0;
			for(Document d : topKDoc){  //doc001
				int id = Integer.parseInt(d.docid());
				
				if(map.get(id) != null && map.get(id) != 0){
					docFreq = map.get(id);
				}
				score += (docFreq + miu * cf / collectionLength) / (miu + ixreader.docLength(id));
				// get a token's probability in each doc in feedback document and add them together to 
				//get the token's p(token|feedback document)
			}
			
			TokenRFScore.put(token, score);
		}
		
		return TokenRFScore;
	}	
	
}

class DocComparator implements Comparator<Document>{
	public int compare(Document a, Document b){
		if(a.score() != b.score()){
			return a.score() < b.score() ? 1 : -1;
		}else{
			return 1;
		}
	}
}

