package search.analyzers;


import search.models.Webpage;
import datastructures.concrete.dictionaries.ChainedHashDictionary;
import datastructures.interfaces.IDictionary;
import datastructures.concrete.KVPair;
import java.net.URI;
import datastructures.concrete.ChainedHashSet;
import datastructures.interfaces.ISet;
import datastructures.interfaces.IList;

/**
 * This class is responsible for computing how "relevant" any given document is
 * to a given search query.
 *
 * See the spec for more details.
 */
public class TfIdfAnalyzer {
    // This field must contain the IDF score for every single word in all
    // the documents.
    private IDictionary<String, Double> idfScores;
    private IDictionary<URI, Double> norms;

    // This field must contain the TF-IDF vector for each webpage you were given
    // in the constructor.
    //
    // We will use each webpage's page URI as a unique key.
    private IDictionary<URI, IDictionary<String, Double>> documentTfIdfVectors;
    

    // Feel free to add extra fields and helper methods.

    public TfIdfAnalyzer(ISet<Webpage> webpages) {
        // Implementation note: We have commented these method calls out so your
        // search engine doesn't immediately crash when you try running it for the
        // first time.
        //
        // You should uncomment these lines when you're ready to begin working
        // on this class.

        this.idfScores = this.computeIdfScores(webpages);
        this.documentTfIdfVectors = this.computeAllDocumentTfIdfVectors(webpages);
        
    }

    // Note: this method, strictly speaking, doesn't need to exist. However,
    // we've included it so we can add some unit tests to help verify that your
    // constructor correctly initializes your fields.
    public IDictionary<URI, IDictionary<String, Double>> getDocumentTfIdfVectors() {
        return this.documentTfIdfVectors;
    }

    // Note: these private methods are suggestions or hints on how to structure your
    // code. However, since they're private, you're not obligated to implement exactly
    // these methods: feel free to change or modify these methods however you want. The
    // important thing is that your 'computeRelevance' method ultimately returns the
    // correct answer in an efficient manner.

    /**
     * Return a dictionary mapping every single unique word found
     * in every single document to their IDF score.
     */
    private IDictionary<String, Double> computeIdfScores(ISet<Webpage> pages) {
    	IDictionary<String, Double> dict = new ChainedHashDictionary<String, Double>();
        for (Webpage page : pages) {
        	ISet<String> set = new ChainedHashSet<String>();
        	for (String word : page.getWords()) {
        		if (!dict.containsKey(word)) {
        			set.add(word);
        			dict.put(word, 1.0);
        		} else {
        			if (!set.contains(word)) {
        				set.add(word);
        				dict.put(word, dict.get(word) + 1);
        			}
        		}
        	}
        }
        IDictionary<String, Double> scores = new ChainedHashDictionary<String, Double>();
        for (KVPair<String, Double> kv : dict) {
        	double log = Math.log(pages.size() / kv.getValue());
            scores.put(kv.getKey(), log);
        }
        return scores; 
    }

    /**
     * Returns a dictionary mapping every unique word found in the given list
     * to their term frequency (TF) score.
     *
     * The input list represents the words contained within a single document.
     */
    private IDictionary<String, Double> computeTfScores(IList<String> words) {
    	int count = 0;
    	IDictionary<String, Double> dict = new ChainedHashDictionary<String, Double>();
    	for (String word : words) {
    		if (!dict.containsKey(word)) {
    			dict.put(word, 1.0);
    		} else {
    			dict.put(word, 1.0 + dict.get(word));
    		}
    		count++;
    	}
    	IDictionary<String, Double> scores = new ChainedHashDictionary<>();
    	for (KVPair<String, Double> kv : dict) {
    		scores.put(kv.getKey(), kv.getValue() / count);
    	}
    	return scores;
    }

    /**
     * See spec for more details on what this method should do.
     */
    private IDictionary<URI, IDictionary<String, Double>> computeAllDocumentTfIdfVectors(ISet<Webpage> pages) {
    	IDictionary<URI, Double> normDict = new ChainedHashDictionary<>();
        IDictionary<URI, IDictionary<String, Double>> tfIdf = new ChainedHashDictionary<>();
        for (Webpage page : pages) {
            IDictionary<String, Double> vectors = new ChainedHashDictionary<>();
            double norm = 0;
            for (KVPair<String, Double> kv : computeTfScores(page.getWords())) {
                vectors.put(kv.getKey(), idfScores.get(kv.getKey()) * kv.getValue());
                norm += vectors.get(kv.getKey()) * vectors.get(kv.getKey());
            }
            tfIdf.put(page.getUri(), vectors);
            normDict.put(page.getUri(), Math.sqrt(norm));
        }
        norms = normDict;
        return tfIdf;
    }

    /**
     * Returns the cosine similarity between the TF-IDF vector for the given query and the
     * URI's document.
     *
     * Precondition: the given uri must have been one of the uris within the list of
     *               webpages given to the constructor.
     */
    public Double computeRelevance(IList<String> query, URI pageUri) {
        // Note: The pseudocode we gave you is not very efficient. When implementing,
        // this method, you should:
        //
        // 1. Figure out what information can be precomputed in your constructor.
        //    Add a third field containing that information.
        //
        // 2. See if you can combine or merge one or more loops.
        
        double numerator = 0.0;
        double denominator = 0.0;
        IDictionary<String, Double> queryTfIdf = new ChainedHashDictionary<>();
        for (KVPair<String, Double> kv : computeTfScores(query)) {
            if (!idfScores.containsKey(kv.getKey())) {
            	queryTfIdf.put(kv.getKey(), 0.0);
            } else {
            	queryTfIdf.put(kv.getKey(), idfScores.get(kv.getKey()) * kv.getValue());
            }
            denominator += queryTfIdf.get(kv.getKey()) * queryTfIdf.get(kv.getKey());
        }
        for (String q : query) {
            if (documentTfIdfVectors.get(pageUri).containsKey(q)) {
            	numerator += documentTfIdfVectors.get(pageUri).get(q) * queryTfIdf.get(q);
            } 
        }
        denominator = norms.get(pageUri) * Math.sqrt(denominator);
   
       if (denominator != 0) {
           return numerator / denominator;
       } else {
           return 0.0;
       }
    }
}
