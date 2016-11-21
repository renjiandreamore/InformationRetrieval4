package PseudoRFSearch;

import java.io.BufferedReader; 
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import Classes.Path;
import Classes.Query;
import Classes.Stemmer;

public class ExtractQuery {

	public HashSet<String> stopwordsSet;
	FileInputStream fis;
	BufferedReader bf;
	ArrayList<Query> queries = new ArrayList<Query>();
	
	public ExtractQuery() throws IOException{
		//you should extract the 4 queries from the Path.TopicDir
		//NT: the query content of each topic should be 1) tokenized, 2) to lowercase, 3) remove stop words, 4) stemming
		//NT: you can simply pick up title only for query, or you can also use title + description + narrative for the query content.
		stopwordsSet = new HashSet<>();
		FileInputStream fis1 = new FileInputStream(Path.StopwordDir);
		BufferedReader bf1 = new BufferedReader(new InputStreamReader(fis1));
		String line = bf1.readLine();
		while((line = bf1.readLine())!=null){
			stopwordsSet.add(line.trim());
		}
		bf1.close();
		fis1.close();
		//put the stopwords into hashset
		
		fis = new FileInputStream(Path.TopicDir);
		bf = new BufferedReader(new InputStreamReader(fis));
		String id = "";
		while((line = bf.readLine()) != null){
		
			if(line.startsWith("<num>")){
				id = line.trim().substring(14, 17);
			}
		
			else if(line.startsWith("<title>")){
				String title = line.substring(8).trim();
				String[] tokens = title.split("\\W+");   //split by all the non-words signs.  "the next generation"
				for(int k = 0; k < tokens.length; k++)
					if(tokens[k].equals("Dysphagia")){
						tokens[k] = "";
					}
				List<String> queryTokens = new ArrayList<String>();
				//System.out.println(tokens[0]);
				for(int i = 0; i < tokens.length; i++){
					String lowercaseToken = tokens[i].toLowerCase();
					String queryToken = "";
					if(!stopwordsSet.contains(lowercaseToken)){
						char[] ch = lowercaseToken.toCharArray();
						Stemmer s = new Stemmer();
						s.add(ch, ch.length);
						s.stem();
						queryToken = s.toString();
						queryTokens.add(queryToken);
					}
				}
				Query q = new Query();
				q.SetTopicId(id);
				q.SetQueryContent(queryTokens);
				queries.add(q);
			}
		}
		bf.close();
		fis.close();
		
	}
	
	public Query next() throws IOException
	{
		if(queries.isEmpty()){
			return null;
		}else{
			return queries.remove(0);
		}
	}
	
	public boolean hasNext ()throws IOException
	{
		return !queries.isEmpty();
	}
	

	
	public static void main(String[] args) throws Exception{
		ExtractQuery e = new ExtractQuery();
		while(e.hasNext()){
			System.out.println(e.next().GetQueryContent());
		}
		
		//eq.next();
		
	}
	
	
}
