/*
 * Created on Mar 14, 2005
 */
package org.alfresco.repo.search.impl.lucene.query;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.Weight;

/**
 * An extension to the Lucene query set.
 * 
 * This query supports structured queries against paths.
 * 
 * The field must have been tokenised using the path tokeniser.
 * 
 * This class manages linking together an ordered chain of absolute and relative
 * positional queries.
 * 
 * @author Andy Hind
 */
public class PathQuery extends Query
{
    /**
     * 
     */
    private static final long serialVersionUID = 3832904355660707892L;

    private String pathField = "PATH";

    private String qNameField = "QNAME";

    private int unitSize = 2;

    private List<StructuredFieldPosition> pathStructuredFieldPositions = new ArrayList<StructuredFieldPosition>();

    private List<StructuredFieldPosition> qNameStructuredFieldPositions = new ArrayList<StructuredFieldPosition>();

    

    /**
     * The base query
     * 
     * @param query
     */

    public PathQuery()
    {
        super();
    }

    public void setQuery(List<StructuredFieldPosition> path, List<StructuredFieldPosition> qname)
    {
        qNameStructuredFieldPositions.clear();
        pathStructuredFieldPositions.clear();
        if (qname.size() != unitSize)
        {
            throw new UnsupportedOperationException();
        }
        if (path.size() % unitSize != 0)
        {
            throw new UnsupportedOperationException();
        }
        qNameStructuredFieldPositions.addAll(qname);
        pathStructuredFieldPositions.addAll(path);
    }
    
    public void appendQuery(List<StructuredFieldPosition> sfps)
    {
        if (sfps.size() != unitSize)
        {
            throw new UnsupportedOperationException();
        }

        StructuredFieldPosition last = null;
        StructuredFieldPosition next = sfps.get(0);

        if (qNameStructuredFieldPositions.size() > 0)
        {
            last = qNameStructuredFieldPositions.get(qNameStructuredFieldPositions.size() - 1);
        }

        if ((last != null) && next.linkParent() && !last.allowslinkingByParent())
        {
            return;
        }

        if ((last != null) && next.linkSelf() && !last.allowsLinkingBySelf())
        {
            return;
        }

        if (qNameStructuredFieldPositions.size() == unitSize)
        {
            pathStructuredFieldPositions.addAll(qNameStructuredFieldPositions);
        }
        qNameStructuredFieldPositions.clear();
        qNameStructuredFieldPositions.addAll(sfps);
    }

    public String getPathField()
    {
        return pathField;
    }

    public void setPathField(String pathField)
    {
        this.pathField = pathField;
    }

    public String getQnameField()
    {
        return qNameField;
    }

    public void setQnameField(String qnameField)
    {
        this.qNameField = qnameField;
    }

    public Term getPathRootTerm()
    {
        return new Term(getPathField(), ";");
    }
    
    public Term getQNameRootTerm()
    {
        return new Term(getQnameField(), ";");
    }

    /*
     * @see org.apache.lucene.search.Query#createWeight(org.apache.lucene.search.Searcher)
     */
    protected Weight createWeight(Searcher searcher)
    {
        return new StructuredFieldWeight(searcher);
    }

    /*
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        return "";
    }

    /*
     * @see org.apache.lucene.search.Query#toString(java.lang.String)
     */
    public String toString(String field)
    {
        return "";
    }

    private class StructuredFieldWeight implements Weight
    {

        /**
         * 
         */
        private static final long serialVersionUID = 3257854259645985328L;

        private Searcher searcher;

        private float value;

        private float idf;

        private float queryNorm;

        private float queryWeight;

        public StructuredFieldWeight(Searcher searcher)
        {
            this.searcher = searcher;

        }

        /*
         * @see org.apache.lucene.search.Weight#explain(org.apache.lucene.index.IndexReader,
         *      int)
         */
        public Explanation explain(IndexReader reader, int doc) throws IOException
        {
            throw new UnsupportedOperationException();
        }

        /*
         * @see org.apache.lucene.search.Weight#getQuery()
         */
        public Query getQuery()
        {
            return PathQuery.this;
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.apache.lucene.search.Weight#getValue()
         */
        public float getValue()
        {
            return value;
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.apache.lucene.search.Weight#normalize(float)
         */
        public void normalize(float queryNorm)
        {
            this.queryNorm = queryNorm;
            queryWeight *= queryNorm; // normalize query weight
            value = queryWeight * idf; // idf for document
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.apache.lucene.search.Weight#scorer(org.apache.lucene.index.IndexReader)
         */
        public Scorer scorer(IndexReader reader) throws IOException
        {
            return PathScorer.createPathScorer(getSimilarity(searcher), PathQuery.this, reader, this);
            
//            if ((pathStructuredFieldPositions.size() + qNameStructuredFieldPositions.size()) == 0) // optimize
//                // zero-term
//                // case
//                return null;
//
//            Scorer selfScorer = null;
//            if(selfWeight != null)
//            {
//                selfScorer = selfWeight.scorer(reader);
//            }
//            
//            for (StructuredFieldPosition sfp : pathStructuredFieldPositions)
//            {
//                if (sfp.getTermText() != null)
//                {
//                    TermPositions p = reader.termPositions(new Term(pathField, sfp.getTermText()));
//                    if (p == null)
//                        return null;
//                    CachingTermPositions ctp = new CachingTermPositions(p);
//                    sfp.setCachingTermPositions(ctp);
//                }
//            }
//
//            for (StructuredFieldPosition sfp : qNameStructuredFieldPositions)
//            {
//                if (sfp.getTermText() != null)
//                {
//                    TermPositions p = reader.termPositions(new Term(qNameField, sfp.getTermText()));
//                    if (p == null)
//                        return null;
//                    CachingTermPositions ctp = new CachingTermPositions(p);
//                    sfp.setCachingTermPositions(ctp);
//                }
//            }
//
//            TermPositions rootPositions = null;
//            if (rootTerm != null)
//            {
//                rootPositions = reader.termPositions(rootTerm);
//            }
//
//            TermPositions tp = reader.termPositions();
//
//            ContainerScorer cs = null;
//
//            TermPositions level0 = null;
//
//            TermPositions nodePositions = reader.termPositions(new Term("ISNODE", "T"));
//
//            // StructuredFieldPosition[] test =
//            // (StructuredFieldPosition[])structuredFieldPositions.toArray(new
//            // StructuredFieldPosition[]{});
//            if (pathStructuredFieldPositions.size() > 0)
//            {
//                TermPositions containerPositions = reader.termPositions(new Term("ISCONTAINER", "T"));
//                cs = new ContainerScorer(this, rootPositions, (StructuredFieldPosition[]) pathStructuredFieldPositions.toArray(new StructuredFieldPosition[] {}),
//                        containerPositions, getSimilarity(searcher), reader.norms(pathField));
//            }
//            else
//            {
//                level0 = reader.termPositions(new Term("ISROOT", "T"));
//            }
//
//            LeafScorer ls = new LeafScorer(this, level0, cs, (StructuredFieldPosition[]) qNameStructuredFieldPositions.toArray(new StructuredFieldPosition[] {}), nodePositions,
//                    selfScorer, reader, getSimilarity(searcher), reader.norms(qNameField));
//
//            return ls;
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.apache.lucene.search.Weight#sumOfSquaredWeights()
         */
        public float sumOfSquaredWeights() throws IOException
        {
            idf = getSimilarity(searcher).idf(getTerms(), searcher); // compute
            // idf
            queryWeight = idf * getBoost(); // compute query weight
            return queryWeight * queryWeight; // square it
        }

        private ArrayList<Term> getTerms()
        {
            ArrayList<Term> answer = new ArrayList<Term>(pathStructuredFieldPositions.size());
            for (StructuredFieldPosition sfp : pathStructuredFieldPositions)
            {
                if (sfp.getTermText() != null)
                {
                    Term term = new Term(pathField, sfp.getTermText());
                    answer.add(term);
                }
            }
            return answer;
        }
    }

    public void removeDescendantAndSelf()
    {
        while ((getLast() != null) && getLast().linkSelf())
        {
            removeLast();
            removeLast();
        }
    }

    private StructuredFieldPosition getLast()

    {
        if (qNameStructuredFieldPositions.size() > 0)
        {
            return qNameStructuredFieldPositions.get(qNameStructuredFieldPositions.size() - 1);
        }
        else
        {
            return null;
        }
    }

    private void removeLast()
    {
        qNameStructuredFieldPositions.clear();
        for (int i = 0; i < unitSize; i++)
        {
            if (pathStructuredFieldPositions.size() > 0)
            {
                qNameStructuredFieldPositions.add(0, pathStructuredFieldPositions.remove(pathStructuredFieldPositions.size() - 1));
            }
        }
    }

    public boolean isEmpty()
    {
        return qNameStructuredFieldPositions.size() == 0;
    }

    public List<StructuredFieldPosition> getPathStructuredFieldPositions()
    {
        return pathStructuredFieldPositions;
    }
    

    public List<StructuredFieldPosition> getQNameStructuredFieldPositions()
    {
        return qNameStructuredFieldPositions;
    }
    

    
    
}